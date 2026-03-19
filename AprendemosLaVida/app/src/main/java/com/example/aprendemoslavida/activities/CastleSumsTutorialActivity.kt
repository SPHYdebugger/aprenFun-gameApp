package com.example.aprendemoslavida.activities

import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivityCastleSumsTutorialBinding

class CastleSumsTutorialActivity : BaseActivity() {
    private lateinit var binding: ActivityCastleSumsTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCastleSumsTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
    }
}
