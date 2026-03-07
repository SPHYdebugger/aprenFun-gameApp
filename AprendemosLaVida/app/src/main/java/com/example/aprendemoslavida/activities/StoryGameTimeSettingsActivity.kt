package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.Toast
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryGameTimeSettingsBinding
import com.example.aprendemoslavida.utils.SettingsManager

class StoryGameTimeSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryGameTimeSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryGameTimeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentValue()

        binding.saveButton.setOnClickListener { saveValue() }
        binding.backButton.setOnClickListener { finish() }
    }

    private fun loadCurrentValue() {
        val totalSeconds = SettingsManager.getStoryGameTimeMs(this) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.minutesInput.setText(minutes.toString())
        binding.secondsInput.setText(seconds.toString())
    }

    private fun saveValue() {
        val minutes = binding.minutesInput.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val seconds = binding.secondsInput.text?.toString()?.trim()?.toIntOrNull() ?: 0
        if (minutes < 0 || seconds < 0 || seconds > 59) {
            Toast.makeText(this, getString(R.string.story_game_time_invalid), Toast.LENGTH_SHORT).show()
            return
        }

        val totalMs = ((minutes * 60) + seconds) * 1000
        if (totalMs < 10_000) {
            Toast.makeText(this, getString(R.string.story_game_time_min), Toast.LENGTH_SHORT).show()
            return
        }

        SettingsManager.setStoryGameTimeMs(this, totalMs)
        Toast.makeText(this, getString(R.string.story_game_time_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}

