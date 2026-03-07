package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.Toast
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryMapCountSettingsBinding
import com.example.aprendemoslavida.utils.SettingsManager

class StoryMapCountSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryMapCountSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryMapCountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateCurrentLabel()

        binding.mode5MapsButton.setOnClickListener {
            SettingsManager.applyStorySessionPreset(this, 5)
            Toast.makeText(this, getString(R.string.story_map_count_saved_5), Toast.LENGTH_SHORT).show()
            updateCurrentLabel()
        }

        binding.mode3MapsButton.setOnClickListener {
            SettingsManager.applyStorySessionPreset(this, 3)
            Toast.makeText(this, getString(R.string.story_map_count_saved_3), Toast.LENGTH_SHORT).show()
            updateCurrentLabel()
        }

        binding.backButton.setOnClickListener { finish() }
    }

    private fun updateCurrentLabel() {
        val maps = SettingsManager.getStoryMapCount(this)
        val timeMin = SettingsManager.getStoryGameTimeMs(this) / 60_000
        binding.currentConfigText.text = getString(R.string.story_map_count_current_format, maps, timeMin)
    }
}

