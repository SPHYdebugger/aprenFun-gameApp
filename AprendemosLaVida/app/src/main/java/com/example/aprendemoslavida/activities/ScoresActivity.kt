package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityScoresBinding
import com.example.aprendemoslavida.utils.ScoreManager

class ScoresActivity : BaseActivity() {
    private lateinit var binding: ActivityScoresBinding
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var isLongPressTriggered: Boolean = false
    private var pendingMode: String = ScoreManager.MODE_NATURAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra(EXTRA_SCORE_MODE) ?: ScoreManager.MODE_NATURAL
        pendingMode = mode
        binding.scoresTitle.text = when (mode) {
            ScoreManager.MODE_MATH -> getString(R.string.scores_title_math)
            ScoreManager.MODE_ENGLISH -> getString(R.string.scores_title_english)
            ScoreManager.MODE_SOCIAL -> getString(R.string.scores_title_social)
            ScoreManager.MODE_STORY -> getString(R.string.scores_title_story)
            else -> getString(R.string.scores_title_natural)
        }

        loadScores(mode)

        binding.backButton.setOnClickListener { finish() }

        binding.backButton.setOnLongClickListener {
            isLongPressTriggered = false
            longPressHandler.postDelayed({
                isLongPressTriggered = true
                showClearDialog()
            }, 5000)
            true
        }

        binding.backButton.setOnTouchListener { _, event ->
            if (isLongPressTriggered || event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                longPressHandler.removeCallbacksAndMessages(null)
            }
            false
        }
    }

    private fun loadScores(mode: String) {
        val scores = ScoreManager(this).getTopScores(10, mode)
        val items = if (scores.isEmpty()) {
            listOf(getString(R.string.scores_empty))
        } else {
            scores.mapIndexed { index, entry ->
                getString(
                    R.string.score_item_format,
                    index + 1,
                    entry.name,
                    entry.score,
                    entry.date
                )
            }
        }

        binding.scoresList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            items
        )
    }

    private fun showClearDialog() {
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.scores_clear_title))
            .setMessage(getString(R.string.scores_clear_message))
            .setPositiveButton(getString(R.string.scores_clear_confirm)) { _, _ ->
                ScoreManager(this).clearScores(pendingMode)
                loadScores(pendingMode)
                Toast.makeText(this, getString(R.string.scores_cleared_toast), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.scores_clear_cancel), null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    companion object {
        const val EXTRA_SCORE_MODE = "extra_score_mode"
    }
}
