package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityStoryAdminMenuBinding

class StoryAdminMenuActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryAdminMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryAdminMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.themeButton.setOnClickListener {
            startActivity(Intent(this, StoryThemeSelectionActivity::class.java))
        }

        binding.gameTimeButton.setOnClickListener {
            startActivity(Intent(this, StoryGameTimeSettingsActivity::class.java))
        }

        binding.mapCountButton.setOnClickListener {
            startActivity(Intent(this, StoryMapCountSettingsActivity::class.java))
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
