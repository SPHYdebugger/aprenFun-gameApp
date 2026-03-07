package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivitySettingsBinding
import com.example.aprendemoslavida.utils.LocaleManager
import com.example.aprendemoslavida.utils.SettingsManager

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var hiddenMenuOpened = false
    private val openHiddenMenuRunnable = Runnable {
        hiddenMenuOpened = true
        startActivity(Intent(this, StoryThemeSelectionActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateLanguageButtonLabel()
        updateQuestionTimeButtonLabel()

        binding.changeLanguageButton.setOnClickListener { toggleLanguage() }
        binding.questionTimeButton.setOnClickListener { showQuestionTimeDialog() }

        binding.backButton.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    hiddenMenuOpened = false
                    longPressHandler.postDelayed(openHiddenMenuRunnable, 8_000L)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(openHiddenMenuRunnable)
                }
            }
            false
        }

        binding.backButton.setOnClickListener {
            if (!hiddenMenuOpened) {
                finish()
            }
        }
    }

    private fun toggleLanguage() {
        val current = LocaleManager.getSavedLanguage(this)
        val next = if (current == "es") "gl" else "es"
        LocaleManager.setLocale(this, next)
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun updateLanguageButtonLabel() {
        val current = LocaleManager.getSavedLanguage(this)
        val labelRes = if (current == "es") {
            R.string.change_language_to_galician
        } else {
            R.string.change_language_to_spanish
        }
        binding.changeLanguageButton.setText(labelRes)
    }

    private fun updateQuestionTimeButtonLabel() {
        val seconds = SettingsManager.getQuestionTimeMs(this) / 1000
        binding.questionTimeButton.text = getString(R.string.question_time_button_format, seconds)
    }

    private fun showQuestionTimeDialog() {
        val times = SettingsManager.availableQuestionTimesMs()
        val labels = times.map { getString(R.string.question_time_option_format, it / 1000) }.toTypedArray()
        val current = SettingsManager.getQuestionTimeMs(this)
        val currentIndex = times.indexOf(current).coerceAtLeast(0)

        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.question_time_dialog_title))
            .setSingleChoiceItems(labels, currentIndex) { dialogInterface, which ->
                SettingsManager.setQuestionTimeMs(this, times[which])
                updateQuestionTimeButtonLabel()
                dialogInterface.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    override fun onDestroy() {
        longPressHandler.removeCallbacks(openHiddenMenuRunnable)
        super.onDestroy()
    }
}
