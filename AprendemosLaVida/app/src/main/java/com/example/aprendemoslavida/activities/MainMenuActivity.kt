package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityMainMenuBinding

class MainMenuActivity : BaseActivity() {
    private lateinit var binding: ActivityMainMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playButton.setOnClickListener {
            startActivity(
                Intent(this, GameModeActivity::class.java).putExtra(
                    GameModeActivity.EXTRA_ACTION,
                    GameModeActivity.ACTION_PLAY
                )
            )
        }

        binding.howToPlayButton.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }

        binding.scoresButton.setOnClickListener {
            startActivity(
                Intent(this, GameModeActivity::class.java).putExtra(
                    GameModeActivity.EXTRA_ACTION,
                    GameModeActivity.ACTION_SCORES
                )
            )
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.exitButton.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
                .setTitle(getString(R.string.exit_confirm_title))
                .setMessage(getString(R.string.exit_confirm_message))
                .setPositiveButton(getString(R.string.exit_confirm_yes)) { _, _ ->
                    finishAffinity()
                }
                .setNegativeButton(getString(R.string.exit_confirm_no), null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
        }
    }
}
