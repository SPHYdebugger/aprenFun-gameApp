package com.example.aprendemoslavida.activities

import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityHowToPlayBinding

class HowToPlayActivity : BaseActivity() {
    private lateinit var binding: ActivityHowToPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
    }
}
