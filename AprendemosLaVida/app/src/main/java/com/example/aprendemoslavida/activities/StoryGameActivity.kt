package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.model.PresentedQuestion
import com.example.aprendemoslavida.data.model.SantiAnswerResponse
import com.example.aprendemoslavida.data.model.SantiState
import com.example.aprendemoslavida.data.model.StoryAnswerResponse
import com.example.aprendemoslavida.data.model.StorySession
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.StorySessionRepository
import com.example.aprendemoslavida.data.storage.IdempotencyStorage
import com.example.aprendemoslavida.databinding.ActivityStoryGameBinding
import com.example.aprendemoslavida.story.StoryTopic
import com.example.aprendemoslavida.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/** Story gameplay backed exclusively by StorySession endpoints. */
class StoryGameActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryGameBinding
    private lateinit var apiClient: ApiClient
    private lateinit var repository: StorySessionRepository
    private lateinit var idempotency: IdempotencyStorage
    private var session: StorySession? = null
    private var countdown: CountDownTimer? = null
    private var currentGateId: String? = null
    private var currentQuestion: PresentedQuestion? = null
    private var actionInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apiClient = ApiClient.getInstance(this)
        repository = StorySessionRepository(apiClient.storySessionApiService)
        idempotency = IdempotencyStorage(this)

        binding.storyGameView.visibility = View.GONE
        binding.joystickView.visibility = View.GONE
        binding.mapContainer.visibility = View.GONE
        binding.controlsContainer.visibility = View.GONE
        binding.storySoundButton.visibility = View.GONE
        binding.topBar.visibility = View.VISIBLE
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.VISIBLE
        binding.introYesButton.text = "Empezar historia online"
        binding.introNoButton.text = getString(R.string.story_intro_back)
        binding.introText.text = "La historia se jugará online. Las preguntas, puertas, puntos y tiempo los decide el servidor."
        binding.introYesButton.setOnClickListener { createStorySession() }
        binding.introNoButton.setOnClickListener { finish() }
        binding.introBackButton.setOnClickListener { abandonStorySession() }
    }

    private fun createStorySession() {
        if (actionInFlight) return
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNullOrBlank()) {
            Toast.makeText(this, "Selecciona un perfil infantil para jugar online", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ChildSelectorActivity::class.java))
            finish()
            return
        }
        actionInFlight = true
        val action = "story_create_${childId}_${SettingsManager.getStoryMapCount(this)}"
        val key = idempotency.getOrCreateKey(action)
        val topics = SettingsManager.getStoryGateTopics(this).map(::backendTopic)
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.createStorySession(
                childProfileId = childId,
                mapCount = SettingsManager.getStoryMapCount(this@StoryGameActivity),
                durationSeconds = SettingsManager.getStoryGameTimeMs(this@StoryGameActivity) / 1000,
                gateTopics = topics,
                idempotencyKey = key
            )
            withContext(Dispatchers.Main) {
                actionInFlight = false
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        session = result.data
                        binding.introOverlay.visibility = View.GONE
                        updateHud()
                        startAuthoritativeCountdown()
                        continueStory()
                    }
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo crear la historia. Reintenta; se conservará la misma clave.")
                }
            }
        }
    }

    private fun continueStory() {
        val current = session ?: return
        if (current.status != "ACTIVE") {
            showResults(current)
            return
        }
        val map = current.currentMap
        if (map == null) {
            finishStorySession()
            return
        }
        val gate = map.gates.firstOrNull { it.status != "RESOLVED" }
        if (gate != null) {
            currentGateId = gate.id
            openGate(gate.id)
            return
        }
        if (map.resolvedGates >= map.minimumGatesToExit) {
            exitMap(map.id)
            return
        }
        showStoryState("El mapa no tiene otra puerta disponible. Actualizando el estado…")
        refreshSession()
    }

    private fun openGate(gateId: String) {
        if (actionInFlight) return
        actionInFlight = true
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.openStoryGate(session?.id.orEmpty(), gateId)
            withContext(Dispatchers.Main) {
                actionInFlight = false
                when (result) {
                    is NetworkResult.Success -> {
                        currentGateId = gateId
                        val q = result.data.currentQuestion ?: result.data.nextQuestion
                        if (q != null) askGateQuestion(q) else refreshSession()
                    }
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo abrir la puerta. Reintenta.")
                }
            }
        }
    }

    private fun askGateQuestion(question: PresentedQuestion) {
        currentQuestion = question
        val options = question.options.map { it.text }.toTypedArray()
        if (options.isEmpty()) {
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                hint = "Respuesta"
            }
            AlertDialog.Builder(this).setTitle("Pregunta").setMessage(question.prompt).setView(input)
                .setPositiveButton("Responder") { _, _ -> submitGateAnswer(numericAnswer = input.text.toString().toIntOrNull()) }
                .setNegativeButton("Cancelar", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Puerta").setMessage(question.prompt)
            .setSingleChoiceItems(options, -1) { dialog, which ->
                dialog.dismiss()
                submitGateAnswer(selectedOptionId = question.options[which].id)
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun submitGateAnswer(selectedOptionId: String? = null, numericAnswer: Int? = null) {
        val current = session ?: return
        val gateId = currentGateId ?: return
        val question = currentQuestion ?: return
        val action = "story_answer_${current.id}_${question.id}"
        val key = idempotency.getOrCreateKey(action)
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.submitStoryAnswer(current.id, gateId, question.id, selectedOptionId, numericAnswer, key)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        showAnswer(result.data)
                        refreshSession()
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        askGateQuestion(question)
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo enviar la respuesta. Reintenta; se conservará la misma clave.")
                        askGateQuestion(question)
                    }
                }
            }
        }
    }

    private fun showAnswer(answer: StoryAnswerResponse) {
        val message = if (answer.correct) "Correcto: +${answer.points} puntos. Total: ${answer.totalScore}" else "Respuesta incorrecta. Total: ${answer.totalScore}"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun offerSanti(santi: SantiState) {
        if (!santi.availableOnCurrentMap || santi.status != "AVAILABLE") return
        AlertDialog.Builder(this).setTitle("Reto de Santi")
            .setMessage("¿Quieres aceptar el reto extra de ${santi.rewardPoints} puntos?")
            .setPositiveButton("Aceptar") { _, _ -> updateSanti("ACCEPT") }
            .setNegativeButton("No", null).show()
    }

    private fun updateSanti(action: String) {
        val current = session ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.updateSantiEncounter(current.id, action)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> if (action == "ACCEPT") {
                        result.data.currentChallenge?.let { askSantiQuestion(it) } ?: refreshSession()
                    } else refreshSession()
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo actualizar el reto de Santi.")
                }
            }
        }
    }

    private fun askSantiQuestion(question: PresentedQuestion) {
        currentQuestion = question
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            hint = "Respuesta"
        }
        AlertDialog.Builder(this).setTitle("Reto de Santi").setMessage(question.prompt).setView(input)
            .setPositiveButton("Responder") { _, _ -> submitSantiAnswer(question, input.text.toString().toIntOrNull()) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun submitSantiAnswer(question: PresentedQuestion, answer: Int?) {
        if (answer == null) {
            Toast.makeText(this, "Escribe una respuesta", Toast.LENGTH_SHORT).show()
            return
        }
        val current = session ?: return
        val action = "story_santi_answer_${current.id}_${question.id}"
        val key = idempotency.getOrCreateKey(action)
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.submitSantiAnswer(current.id, question.id, answer, key)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        showSantiAnswer(result.data)
                        refreshSession()
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        askSantiQuestion(question)
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo enviar la respuesta de Santi. Reintenta.")
                        askSantiQuestion(question)
                    }
                }
            }
        }
    }

    private fun showSantiAnswer(answer: SantiAnswerResponse) {
        Toast.makeText(this, if (answer.correct) "Reto superado: +${answer.points}" else "Reto no superado", Toast.LENGTH_SHORT).show()
    }

    private fun exitMap(mapId: String) {
        val current = session ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.exitStoryMap(current.id, mapId)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> { session = result.data; updateHud(); continueStory() }
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo salir del mapa. Reintenta.")
                }
            }
        }
    }

    private fun refreshSession() {
        val id = session?.id ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.getStorySession(id)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        session = result.data
                        updateHud()
                        result.data.santi?.let { offerSanti(it) }
                        continueStory()
                    }
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo actualizar la historia.")
                }
            }
        }
    }

    private fun finishStorySession() {
        val id = session?.id ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.finishStorySession(id)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> showResults(result.data)
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo finalizar la historia.")
                }
            }
        }
    }

    private fun abandonStorySession() {
        val id = session?.id
        if (id != null) CoroutineScope(Dispatchers.IO).launch { repository.abandonStorySession(id) }
        finish()
    }

    private fun showResults(finalSession: StorySession) {
        countdown?.cancel()
        startActivity(Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, finalSession.score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, finalSession.remainingTimeMs)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, finalSession.completedMaps)
            putExtra(ResultActivity.EXTRA_GAME_MODE, SubjectMapper.CODE_STORY)
            putExtra(ResultActivity.EXTRA_SESSION_ID, finalSession.id)
        })
        finish()
    }

    private fun updateHud() {
        val current = session ?: return
        binding.scoreText.text = "Puntos: ${current.score}"
        binding.trophyCountText.text = "Mapas: ${current.completedMaps}/${current.mapCount}"
        binding.timeText.text = formatTime(current.remainingTimeMs)
    }

    private fun showStoryState(message: String) {
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.VISIBLE
        binding.introYesButton.text = "Continuar"
        binding.introNoButton.text = "Abandonar"
        binding.introText.text = message
        binding.introYesButton.setOnClickListener { continueStory() }
        binding.introNoButton.setOnClickListener { abandonStorySession() }
    }

    private fun startAuthoritativeCountdown() {
        countdown?.cancel()
        val remaining = session?.remainingTimeMs ?: 0L
        countdown = object : CountDownTimer(remaining.coerceAtLeast(1L), 1000L) {
            override fun onTick(ms: Long) { binding.timeText.text = formatTime(ms) }
            override fun onFinish() { binding.timeText.text = formatTime(0); finishStorySession() }
        }.start()
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000L).coerceAtLeast(0L)
        return getString(R.string.story_time_format, (seconds / 60).toInt(), (seconds % 60).toInt())
    }

    private fun backendTopic(topic: StoryTopic): String = when (topic) {
        StoryTopic.NATURAL -> SubjectMapper.CODE_NATURAL
        StoryTopic.MATH_MULTIPLICATION -> SubjectMapper.TOPIC_MULTIPLICATION
        StoryTopic.MATH_ADD_SUB -> SubjectMapper.TOPIC_ADD_SUB
        StoryTopic.ENGLISH -> SubjectMapper.CODE_ENGLISH
        StoryTopic.SOCIAL -> SubjectMapper.CODE_SOCIAL
        StoryTopic.LANGUAGE -> SubjectMapper.CODE_LANGUAGE
        StoryTopic.CASTLES -> SubjectMapper.TOPIC_CASTLES
    }

    private fun showRetry(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun showNetworkError(status: Int, detail: String?) {
        val message = when (status) {
            401 -> "La sesión ha caducado. Vuelve a iniciar sesión."
            403 -> "No tienes permiso para esta acción."
            409 -> "La historia cambió en el servidor. Se volverá a consultar."
            429 -> "Demasiadas solicitudes. Espera y reintenta."
            in 500..599 -> "El servidor no está disponible. Reintenta."
            else -> detail ?: "Error del servidor ($status)."
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        countdown?.cancel()
        super.onDestroy()
    }
}
