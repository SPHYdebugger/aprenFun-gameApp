package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.databinding.ActivityResultBinding

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val totalTime = intent.getIntExtra(EXTRA_TOTAL_TIME, 0)
        val totalQuestions = intent.getIntExtra(EXTRA_TOTAL_QUESTIONS, 0)
        val rawMode = intent.getStringExtra(EXTRA_GAME_MODE) ?: SubjectMapper.CODE_NATURAL
        val mode = SubjectMapper.toBackendSubjectCode(rawMode)
        val topic = intent.getStringExtra(EXTRA_REMOTE_TOPIC)

        binding.scoreText.text = getString(R.string.result_score_format, score)
        if (totalTime > 0) {
            binding.timeText.text = getString(R.string.result_time_format, totalTime / 1000)
            binding.timeText.visibility = View.VISIBLE
        } else {
            binding.timeText.visibility = View.GONE
        }
        binding.messageText.text = buildMessage(score, totalQuestions)

        // Local save button is hidden because scores & progress are 100% managed by backend
        binding.saveScoreButton.visibility = View.GONE

        binding.playAgainButton.setOnClickListener {
            val replayIntent = when (mode) {
                SubjectMapper.CODE_MATH -> when (topic) {
                    SubjectMapper.TOPIC_ADD_SUB -> Intent(this, AddSubMathGameActivity::class.java)
                    SubjectMapper.TOPIC_CASTLES -> Intent(this, TorresActivity::class.java)
                    else -> Intent(this, MathGameActivity::class.java)
                }
                SubjectMapper.CODE_ENGLISH -> Intent(this, EnglishGameActivity::class.java)
                SubjectMapper.CODE_SOCIAL -> Intent(this, SocialGameActivity::class.java)
                SubjectMapper.CODE_LANGUAGE -> Intent(this, LanguageGameActivity::class.java)
                SubjectMapper.CODE_STORY -> Intent(this, StoryGameActivity::class.java)
                else -> Intent(this, GameActivity::class.java)
            }
            startActivity(replayIntent)
            finish()
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
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_REMOTE_TOPIC = "extra_remote_topic"
        const val EXTRA_SOCIAL_TOPIC = "extra_social_topic"
        const val EXTRA_MATH_TYPE = "extra_math_type"
    }
}
