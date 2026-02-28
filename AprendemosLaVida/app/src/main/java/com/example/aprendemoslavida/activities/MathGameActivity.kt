package com.example.aprendemoslavida.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityGameBinding
import com.example.aprendemoslavida.utils.MathGameManager

class MathGameActivity : BaseActivity() {
    private lateinit var binding: ActivityGameBinding
    private val gameManager = MathGameManager()
    private var timer: CountDownTimer? = null
    private var timeLeftMs: Int = 12000
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private var currentCorrectIndex: Int = 0
    private var defaultAnswerTint: ColorStateList? = null
    private var currentA: Int = 1
    private var currentB: Int = 1
    private var currentAnswer: Int = 1
    private var exitDialogShowing: Boolean = false

    private val questionTimeMs = 12000
    private val answerFeedbackMs = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.answer1.setOnClickListener { handleAnswer(0) }
        binding.answer2.setOnClickListener { handleAnswer(1) }
        binding.answer3.setOnClickListener { handleAnswer(2) }
        binding.answer4.setOnClickListener { handleAnswer(3) }
        binding.backButton.setOnClickListener { showExitDialog() }
        defaultAnswerTint = binding.answer1.backgroundTintList

        showQuestion(resetTimer = true)
    }

    private fun showQuestion(resetTimer: Boolean) {
        val question = gameManager.currentQuestion() ?: return
        resetAnswerColors()
        val shuffled = question.options
            .mapIndexed { index, option -> option to (index == question.correctIndex) }
            .shuffled()
        currentCorrectIndex = shuffled.indexOfFirst { it.second }

        currentA = question.a
        currentB = question.b
        currentAnswer = question.correctAnswer

        binding.questionText.text = getString(R.string.math_question_format, currentA, currentB)
        binding.answer1.text = shuffled[0].first.toString()
        binding.answer2.text = shuffled[1].first.toString()
        binding.answer3.text = shuffled[2].first.toString()
        binding.answer4.text = shuffled[3].first.toString()
        binding.progressText.text = getString(
            R.string.game_progress_format,
            gameManager.currentIndex + 1,
            gameManager.totalQuestions()
        )
        binding.pointsText.visibility = View.INVISIBLE
        enableAnswers(true)
        if (resetTimer) {
            startTimer(questionTimeMs)
        } else {
            startTimer(timeLeftMs)
        }
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
        val question = gameManager.currentQuestion() ?: return
        if (exitDialogShowing) return
        timer?.cancel()
        enableAnswers(false)

        val correct = selectedIndex == currentCorrectIndex
        if (correct) {
            val elapsed = questionTimeMs - timeLeftMs
            gameManager.addTime(elapsed)
            val points = gameManager.pointsForElapsed(elapsed)
            gameManager.addScore(points)
            showPoints(getString(R.string.points_correct_format, points))
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            showCorrectAnswerFeedback(selectedIndex)
            binding.root.postDelayed({ goNext() }, answerFeedbackMs)
        } else {
            gameManager.addScore(-20)
            showPoints(getString(R.string.points_penalty_format, 20))
            tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
            showWrongDialog()
        }
    }

    private fun showWrongDialog() {
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setMessage(getString(R.string.math_wrong_message_format, currentA, currentB, currentAnswer))
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                enableAnswers(true)
                showQuestion(resetTimer = false)
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
    }

    private fun onTimeUp() {
        gameManager.addTime(questionTimeMs)
        enableAnswers(false)
        showPoints(getString(R.string.time_up))
        tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)

        binding.root.postDelayed({
            goNext()
        }, 700)
    }

    private fun goNext() {
        val hasMore = gameManager.moveNext()
        if (hasMore) {
            showQuestion(resetTimer = true)
        } else {
            goToResults()
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
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, gameManager.score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, gameManager.totalTimeMs)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, gameManager.totalQuestions())
            putExtra(ResultActivity.EXTRA_GAME_MODE, com.example.aprendemoslavida.utils.ScoreManager.MODE_MATH)
            putExtra(ResultActivity.EXTRA_MATH_TYPE, MathTopicsActivity.TYPE_MULTIPLICATION)
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
