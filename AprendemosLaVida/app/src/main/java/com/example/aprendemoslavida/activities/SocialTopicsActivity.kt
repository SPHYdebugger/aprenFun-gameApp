package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.databinding.ActivitySocialTopicsBinding
import com.example.aprendemoslavida.utils.SocialGameManager

class SocialTopicsActivity : BaseActivity() {
    private lateinit var binding: ActivitySocialTopicsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topic0Button.setOnClickListener {
            startActivity(
                Intent(this, SocialGameActivity::class.java).putExtra(
                    SocialGameActivity.EXTRA_TOPIC,
                    SocialGameManager.TOPIC_SOLAR_SYSTEM
                )
            )
        }

        binding.topic1Button.setOnClickListener {
            startActivity(
                Intent(this, SocialGameActivity::class.java).putExtra(
                    SocialGameActivity.EXTRA_TOPIC,
                    SocialGameManager.TOPIC_LANDSCAPE
                )
            )
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
