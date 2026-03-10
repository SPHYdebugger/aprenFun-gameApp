package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityResultBinding
import com.example.aprendemoslavida.model.ScoreEntry
import com.example.aprendemoslavida.utils.ScoreManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : BaseActivity() {
    private lateinit var binding: ActivityResultBinding
    private var scoreSaved: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val totalTime = intent.getIntExtra(EXTRA_TOTAL_TIME, 0)
        val totalQuestions = intent.getIntExtra(EXTRA_TOTAL_QUESTIONS, 0)
        val mode = intent.getStringExtra(EXTRA_GAME_MODE) ?: ScoreManager.MODE_NATURAL
        val socialTopic = intent.getIntExtra(EXTRA_SOCIAL_TOPIC, 0)
        val mathType = intent.getStringExtra(EXTRA_MATH_TYPE) ?: MathTopicsActivity.TYPE_MULTIPLICATION

        binding.scoreText.text = getString(R.string.result_score_format, score)
        binding.timeText.text = getString(R.string.result_time_format, totalTime / 1000)
        binding.messageText.text = buildMessage(score, totalQuestions)

        binding.playAgainButton.setOnClickListener {
            val replayIntent = when (mode) {
                ScoreManager.MODE_MATH -> {
                    when (mathType) {
                        MathTopicsActivity.TYPE_ADD_SUB -> Intent(this, AddSubMathGameActivity::class.java)
                        MathTopicsActivity.TYPE_ADD_SUB_CASTLES -> Intent(this, TorresActivity::class.java)
                        else -> Intent(this, MathGameActivity::class.java)
                    }
                }
                ScoreManager.MODE_ENGLISH -> Intent(this, EnglishGameActivity::class.java)
                ScoreManager.MODE_SOCIAL -> Intent(this, SocialGameActivity::class.java).putExtra(
                    SocialGameActivity.EXTRA_TOPIC,
                    socialTopic
                )
                ScoreManager.MODE_LANGUAGE -> Intent(this, LanguageGameActivity::class.java)
                ScoreManager.MODE_STORY -> Intent(this, StoryGameActivity::class.java)
                else -> Intent(this, GameActivity::class.java)
            }
            startActivity(replayIntent)
            finish()
        }

        binding.saveScoreButton.setOnClickListener {
            if (!scoreSaved) {
                showSaveDialog(score, mode)
            }
        }

        binding.viewScoresButton.setOnClickListener {
            startActivity(
                Intent(this, ScoresActivity::class.java).putExtra(
                    ScoresActivity.EXTRA_SCORE_MODE,
                    mode
                )
            )
            finish()
        }

        binding.menuButton.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }

    private fun showSaveDialog(score: Int, mode: String) {
        val input = EditText(this)
        input.hint = getString(R.string.save_score_hint)
        input.filters = arrayOf(InputFilter.LengthFilter(16))
        input.setBackgroundResource(R.drawable.bg_dialog_input)
        val inputPadding = resources.getDimensionPixelSize(R.dimen.dialog_input_padding)
        input.setPadding(inputPadding, inputPadding, inputPadding, inputPadding)

        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.save_score_title))
            .setView(input)
            .setPositiveButton(getString(R.string.save_score_positive)) { _, _ ->
                val name = input.text.toString().trim()
                val safeName = if (name.isEmpty()) getString(R.string.score_default_name) else name
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                ScoreManager(this).saveScore(ScoreEntry(safeName, date, score, mode))
                scoreSaved = true
                binding.saveScoreButton.isEnabled = false
                Toast.makeText(this, getString(R.string.save_score_toast), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.save_score_negative), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    private fun buildMessage(score: Int, totalQuestions: Int): String {
        val maxScore = totalQuestions * 100
        if (maxScore <= 0) return getString(R.string.message_keep_practicing)
        val percent = (score.toFloat() / maxScore.toFloat()) * 100f
        return when {
            percent >= 70f -> getString(R.string.message_great)
            percent >= 40f -> getString(R.string.message_good)
            else -> getString(R.string.message_keep_practicing)
        }
    }

    companion object {
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_TOTAL_TIME = "extra_total_time"
        const val EXTRA_TOTAL_QUESTIONS = "extra_total_questions"
        const val EXTRA_GAME_MODE = "extra_game_mode"
        const val EXTRA_SOCIAL_TOPIC = "extra_social_topic"
        const val EXTRA_MATH_TYPE = "extra_math_type"
    }
}
