package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityGameModeBinding

class GameModeActivity : BaseActivity() {
    private lateinit var binding: ActivityGameModeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val action = intent.getStringExtra(EXTRA_ACTION) ?: ACTION_PLAY

        binding.naturalButton.setOnClickListener {
            if (action == ACTION_SCORES) {
                startActivity(
                    Intent(this, ScoresActivity::class.java).putExtra(
                        ScoresActivity.EXTRA_SCORE_MODE,
                        com.example.aprendemoslavida.utils.ScoreManager.MODE_NATURAL
                    )
                )
            } else {
                startActivity(Intent(this, GameActivity::class.java))
            }
        }

        binding.mathButton.setOnClickListener {
            if (action == ACTION_SCORES) {
                startActivity(
                    Intent(this, ScoresActivity::class.java).putExtra(
                        ScoresActivity.EXTRA_SCORE_MODE,
                        com.example.aprendemoslavida.utils.ScoreManager.MODE_MATH
                    )
                )
            } else {
                startActivity(Intent(this, MathTopicsActivity::class.java))
            }
        }

        binding.englishButton.setOnClickListener {
            if (action == ACTION_SCORES) {
                startActivity(
                    Intent(this, ScoresActivity::class.java).putExtra(
                        ScoresActivity.EXTRA_SCORE_MODE,
                        com.example.aprendemoslavida.utils.ScoreManager.MODE_ENGLISH
                    )
                )
            } else {
                startActivity(Intent(this, EnglishGameActivity::class.java))
            }
        }

        binding.socialButton.setOnClickListener {
            if (action == ACTION_SCORES) {
                startActivity(
                    Intent(this, ScoresActivity::class.java).putExtra(
                        ScoresActivity.EXTRA_SCORE_MODE,
                        com.example.aprendemoslavida.utils.ScoreManager.MODE_SOCIAL
                    )
                )
            } else {
                startActivity(Intent(this, SocialTopicsActivity::class.java))
            }
        }

        binding.backButton.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val ACTION_PLAY = "play"
        const val ACTION_SCORES = "scores"
    }
}
