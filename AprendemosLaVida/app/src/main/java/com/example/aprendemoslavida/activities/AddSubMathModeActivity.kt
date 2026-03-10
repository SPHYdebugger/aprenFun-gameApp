package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityAddSubMathModeBinding

class AddSubMathModeActivity : BaseActivity() {
    private lateinit var binding: ActivityAddSubMathModeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSubMathModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.operationsButton.setOnClickListener {
            startActivity(Intent(this, AddSubMathGameActivity::class.java))
        }

        binding.castlesButton.setOnClickListener {
            startActivity(Intent(this, TorresActivity::class.java))
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
