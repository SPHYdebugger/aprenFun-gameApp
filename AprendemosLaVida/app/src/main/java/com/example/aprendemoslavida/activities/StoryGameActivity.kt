package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.Gravity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import android.os.CountDownTimer
import android.media.AudioManager
import android.media.ToneGenerator
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
    private var countdownTimer: CountDownTimer? = null
    private val totalTimeMs = 4 * 60 * 1000L
    private var timeLeftMs: Long = totalTimeMs
    private var warningShown: Boolean = false
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    private var currentDialogGateId: Int? = null
    private var gameStartMs: Long = 0L
    private var exitingDialog: Boolean = false
    private var gameStarted: Boolean = false
    private var introDeclined: Boolean = false
    private val typewriterHandler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null
    private var typewriterFullText: String? = null
    private var typewriterOnFinished: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.storyGameView.listener = this
        binding.storyGameView.setQuestionBlocking(true)
        binding.topBar.visibility = View.GONE
        binding.mapContainer.visibility = View.GONE
        binding.controlsContainer.visibility = View.GONE
        updateHud()
        setupIntroFlow()

        binding.joystickView.listener = object : com.example.aprendemoslavida.story.VirtualJoystickView.Listener {
            override fun onInputChanged(x: Float, y: Float) {
                binding.storyGameView.setInputVector(x, y)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!gameStarted && binding.introOverlay.visibility == View.VISIBLE) {
                    finish()
                    return
                }
                showExitDialog()
            }
        })
    }

    override fun onGateBlocked(gateId: Int) {
        if (!gameStarted) return
        if (currentDialogGateId != null || exitingDialog) return

        currentDialogGateId = gateId
        progressManager.startTimerIfNeeded(gateId)
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        showQuestionDialog(gateId)
    }

    override fun onExitReached() {
        if (!gameStarted) return
        if (!progressManager.requiredGatesUnlocked()) {
            showStoryToast(getString(R.string.story_need_all_checkpoints))
            return
        }

        countdownTimer?.cancel()
        showCompletionDialog()
    }

    override fun onStoryQuestionAnswered(gateId: Int, selectedIndex: Int) {
        if (!gameStarted) return
        val question = progressManager.getOrCreateQuestion(gateId)
        if (selectedIndex == question.correctIndex) {
            val elapsedMs = progressManager.elapsedForGate(gateId)
            val gained = scoreManager.onCorrectAnswer(elapsedMs)
            progressManager.unlockGate(gateId)
            binding.storyGameView.setGates(progressManager.gates)
            showStoryToast(getString(R.string.story_correct_points_format, gained))
            closeQuestionState()
        } else {
            scoreManager.onWrongAttempt()
            showStoryToast(getString(R.string.story_wrong_penalty))
            // Keep the same question and keep the gate timer running.
            binding.root.postDelayed({ showQuestionDialog(gateId) }, 120L)
        }
        updateHud()
    }

    private fun showQuestionDialog(gateId: Int) {
        if (!gameStarted) return
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
        binding.scoreText.text = getString(R.string.story_score_format, scoreManager.score)
        binding.timeText.text = formatTime(timeLeftMs)
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

    private fun goToResults(finalScore: Int = scoreManager.score) {
        val totalTime = (SystemClock.elapsedRealtime() - gameStartMs).toInt()
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, finalScore)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, totalTime)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, if (gameStarted) progressManager.gates.size else 0)
            putExtra(ResultActivity.EXTRA_GAME_MODE, ScoreManager.MODE_STORY)
        }
        startActivity(intent)
        finish()
    }

    private fun showCompletionDialog() {
        val trophiesView = buildTrophiesView()
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.story_completed))
            .setView(trophiesView)
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                goToResults()
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
    }

    private val hideStoryToastRunnable = Runnable {
        binding.storyToast.animate().alpha(0f).setDuration(200).withEndAction {
            binding.storyToast.visibility = View.GONE
        }.start()
    }

    private fun showStoryToast(message: String) {
        binding.storyToast.text = message
        binding.storyToast.visibility = View.VISIBLE
        binding.storyToast.alpha = 0f
        binding.storyToast.animate().alpha(1f).setDuration(150).start()
        binding.storyToast.removeCallbacks(hideStoryToastRunnable)
        binding.storyToast.postDelayed(hideStoryToastRunnable, 1400L)
    }

    private fun buildTrophiesView(): android.view.View {
        val padding = dp(16)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val message = android.widget.TextView(this).apply {
            text = getString(R.string.story_final_score_format, scoreManager.score)
            setTextColor(getColor(R.color.text_primary))
        }

        val label = android.widget.TextView(this).apply {
            text = getString(R.string.story_trophies_label)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, dp(12), 0, dp(8))
        }

        val size = dp(28)
        val trophies = progressManager.gates.sortedBy { it.id }
        val rows = listOf(
            trophies.take(5),
            trophies.drop(5).take(5)
        )

        container.addView(message)
        container.addView(label)
        rows.forEach { rowTrophies ->
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            rowTrophies.forEach { gate ->
                val image = android.widget.ImageView(this)
                image.setImageBitmap(buildTrophyBitmap(size, gate.unlocked))
                if (!gate.unlocked) {
                    image.alpha = 0.35f
                }
                val params = android.widget.LinearLayout.LayoutParams(size, size)
                params.marginEnd = dp(6)
                image.layoutParams = params
                row.addView(image)
            }
            container.addView(row)
        }
        return container
    }

    private fun buildTrophyBitmap(sizePx: Int, colored: Boolean): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val gold = if (colored) android.graphics.Color.parseColor("#F2C94C") else android.graphics.Color.parseColor("#B0B0B0")
        val darkGold = if (colored) android.graphics.Color.parseColor("#C9A33A") else android.graphics.Color.parseColor("#8F8F8F")
        val base = if (colored) android.graphics.Color.parseColor("#8C6B2A") else android.graphics.Color.parseColor("#7A7A7A")

        paint.color = gold
        canvas.drawRect(
            sizePx * 0.28f,
            sizePx * 0.2f,
            sizePx * 0.72f,
            sizePx * 0.55f,
            paint
        )
        canvas.drawOval(
            sizePx * 0.18f,
            sizePx * 0.25f,
            sizePx * 0.35f,
            sizePx * 0.45f,
            paint
        )
        canvas.drawOval(
            sizePx * 0.65f,
            sizePx * 0.25f,
            sizePx * 0.82f,
            sizePx * 0.45f,
            paint
        )

        paint.color = darkGold
        canvas.drawRect(
            sizePx * 0.45f,
            sizePx * 0.55f,
            sizePx * 0.55f,
            sizePx * 0.7f,
            paint
        )

        paint.color = base
        canvas.drawRect(
            sizePx * 0.3f,
            sizePx * 0.72f,
            sizePx * 0.7f,
            sizePx * 0.85f,
            paint
        )

        return bitmap
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000L).toInt().coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return getString(R.string.story_time_format, minutes, seconds)
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        timeLeftMs = totalTimeMs
        warningShown = false
        updateHud()

        countdownTimer = object : CountDownTimer(totalTimeMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished
                if (!warningShown && timeLeftMs <= 60_000L) {
                    warningShown = true
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    showStoryToast(getString(R.string.story_time_warning))
                }
                if (timeLeftMs <= 60_000L) {
                    val seconds = (timeLeftMs / 1000L).toInt()
                    val color = if (seconds % 2 == 0) {
                        getColor(R.color.time_warning)
                    } else {
                        getColor(R.color.text_primary)
                    }
                    binding.timeText.setTextColor(color)
                } else {
                    binding.timeText.setTextColor(getColor(R.color.text_primary))
                }
                updateHud()
            }

            override fun onFinish() {
                timeLeftMs = 0L
                binding.timeText.setTextColor(getColor(R.color.text_primary))
                updateHud()
                onTimeUp()
            }
        }.start()
    }

    private fun onTimeUp() {
        if (!gameStarted) return
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        countdownTimer?.cancel()
        val halfScore = (scoreManager.score / 2).coerceAtLeast(0)
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.story_time_up_title))
            .setMessage(getString(R.string.story_time_up_message))
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                goToResults(halfScore)
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        cancelTypewriter()
        tone.release()
        super.onDestroy()
    }

    private fun setupIntroFlow() {
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.GONE
        binding.introBackButton.visibility = View.GONE
        binding.introText.text = ""
        binding.introOverlay.setOnClickListener {
            if (typewriterRunnable != null) {
                completeTypewriterImmediately()
            }
        }
        binding.introBackButton.setOnClickListener { goToMainMenu() }
        binding.introYesButton.setOnClickListener {
            if (gameStarted) return@setOnClickListener
            cancelTypewriter()
            binding.introOverlay.visibility = View.GONE
            startStoryGame()
        }
        binding.introNoButton.setOnClickListener {
            if (introDeclined) return@setOnClickListener
            introDeclined = true
            binding.introButtonsRow.visibility = View.GONE
            binding.introBackButton.visibility = View.GONE
            startTypewriter(getString(R.string.story_intro_no_text)) {
                binding.introBackButton.visibility = View.VISIBLE
            }
        }
        startTypewriter(getString(R.string.story_intro_text)) {
            if (!introDeclined) {
                binding.introButtonsRow.visibility = View.VISIBLE
            }
        }
    }

    private fun startStoryGame() {
        gameStarted = true
        questionProvider = StoryQuestionProvider(this)
        progressManager = StoryProgressManager(questionProvider)
        gameStartMs = SystemClock.elapsedRealtime()
        binding.storyGameView.setGates(progressManager.gates)
        binding.storyGameView.randomizeSecretEntrance()
        binding.storyGameView.resetPlayerPosition()
        binding.storyGameView.setQuestionBlocking(false)
        binding.topBar.visibility = View.VISIBLE
        binding.mapContainer.visibility = View.VISIBLE
        binding.controlsContainer.visibility = View.VISIBLE
        binding.timeText.setTextColor(getColor(R.color.text_primary))
        updateHud()
        startCountdown()
    }

    private fun startTypewriter(text: String, onFinished: (() -> Unit)? = null) {
        cancelTypewriter()
        binding.introText.text = ""
        typewriterFullText = text
        typewriterOnFinished = onFinished
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (index >= text.length) {
                    val callback = typewriterOnFinished
                    typewriterRunnable = null
                    typewriterFullText = null
                    typewriterOnFinished = null
                    callback?.invoke()
                    return
                }
                index += 1
                binding.introText.text = text.substring(0, index)
                typewriterHandler.postDelayed(this, 52L)
            }
        }
        typewriterRunnable = runnable
        typewriterHandler.post(runnable)
    }

    private fun cancelTypewriter() {
        typewriterRunnable?.let { typewriterHandler.removeCallbacks(it) }
        typewriterRunnable = null
        typewriterFullText = null
        typewriterOnFinished = null
    }

    private fun completeTypewriterImmediately() {
        val fullText = typewriterFullText ?: return
        val callback = typewriterOnFinished
        typewriterRunnable?.let { typewriterHandler.removeCallbacks(it) }
        typewriterRunnable = null
        typewriterFullText = null
        typewriterOnFinished = null
        binding.introText.text = fullText
        callback?.invoke()
    }

    private fun goToMainMenu() {
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }
}
