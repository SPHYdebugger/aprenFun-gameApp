package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryGameBinding
import com.example.aprendemoslavida.story.StoryGameView
import com.example.aprendemoslavida.story.StoryProgressManager
import com.example.aprendemoslavida.story.StoryQuestionDialogFragment
import com.example.aprendemoslavida.story.StoryQuestionProvider
import com.example.aprendemoslavida.story.StoryScoreManager
import com.example.aprendemoslavida.utils.ScoreManager

// Hosts the Zelda-like mode and coordinates map, gates, questions and scoring.
class StoryGameActivity : BaseActivity(), StoryGameView.Listener, StoryQuestionDialogFragment.Listener {
    private lateinit var binding: ActivityStoryGameBinding
    private lateinit var progressManager: StoryProgressManager
    private lateinit var questionProvider: StoryQuestionProvider
    private val scoreManager = StoryScoreManager()

    private var currentDialogGateId: Int? = null
    private var gameStartMs: Long = 0L
    private var exitingDialog: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questionProvider = StoryQuestionProvider(this)
        progressManager = StoryProgressManager(questionProvider)
        gameStartMs = SystemClock.elapsedRealtime()

        binding.storyGameView.listener = this
        binding.storyGameView.setGates(progressManager.gates)
        binding.storyGameView.resetPlayerPosition()
        updateHud()

        binding.joystickView.listener = object : com.example.aprendemoslavida.story.VirtualJoystickView.Listener {
            override fun onInputChanged(x: Float, y: Float) {
                binding.storyGameView.setInputVector(x, y)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    override fun onGateBlocked(gateId: Int) {
        if (currentDialogGateId != null || exitingDialog) return

        currentDialogGateId = gateId
        progressManager.startTimerIfNeeded(gateId)
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        showQuestionDialog(gateId)
    }

    override fun onExitReached() {
        if (!progressManager.allGatesUnlocked()) {
            Toast.makeText(this, getString(R.string.story_need_all_checkpoints), Toast.LENGTH_SHORT).show()
            return
        }

        showCompletionDialog()
    }

    override fun onStoryQuestionAnswered(gateId: Int, selectedIndex: Int) {
        val question = progressManager.getOrCreateQuestion(gateId)
        if (selectedIndex == question.correctIndex) {
            val elapsedMs = progressManager.elapsedForGate(gateId)
            val gained = scoreManager.onCorrectAnswer(elapsedMs)
            progressManager.unlockGate(gateId)
            binding.storyGameView.setGates(progressManager.gates)
            Toast.makeText(this, getString(R.string.story_correct_points_format, gained), Toast.LENGTH_SHORT).show()
            closeQuestionState()
        } else {
            scoreManager.onWrongAttempt()
            Toast.makeText(this, getString(R.string.story_wrong_penalty), Toast.LENGTH_SHORT).show()
            // Keep the same question and keep the gate timer running.
            binding.root.postDelayed({ showQuestionDialog(gateId) }, 120L)
        }
        updateHud()
    }

    private fun showQuestionDialog(gateId: Int) {
        if (supportFragmentManager.findFragmentByTag(StoryQuestionDialogFragment.TAG) != null) return
        val dialog = StoryQuestionDialogFragment.newInstance(gateId, progressManager.getOrCreateQuestion(gateId))
        dialog.show(supportFragmentManager, StoryQuestionDialogFragment.TAG)
    }

    private fun closeQuestionState() {
        currentDialogGateId = null
        exitingDialog = false
        binding.joystickView.resetStick()
        binding.storyGameView.setQuestionBlocking(false)
    }

    private fun updateHud() {
        val unlocked = progressManager.gates.count { it.unlocked }
        val total = progressManager.gates.size
        binding.scoreText.text = getString(R.string.story_score_format, scoreManager.score)
        binding.progressText.text = getString(R.string.story_checkpoints_format, unlocked, total)
    }

    private fun showExitDialog() {
        if (exitingDialog) return
        exitingDialog = true
        binding.storyGameView.setQuestionBlocking(true)
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.exit_confirm_title))
            .setMessage(getString(R.string.exit_confirm_game_message))
            .setPositiveButton(getString(R.string.exit_confirm_yes)) { _, _ ->
                finish()
            }
            .setNegativeButton(getString(R.string.exit_confirm_no)) { _, _ ->
                exitingDialog = false
                if (currentDialogGateId == null) {
                    binding.storyGameView.setQuestionBlocking(false)
                }
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    private fun goToResults() {
        val totalTime = (SystemClock.elapsedRealtime() - gameStartMs).toInt()
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, scoreManager.score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, totalTime)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, progressManager.gates.size)
            putExtra(ResultActivity.EXTRA_GAME_MODE, ScoreManager.MODE_STORY)
        }
        startActivity(intent)
        finish()
    }

    private fun showCompletionDialog() {
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.story_completed))
            .setMessage(getString(R.string.story_final_score_format, scoreManager.score))
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                goToResults()
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
    }
}
