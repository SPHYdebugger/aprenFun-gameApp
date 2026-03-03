package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityGameTypeMenuBinding

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
            startActivity(Intent(this, StoryGameActivity::class.java))
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
