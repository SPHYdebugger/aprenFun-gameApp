package com.example.aprendemoslavida.activities

import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityCastleSubtractionsTutorialBinding

class CastleSubtractionsTutorialActivity : BaseActivity() {
    private lateinit var binding: ActivityCastleSubtractionsTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCastleSubtractionsTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
    }
}
