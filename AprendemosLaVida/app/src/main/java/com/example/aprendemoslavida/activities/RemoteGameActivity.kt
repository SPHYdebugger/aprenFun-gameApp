package com.example.aprendemoslavida.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.model.GameAnswer
import com.example.aprendemoslavida.data.model.GameSession
import com.example.aprendemoslavida.data.model.PresentedQuestion
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.data.model.AnswerFeedback
import com.example.aprendemoslavida.data.model.AnswerColor
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.GameSessionRepository
import com.example.aprendemoslavida.data.storage.IdempotencyStorage
import com.example.aprendemoslavida.databinding.ActivityGameBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Shared UI for every online game. The server supplies questions, answers,
 * points and deadlines; this class only renders them and submits user input.
 */
abstract class RemoteGameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var apiClient: ApiClient
    private lateinit var repository: GameSessionRepository
    private lateinit var idempotency: IdempotencyStorage

    private var session: GameSession? = null
    private var question: PresentedQuestion? = null
    private var timer: CountDownTimer? = null
    private var answerInFlight = false
    private var createJob: Job? = null
    private var numericInput: EditText? = null
    private var defaultTint: ColorStateList? = null

    protected abstract val backendSubjectCode: String
    protected open val backendTopicCode: String? = null
    protected open val resultMode: String get() = backendSubjectCode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apiClient = ApiClient.getInstance(this)
        repository = GameSessionRepository(apiClient.gameSessionApiService)
        idempotency = IdempotencyStorage(this)
        defaultTint = binding.answer1.backgroundTintList

        listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4)
            .forEachIndexed { index, button -> button.setOnClickListener { submit(index) } }
        binding.backButton.setOnClickListener { abandonAndExit() }
        createRemoteSession()
    }

    private fun createRemoteSession() {
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNullOrBlank()) {
            Toast.makeText(this, "Selecciona un perfil infantil para jugar online", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ChildSelectorActivity::class.java))
            finish()
            return
        }
        enableAnswers(false)
        val action = "game_create_${childId}_${backendSubjectCode}_${backendTopicCode ?: "default"}"
        val key = idempotency.getOrCreateKey(action)
        createJob = CoroutineScope(Dispatchers.IO).launch {
            val result = repository.createGameSession(childId, backendSubjectCode, backendTopicCode, idempotencyKey = key)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        session = result.data
                        renderQuestion(result.data.currentQuestion)
                    }
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo crear la partida. Pulsa atrás y vuelve a intentarlo.")
                }
            }
        }
    }

    private fun renderQuestion(next: PresentedQuestion?) {
        question = next
        if (next == null) {
            loadAuthoritativeSession()
            return
        }
        resetAnswerColors()
        binding.questionText.text = next.prompt.ifBlank { next.castle?.let { "${it.top} ${it.operation} ${it.bottom}" } ?: "" }
        val options = next.options
        val castle = next.interaction.equals("CASTLE", ignoreCase = true)
        numericInput?.let { binding.root.removeView(it) }
        numericInput = null
        if (castle && options.isEmpty()) {
            showNumericCastleInput()
        } else {
            val buttons = listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4)
            buttons.forEachIndexed { i, button ->
                button.isVisible = i < options.size
                button.text = options.getOrNull(i)?.text.orEmpty()
            }
        }
        val total = session?.totalQuestions ?: 0
        binding.progressText.text = getString(R.string.game_progress_format, next.position + 1, total)
        binding.pointsText.visibility = View.INVISIBLE
        enableAnswers(true)
        startAuthoritativeTimer(next.deadline ?: session?.expiresAt)
    }

    private fun showNumericCastleInput() {
        val input = EditText(this).apply {
            hint = "Respuesta"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            textSize = 22f
            setTextColor(getColor(R.color.text_primary))
        }
        val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = 32
            marginEnd = 32
            topToBottom = binding.pointsText.id
            bottomToTop = binding.answer1.id
        }
        binding.root.addView(input, params)
        numericInput = input
        binding.answer1.isVisible = true
        binding.answer1.text = "Comprobar"
        binding.answer2.isVisible = false
        binding.answer3.isVisible = false
        binding.answer4.isVisible = false
    }

    private fun startAuthoritativeTimer(deadline: String?) {
        timer?.cancel()
        val remaining = try {
            deadline?.let { Duration.between(Instant.now(), Instant.parse(it)).toMillis() } ?: 0L
        } catch (_: Exception) { 0L }
        val duration = remaining.coerceAtLeast(0L)
        binding.timerProgress.max = duration.coerceAtLeast(1L).toInt()
        timer = object : CountDownTimer(duration.coerceAtLeast(1L), 100L) {
            override fun onTick(ms: Long) {
                binding.timerProgress.progress = ms.toInt()
                binding.timerText.text = getString(R.string.timer_seconds_format, ((ms + 999) / 1000).toInt())
            }
            override fun onFinish() {
                binding.timerProgress.progress = 0
                binding.timerText.text = getString(R.string.timer_seconds_format, 0)
                // Expiry is decided by the API. Submit no answer and let the
                // authoritative response decide whether the session ended.
                submit(-1)
            }
        }.start()
    }

    private fun submit(index: Int) {
        if (answerInFlight) return
        val currentSession = session ?: return
        val currentQuestion = question ?: return
        val numeric = numericInput?.text?.toString()?.toIntOrNull()
        if (numericInput != null && numeric == null) {
            Toast.makeText(this, "Escribe una respuesta", Toast.LENGTH_SHORT).show()
            return
        }
        answerInFlight = true
        timer?.cancel()
        enableAnswers(false)
        val action = "game_answer_${currentSession.id}_${currentQuestion.id}"
        val key = idempotency.getOrCreateKey(action)
        val optionId = currentQuestion.options.getOrNull(index)?.id
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.submitGameAnswer(currentSession.id, currentQuestion.id, optionId, numeric, key)
            withContext(Dispatchers.Main) { handleAnswerResult(result, action, index) }
        }
    }

    private fun handleAnswerResult(result: NetworkResult<GameAnswer>, action: String, selectedIndex: Int) {
        answerInFlight = false
        when (result) {
            is NetworkResult.Success -> {
                idempotency.clearKey(action)
                showAnswerFeedback(result.data, selectedIndex)
                binding.root.postDelayed({
                    if (result.data.sessionStatus == "COMPLETED" || result.data.nextQuestion == null) loadAuthoritativeSession()
                    else renderQuestion(result.data.nextQuestion)
                }, 650L)
            }
            is NetworkResult.Error -> {
                showNetworkError(result.statusCode, result.problem.detail)
                enableAnswers(true)
                startAuthoritativeTimer(question?.deadline ?: session?.expiresAt)
            }
            is NetworkResult.Exception -> {
                showRetry("No se pudo enviar la respuesta. Puedes reintentarlo; se conservará la misma clave.")
                enableAnswers(true)
                startAuthoritativeTimer(question?.deadline ?: session?.expiresAt)
            }
        }
    }

    private fun showAnswerFeedback(answer: GameAnswer, selectedIndex: Int) {
        val buttons = listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4)
        val state = AnswerFeedback.resolve(answer, question ?: return, selectedIndex)
        if (state.selectedColor == AnswerColor.CORRECT) {
            buttons.getOrNull(state.selectedIndex)?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.answer_correct))
            binding.pointsText.text = getString(R.string.points_correct_format, answer.points)
        } else {
            buttons.getOrNull(state.selectedIndex)?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.answer_wrong))
            buttons.getOrNull(state.correctIndex)?.backgroundTintList = ColorStateList.valueOf(getColor(R.color.answer_correct))
            binding.pointsText.text = getString(R.string.points_wrong)
        }
        binding.pointsText.visibility = View.VISIBLE
    }

    private fun loadAuthoritativeSession() {
        val current = session ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = if (backendTopicCode == SubjectMapper.TOPIC_CASTLES) {
                repository.finishCastleGameSession(current.id)
            } else {
                repository.getGameSession(current.id)
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> showResults(result.data)
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo obtener el resultado del servidor.")
                }
            }
        }
    }

    private fun showResults(finalSession: GameSession) {
        timer?.cancel()
        startActivity(Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, finalSession.score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, 0)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, finalSession.totalQuestions)
            putExtra(ResultActivity.EXTRA_GAME_MODE, resultMode)
            putExtra(ResultActivity.EXTRA_SESSION_ID, finalSession.id)
            putExtra(ResultActivity.EXTRA_REMOTE_TOPIC, backendTopicCode)
        })
        finish()
    }

    private fun abandonAndExit() {
        val current = session
        if (current != null) {
            CoroutineScope(Dispatchers.IO).launch { repository.abandonGameSession(current.id) }
        }
        finish()
    }

    private fun enableAnswers(enabled: Boolean) {
        listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4).forEach { it.isEnabled = enabled }
    }

    private fun resetAnswerColors() {
        defaultTint?.let { tint -> listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4).forEach { it.backgroundTintList = tint } }
    }

    private fun showRetry(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun showNetworkError(status: Int, detail: String?) {
        val message = when (status) {
            401 -> "La sesión ha caducado. Vuelve a iniciar sesión."
            403 -> "No tienes permiso para esta acción."
            409 -> "La partida cambió en el servidor. Se volverá a consultar."
            429 -> "Demasiadas solicitudes. Espera un momento y reintenta."
            in 500..599 -> "El servidor no está disponible. Reintenta en unos segundos."
            else -> detail ?: "Error del servidor ($status)."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        timer?.cancel()
        createJob?.cancel()
        super.onDestroy()
    }
}
