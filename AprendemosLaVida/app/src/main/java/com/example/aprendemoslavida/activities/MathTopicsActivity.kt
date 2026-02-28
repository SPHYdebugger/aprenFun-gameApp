package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityMathTopicsBinding

class MathTopicsActivity : BaseActivity() {
    private lateinit var binding: ActivityMathTopicsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMathTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.multiplicationsButton.setOnClickListener {
            startActivity(Intent(this, MathGameActivity::class.java))
        }

        binding.addSubButton.setOnClickListener {
            startActivity(Intent(this, AddSubMathGameActivity::class.java))
        }

        binding.backButton.setOnClickListener { finish() }
    }

    companion object {
        const val TYPE_MULTIPLICATION = "multiplication"
        const val TYPE_ADD_SUB = "add_sub"
    }
}
