package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityGameTypeMenuBinding
import com.example.aprendemoslavida.utils.SettingsManager

class GameTypeMenuActivity : BaseActivity() {
    private lateinit var binding: ActivityGameTypeMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameTypeMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.questionsModeButton.setOnClickListener {
            startActivity(
                Intent(this, GameModeActivity::class.java).putExtra(
                    GameModeActivity.EXTRA_ACTION,
                    GameModeActivity.ACTION_PLAY
                )
            )
        }

        binding.storyModeButton.setOnClickListener {
            val streakState = SettingsManager.previewStoryStreak(this)
            val target = if (streakState.firstPlayToday) {
                Intent(this, StoryStreakActivity::class.java)
            } else {
                Intent(this, StoryGameActivity::class.java)
            }
            startActivity(target)
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
