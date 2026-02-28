package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivitySettingsBinding
import com.example.aprendemoslavida.utils.LocaleManager

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.changeLanguageButton.setOnClickListener {
            LocaleManager.setLocale(this, "gl")
            val intent = Intent(this, MainMenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
