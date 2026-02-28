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
import com.example.aprendemoslavida.utils.EnglishGameManager
import com.example.aprendemoslavida.utils.ScoreManager

class EnglishGameActivity : BaseActivity() {
    private lateinit var binding: ActivityGameBinding
    private val gameManager by lazy { EnglishGameManager(this) }
    private var timer: CountDownTimer? = null
    private var timeLeftMs: Int = 24000
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private var currentCorrectIndex: Int = 0
    private var defaultAnswerTint: ColorStateList? = null
    private var exitDialogShowing: Boolean = false

    private val questionTimeMs = 24000
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

        showQuestion()
    }

    private fun showQuestion() {
        val question = gameManager.currentQuestion() ?: return
        resetAnswerColors()
        val shuffled = question.options
            .mapIndexed { index, option -> option to (index == question.correctIndex) }
            .shuffled()
        currentCorrectIndex = shuffled.indexOfFirst { it.second }

        binding.questionText.text = question.text
        binding.answer1.text = shuffled[0].first
        binding.answer2.text = shuffled[1].first
        binding.answer3.text = shuffled[2].first
        binding.answer4.text = shuffled[3].first
        binding.progressText.text = getString(
            R.string.game_progress_format,
            gameManager.currentIndex + 1,
            gameManager.totalQuestions()
        )
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
        val question = gameManager.currentQuestion() ?: return
        if (exitDialogShowing) return
        timer?.cancel()
        enableAnswers(false)

        val elapsed = questionTimeMs - timeLeftMs
        gameManager.addTime(elapsed)

        val correct = selectedIndex == currentCorrectIndex
        if (correct) {
            val points = gameManager.pointsForElapsed(elapsed)
            gameManager.addScore(points)
            gameManager.addCorrect()
            showPoints(getString(R.string.points_correct_format, points))
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            showCorrectAnswerFeedback(selectedIndex)
            binding.root.postDelayed({
                goNext()
            }, answerFeedbackMs)
        } else {
            showPoints(getString(R.string.points_wrong))
            tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
            showWrongAnswerFeedback(selectedIndex)
            binding.root.postDelayed({
                goNext()
            }, answerFeedbackMs)
        }
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
            showQuestion()
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

    private fun showWrongAnswerFeedback(selectedIndex: Int) {
        val buttons = listOf(binding.answer1, binding.answer2, binding.answer3, binding.answer4)
        buttons.getOrNull(selectedIndex)?.backgroundTintList =
            ColorStateList.valueOf(getColor(R.color.answer_wrong))
        buttons.getOrNull(currentCorrectIndex)?.backgroundTintList =
            ColorStateList.valueOf(getColor(R.color.answer_correct))
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
            putExtra(ResultActivity.EXTRA_GAME_MODE, ScoreManager.MODE_ENGLISH)
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
