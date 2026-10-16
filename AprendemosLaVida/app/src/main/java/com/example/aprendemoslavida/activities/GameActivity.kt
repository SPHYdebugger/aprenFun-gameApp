package com.example.aprendemoslavida.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.model.GameAnswer
import com.example.aprendemoslavida.data.model.GameSession
import com.example.aprendemoslavida.data.model.PresentedQuestion
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.GameSessionRepository
import com.example.aprendemoslavida.databinding.ActivityGameBinding
import com.example.aprendemoslavida.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var gameSessionRepository: GameSessionRepository
    private lateinit var apiClient: ApiClient

    private var activeSession: GameSession? = null
    private var currentQuestion: PresentedQuestion? = null

    private var timer: CountDownTimer? = null
    private var timeLeftMs: Int = 12000
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private var defaultAnswerTint: ColorStateList? = null

    private var questionTimeMs = 12000
    private val answerFeedbackMs = 2000L
    private var exitDialogShowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient.getInstance(this)
        gameSessionRepository = GameSessionRepository(apiClient.gameSessionApiService)

        binding.answer1.setOnClickListener { handleAnswer(0) }
        binding.answer2.setOnClickListener { handleAnswer(1) }
        binding.answer3.setOnClickListener { handleAnswer(2) }
        binding.answer4.setOnClickListener { handleAnswer(3) }
        binding.backButton.setOnClickListener { showExitDialog() }
        defaultAnswerTint = binding.answer1.backgroundTintList

        questionTimeMs = SettingsManager.getQuestionTimeMs(this)

        initRemoteGameSession()
    }

    private fun initRemoteGameSession() {
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNullOrBlank()) {
            Toast.makeText(this, "Selecciona un perfil infantil para jugar online", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ChildSelectorActivity::class.java))
            finish()
            return
        }

        enableAnswers(false)
        CoroutineScope(Dispatchers.IO).launch {
            val result = gameSessionRepository.createGameSession(
                childProfileId = childId,
                subjectCode = "NATURAL"
            )
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        activeSession = result.data
                        displayQuestion(result.data.currentQuestion)
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this@GameActivity, "Error al crear la sesión: ${result.problem.detail}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@GameActivity, "Sin conexión al servidor", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun displayQuestion(q: PresentedQuestion?) {
        if (q == null) {
            goToResults()
            return
        }
        currentQuestion = q
        resetAnswerColors()

        binding.questionText.text = q.prompt
        val opts = q.options
        binding.answer1.text = opts.getOrNull(0)?.text ?: ""
        binding.answer2.text = opts.getOrNull(1)?.text ?: ""
        binding.answer3.text = opts.getOrNull(2)?.text ?: ""
        binding.answer4.text = opts.getOrNull(3)?.text ?: ""

        val pos = q.position + 1
        val total = activeSession?.totalQuestions ?: 20
        binding.progressText.text = getString(R.string.game_progress_format, pos, total)
        binding.pointsText.visibility = View.INVISIBLE

        enableAnswers(true)
        startTimer(questionTimeMs)
    }

    private fun startTimer(durationMs: Int) {
        timer?.cancel()
        timeLeftMs = durationMs
        binding.timerProgress.max = questionTimeMs
        binding.timerProgress.progress = timeLeftMs
        binding.timerText.text = getString(R.string.timer_seconds_format, (timeLeftMs / 1000) + 1)

        timer = object : CountDownTimer(durationMs.toLong(), 100) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished.toInt()
                binding.timerProgress.progress = timeLeftMs
                binding.timerText.text = getString(R.string.timer_seconds_format, (timeLeftMs / 1000) + 1)
            }

            override fun onFinish() {
                timeLeftMs = 0
                onTimeUp()
            }
        }.start()
    }

    private fun handleAnswer(selectedIndex: Int) {
        val session = activeSession ?: return
        val q = currentQuestion ?: return
        if (exitDialogShowing) return
        timer?.cancel()
        enableAnswers(false)

        val selectedOptionId = q.options.getOrNull(selectedIndex)?.id ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val result = gameSessionRepository.submitGameAnswer(
                sessionId = session.id,
                sessionQuestionId = q.id,
                selectedOptionId = selectedOptionId
            )
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        val answer = result.data
                        processAnswerResult(answer, selectedIndex)
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this@GameActivity, "Error en respuesta: ${result.problem.detail}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@GameActivity, "Sin conexión al servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processAnswerResult(answer: GameAnswer, selectedIndex: Int) {
        if (answer.correct) {
            showPoints(getString(R.string.points_correct_format, answer.points))
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            showCorrectAnswerFeedback(selectedIndex)
        } else {
            showPoints(getString(R.string.points_wrong))
            tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
            showWrongAnswerFeedback(selectedIndex, answer.correctOptionId)
        }

        binding.root.postDelayed({
            if (answer.sessionStatus == "COMPLETED" || answer.nextQuestion == null) {
                goToResults()
            } else {
                displayQuestion(answer.nextQuestion)
            }
        }, answerFeedbackMs)
    }

    private fun onTimeUp() {
        enableAnswers(false)
        showPoints(getString(R.string.time_up))
        tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)

        // Submit time up answer
        val session = activeSession ?: return
        val q = currentQuestion ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = gameSessionRepository.submitGameAnswer(
                sessionId = session.id,
                sessionQuestionId = q.id,
                selectedOptionId = null
            )
            withContext(Dispatchers.Main) {
                binding.root.postDelayed({
                    if (result is NetworkResult.Success) {
                        displayQuestion(result.data.nextQuestion)
                    } else {
                        goToResults()
                    }
                }, 1000)
            }
        }
    }

    private fun showPoints(text: String) {
        binding.pointsText.text = text
        binding.pointsText.visibility = View.VISIBLE
        binding.pointsText.alpha = 0f
        binding.pointsText.scaleX = 0.8f
        binding.pointsText.scaleY = 0.8f
        binding.pointsText.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).start()
    }

    private fun enableAnswers(enabled: Boolean) {
        binding.answer1.isEnabled = enabled
        binding.answer2.isEnabled = enabled
        binding.answer3.isEnabled = enabled
        binding.answer4.isEnabled = enabled
    }

    private fun showWrongAnswerFeedback(selectedIndex: Int, correctOptionId: String?) {
        val buttons = listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4)
        buttons.getOrNull(selectedIndex)?.backgroundTintList =
            ColorStateList.valueOf(getColor(R.color.answer_wrong))

        val correctIndex = currentQuestion?.options?.indexOfFirst { it.id == correctOptionId } ?: -1
        if (correctIndex >= 0) {
            buttons.getOrNull(correctIndex)?.backgroundTintList =
                ColorStateList.valueOf(getColor(R.color.answer_correct))
        }
    }

    private fun showCorrectAnswerFeedback(selectedIndex: Int) {
        val buttons = listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4)
        buttons.getOrNull(selectedIndex)?.backgroundTintList =
            ColorStateList.valueOf(getColor(R.color.answer_correct))
    }

    private fun resetAnswerColors() {
        val tint = defaultAnswerTint ?: return
        binding.answer1.backgroundTintList = tint
        binding.answer2.backgroundTintList = tint
        binding.answer3.backgroundTintList = tint
        binding.answer4.backgroundTintList = tint
    }

    private fun showExitDialog() {
        if (exitDialogShowing) return
        exitDialogShowing = true
        timer?.cancel()
        enableAnswers(false)

        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.exit_confirm_title))
            .setMessage(getString(R.string.exit_confirm_game_message))
            .setPositiveButton(getString(R.string.exit_confirm_yes)) { _, _ ->
                val session = activeSession
                if (session != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        gameSessionRepository.abandonGameSession(session.id)
                    }
                }
                finish()
            }
            .setNegativeButton(getString(R.string.exit_confirm_no)) { _, _ ->
                exitDialogShowing = false
                enableAnswers(true)
                startTimer(timeLeftMs)
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    private fun goToResults() {
        val session = activeSession
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, session?.score ?: 0)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, 0)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, session?.totalQuestions ?: 20)
            putExtra(ResultActivity.EXTRA_GAME_MODE, com.example.aprendemoslavida.utils.ScoreManager.MODE_NATURAL)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        timer?.cancel()
        tone.release()
        super.onDestroy()
    }
}
