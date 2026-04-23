package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryCongratulationsBinding

class StoryCongratulationsActivity : BaseActivity() {
    private lateinit var binding: ActivityStoryCongratulationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryCongratulationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.congratsText.text = getString(R.string.story_final_celebration_text)
        binding.confettiView.start()

        binding.root.setOnClickListener { goToResults() }
    }

    override fun onDestroy() {
        binding.confettiView.stop()
        super.onDestroy()
    }

    override fun onBackPressed() {
        goToResults()
    }

    private fun goToResults() {
        val score = intent.getIntExtra(ResultActivity.EXTRA_SCORE, 0)
        val totalTime = intent.getIntExtra(ResultActivity.EXTRA_TOTAL_TIME, 0)
        val totalQuestions = intent.getIntExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, 0)
        val gameMode = intent.getStringExtra(ResultActivity.EXTRA_GAME_MODE)
            ?: com.example.aprendemoslavida.utils.ScoreManager.MODE_STORY

        val resultIntent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, totalTime)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, totalQuestions)
            putExtra(ResultActivity.EXTRA_GAME_MODE, gameMode)
        }
        startActivity(resultIntent)
        finish()
    }
}
