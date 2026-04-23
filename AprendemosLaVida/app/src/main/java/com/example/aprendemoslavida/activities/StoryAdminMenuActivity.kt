package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryAdminMenuBinding
import com.example.aprendemoslavida.utils.SettingsManager

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

        binding.resetGlobalPointsButton.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
                .setTitle(getString(R.string.story_hidden_menu_reset_global_points))
                .setMessage(getString(R.string.story_hidden_menu_reset_global_points_confirm))
                .setPositiveButton(getString(R.string.scores_clear_confirm)) { _, _ ->
                    SettingsManager.resetGlobalPoints(this)
                    Toast.makeText(this, getString(R.string.story_hidden_menu_reset_global_points_done), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.scores_clear_cancel), null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
