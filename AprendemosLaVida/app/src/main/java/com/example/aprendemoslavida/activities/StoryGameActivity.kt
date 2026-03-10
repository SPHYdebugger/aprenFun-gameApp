package com.example.aprendemoslavida.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.CountDownTimer
import android.content.res.ColorStateList
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.databinding.ActivityStoryGameBinding
import com.example.aprendemoslavida.story.StoryGameView
import com.example.aprendemoslavida.story.StoryCastleDialogFragment
import com.example.aprendemoslavida.story.StoryMap
import com.example.aprendemoslavida.story.StoryProgressManager
import com.example.aprendemoslavida.story.StoryQuestionDialogFragment
import com.example.aprendemoslavida.story.StoryQuestionProvider
import com.example.aprendemoslavida.story.StoryScoreManager
import com.example.aprendemoslavida.story.StoryTopic
import com.example.aprendemoslavida.utils.ScoreManager
import com.example.aprendemoslavida.utils.SettingsManager
import kotlin.random.Random

// Hosts the Zelda-like mode and coordinates map, gates, questions and scoring.
class StoryGameActivity : BaseActivity(), StoryGameView.Listener, StoryQuestionDialogFragment.Listener, StoryCastleDialogFragment.Listener {
    private lateinit var binding: ActivityStoryGameBinding
    private lateinit var progressManager: StoryProgressManager
    private lateinit var questionProvider: StoryQuestionProvider
    private val storyMaps: List<StoryMap> = listOf(StoryMap.createDefault()) + StoryMap.createAllVariants()
    private val scoreManager = StoryScoreManager()
    private var countdownTimer: CountDownTimer? = null
    private var totalTimeMs = 4 * 60 * 1000L
    private var timeLeftMs: Long = totalTimeMs
    private var warningShown: Boolean = false
    private var storyTimerPausedForCastle: Boolean = false
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private var musicPlayer: MediaPlayer? = null
    private var soundEnabled: Boolean = true
    private var fastMusicApplied: Boolean = false

    private var currentDialogGateId: Int? = null
    private var gameStartMs: Long = 0L
    private var exitingDialog: Boolean = false
    private var gameStarted: Boolean = false
    private var introDeclined: Boolean = false
    private var currentMapIndex: Int = 0
    private var mapStartScore: Int = 0
    private var sessionMapCount: Int = 5
    private var completedMapsCount: Int = 0
    private val typewriterHandler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null
    private var typewriterFullText: String? = null
    private var typewriterOnFinished: (() -> Unit)? = null
    private var overlayTapAction: (() -> Unit)? = null
    private var santiChallengeRound: Int = -1
    private var santiEncounterDone: Boolean = false
    private var santiEncounterActive: Boolean = false

    companion object {
        private const val SANTI_CASTLE_GATE_ID = -999
        private const val SANTI_REWARD_POINTS = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.storyGameView.listener = this
        binding.storyGameView.setQuestionBlocking(true)
        binding.topBar.visibility = View.GONE
        binding.mapContainer.visibility = View.GONE
        binding.controlsContainer.visibility = View.GONE
        binding.storySoundButton.visibility = View.GONE
        soundEnabled = SettingsManager.isSoundEnabled(this)
        updateSoundIcon()
        binding.storySoundButton.setOnClickListener { toggleStorySound() }
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
        if (santiEncounterActive) return
        if (currentDialogGateId != null || exitingDialog) return

        currentDialogGateId = gateId
        progressManager.startTimerIfNeeded(gateId)
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        val gateTopic = progressManager.getGate(gateId)?.topic
        if (gateTopic == StoryTopic.CASTLES) {
            pauseStoryTimerForCastle()
            showCastleDialog(gateId)
        } else {
            showQuestionDialog(gateId)
        }
    }

    override fun onExitReached() {
        if (!gameStarted) return
        if (santiEncounterActive) return
        if (progressManager.unlockedGateCount() < 5) {
            showStoryToast(getString(R.string.story_need_min_trophies))
            return
        }

        countdownTimer?.cancel()
        showWorldSummaryDialog(timeUp = false)
    }

    override fun onSantiNpcReached() {
        if (!gameStarted) return
        if (santiEncounterDone || santiEncounterActive) return
        if (currentDialogGateId != null || exitingDialog) return

        santiEncounterActive = true
        santiEncounterDone = true
        binding.storyGameView.setSantiNpcTile(null)
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        pauseStoryTimerForCastle()
        showSantiOfferOverlay()
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

    override fun onStoryCastleResolved(gateId: Int, points: Int) {
        if (!gameStarted) return
        if (gateId == SANTI_CASTLE_GATE_ID) {
            scoreManager.addPoints(SANTI_REWARD_POINTS)
            updateHud()
            showSantiClosingOverlay(
                getString(R.string.story_santi_success_text),
                getString(R.string.story_santi_success_toast)
            )
            return
        }
        progressManager.unlockGate(gateId)
        binding.storyGameView.setGates(progressManager.gates)
        scoreManager.addPoints(points)
        showStoryToast(getString(R.string.story_castle_correct_points_format, points))
        closeQuestionState()
        resumeStoryTimerAfterCastle()
        updateHud()
    }

    override fun onStoryCastleFailed(gateId: Int) {
        if (!gameStarted) return
        if (gateId == SANTI_CASTLE_GATE_ID) {
            showSantiClosingOverlay(
                getString(R.string.story_santi_fail_text),
                getString(R.string.story_santi_fail_toast)
            )
            return
        }
        progressManager.unlockGate(gateId)
        binding.storyGameView.setGates(progressManager.gates)
        closeQuestionState()
        resumeStoryTimerAfterCastle()
        updateHud()
    }

    private fun showQuestionDialog(gateId: Int) {
        if (!gameStarted) return
        if (supportFragmentManager.findFragmentByTag(StoryQuestionDialogFragment.TAG) != null) return
        val dialog = StoryQuestionDialogFragment.newInstance(gateId, progressManager.getOrCreateQuestion(gateId))
        dialog.show(supportFragmentManager, StoryQuestionDialogFragment.TAG)
    }

    private fun showCastleDialog(gateId: Int) {
        if (!gameStarted) return
        if (supportFragmentManager.findFragmentByTag(StoryCastleDialogFragment.TAG) != null) return
        val title = if (gateId == SANTI_CASTLE_GATE_ID) {
            getString(R.string.story_santi_challenge_title)
        } else {
            null
        }
        val dialog = StoryCastleDialogFragment.newInstance(gateId, title)
        dialog.show(supportFragmentManager, StoryCastleDialogFragment.TAG)
    }

    private fun showSantiOfferOverlay() {
        overlayTapAction = null
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.GONE
        binding.introBackButton.visibility = View.GONE
        binding.introCharacter.setImageResource(R.drawable.story_player2_front)
        startTypewriter(getString(R.string.story_santi_offer_text)) {
            if (santiEncounterActive) {
                binding.introButtonsRow.visibility = View.VISIBLE
            }
        }
    }

    private fun showSantiClosingOverlay(text: String, toastMessage: String) {
        overlayTapAction = {
            overlayTapAction = null
            binding.introOverlay.visibility = View.GONE
            showStoryToast(toastMessage)
            finishSantiEncounter()
        }
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.GONE
        binding.introBackButton.visibility = View.GONE
        binding.introCharacter.setImageResource(R.drawable.story_player2_front)
        startTypewriter(text)
    }

    private fun finishSantiEncounter() {
        santiEncounterActive = false
        binding.joystickView.resetStick()
        binding.storyGameView.setQuestionBlocking(false)
        resumeStoryTimerAfterCastle()
        updateHud()
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
                countdownTimer?.cancel()
                binding.joystickView.resetStick()
                showWorldSummaryDialog(
                    timeUp = false,
                    forceFinishOnOk = true,
                    titleOverride = getString(R.string.story_exit_summary_title)
                )
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
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, if (gameStarted) sessionMapCount * 10 else 0)
            putExtra(ResultActivity.EXTRA_GAME_MODE, ScoreManager.MODE_STORY)
        }
        startActivity(intent)
        finish()
    }

    private fun showWorldSummaryDialog(
        timeUp: Boolean,
        forceFinishOnOk: Boolean = false,
        titleOverride: String? = null
    ) {
        val worldScore = (scoreManager.score - mapStartScore).coerceAtLeast(0)
        val completedWorlds = when {
            timeUp -> completedMapsCount
            forceFinishOnOk -> completedMapsCount
            else -> completedMapsCount + 1
        }.coerceIn(0, sessionMapCount)
        val summaryView = buildWorldSummaryView(worldScore, completedWorlds)
        val title = titleOverride ?: if (timeUp) {
            getString(R.string.story_time_up_uppercase)
        } else {
            getString(R.string.story_completed)
        }

        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(title)
            .setView(summaryView)
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                onSummaryAccepted(timeUp = timeUp, forceFinishOnOk = forceFinishOnOk)
            }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
    }

    private fun onSummaryAccepted(timeUp: Boolean, forceFinishOnOk: Boolean) {
        if (timeUp || forceFinishOnOk) {
            stopMusic()
            goToResults()
            return
        }

        completedMapsCount += 1
        val hasNextMap = completedMapsCount < sessionMapCount
        val hasTimeLeft = timeLeftMs > 0L
        if (!hasNextMap || !hasTimeLeft) {
            stopMusic()
            if (!hasNextMap && hasTimeLeft) {
                goToFinalCelebration()
            } else {
                goToResults()
            }
            return
        }

        currentMapIndex = nextMapIndex()
        loadMap(currentMapIndex)
        startCountdown()
    }

    private fun goToFinalCelebration() {
        val totalTime = (SystemClock.elapsedRealtime() - gameStartMs).toInt()
        val intent = Intent(this, StoryCongratulationsActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_SCORE, scoreManager.score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, totalTime)
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, sessionMapCount * 10)
            putExtra(ResultActivity.EXTRA_GAME_MODE, ScoreManager.MODE_STORY)
        }
        startActivity(intent)
        finish()
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

    private fun buildWorldSummaryView(worldScore: Int, completedWorlds: Int): android.view.View {
        val padding = dp(16)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val message = android.widget.TextView(this).apply {
            text = getString(R.string.story_world_score_format, worldScore)
            setTextColor(getColor(R.color.text_primary))
        }

        val totalMessage = android.widget.TextView(this).apply {
            text = getString(R.string.story_total_score_format, scoreManager.score)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, dp(4), 0, 0)
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
        container.addView(totalMessage)
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

        container.addView(buildProgressSection(completedWorlds))
        return container
    }

    private fun buildProgressSection(completedWorlds: Int): android.view.View {
        val wrapper = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, 0)
        }

        val title = android.widget.TextView(this).apply {
            text = getString(R.string.story_progress_title)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, dp(8))
        }
        wrapper.addView(title)

        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val dotSize = dp(14)
        val lineHeight = dp(4)
        val lineWidth = dp(36)
        val doneColor = Color.parseColor("#33AA33")
        val pendingColor = Color.parseColor("#E53935")
        val safeCompleted = completedWorlds.coerceIn(0, sessionMapCount)

        for (i in 0 until sessionMapCount) {
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (i < safeCompleted) doneColor else pendingColor)
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
            }
            row.addView(dot)

            if (i < sessionMapCount - 1) {
                val line = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = lineHeight / 2f
                        setColor(if (i < safeCompleted - 1) doneColor else pendingColor)
                    }
                    layoutParams = android.widget.LinearLayout.LayoutParams(lineWidth, lineHeight).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }
                row.addView(line)
            }
        }
        wrapper.addView(row)
        return wrapper
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
        if (timeLeftMs <= 0L) {
            onTimeUp()
            return
        }
        updateHud()

        countdownTimer = object : CountDownTimer(timeLeftMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished
                if (!warningShown && timeLeftMs <= 60_000L) {
                    warningShown = true
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    showStoryToast(getString(R.string.story_time_warning))
                    setMusicSpeed(multiplier = 1.25f)
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

    private fun pauseStoryTimerForCastle() {
        if (storyTimerPausedForCastle) return
        countdownTimer?.cancel()
        storyTimerPausedForCastle = true
    }

    private fun resumeStoryTimerAfterCastle() {
        if (!storyTimerPausedForCastle) return
        storyTimerPausedForCastle = false
        if (gameStarted && timeLeftMs > 0L && !exitingDialog && currentDialogGateId == null) {
            startCountdown()
        }
    }

    private fun onTimeUp() {
        if (!gameStarted) return
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        countdownTimer?.cancel()
        timeLeftMs = 0L
        binding.timeText.setTextColor(getColor(R.color.text_primary))
        updateHud()
        showWorldSummaryDialog(timeUp = true)
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        cancelTypewriter()
        stopMusic()
        tone.release()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        musicPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (gameStarted && soundEnabled) {
            try {
                musicPlayer?.start()
            } catch (_: IllegalStateException) {
                startMusicIfNeeded()
            }
        }
    }

    private fun setupIntroFlow() {
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.GONE
        binding.introBackButton.visibility = View.GONE
        binding.introText.text = ""
        binding.introOverlay.setOnClickListener {
            if (typewriterRunnable != null) {
                completeTypewriterImmediately()
                return@setOnClickListener
            }
            overlayTapAction?.invoke()
        }
        binding.introBackButton.setOnClickListener { goToMainMenu() }
        binding.introYesButton.setOnClickListener {
            if (santiEncounterActive) {
                binding.introButtonsRow.visibility = View.GONE
                binding.introOverlay.visibility = View.GONE
                showCastleDialog(SANTI_CASTLE_GATE_ID)
                return@setOnClickListener
            }
            if (gameStarted) return@setOnClickListener
            cancelTypewriter()
            binding.introOverlay.visibility = View.GONE
            startStoryGame()
        }
        binding.introNoButton.setOnClickListener {
            if (santiEncounterActive) {
                binding.introButtonsRow.visibility = View.GONE
                showSantiClosingOverlay(
                    getString(R.string.story_santi_decline_text),
                    getString(R.string.story_santi_decline_toast)
                )
                return@setOnClickListener
            }
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
        currentMapIndex = 0
        completedMapsCount = 0
        sessionMapCount = SettingsManager.getStoryMapCount(this).coerceIn(3, 5)
        santiChallengeRound = Random.nextInt(sessionMapCount)
        santiEncounterDone = false
        santiEncounterActive = false
        overlayTapAction = null
        totalTimeMs = SettingsManager.getStoryGameTimeMs(this).toLong()
        timeLeftMs = totalTimeMs
        warningShown = false
        questionProvider = StoryQuestionProvider(this)
        gameStartMs = SystemClock.elapsedRealtime()
        scoreManager.reset()
        loadMap(currentMapIndex)
        binding.topBar.visibility = View.VISIBLE
        binding.mapContainer.visibility = View.VISIBLE
        binding.controlsContainer.visibility = View.VISIBLE
        binding.storySoundButton.visibility = View.VISIBLE
        binding.timeText.setTextColor(getColor(R.color.text_primary))
        updateHud()
        startMusicIfNeeded()
        startCountdown()
    }

    private fun nextMapIndex(): Int {
        if (sessionMapCount == 3) {
            val candidates = storyMaps.indices.filter { it != currentMapIndex }
            if (candidates.isNotEmpty()) {
                return candidates[Random.nextInt(candidates.size)]
            }
        }
        return (currentMapIndex + 1).coerceAtMost(storyMaps.lastIndex)
    }

    private fun toggleStorySound() {
        soundEnabled = !soundEnabled
        SettingsManager.setSoundEnabled(this, soundEnabled)
        updateSoundIcon()
        if (soundEnabled) {
            startMusicIfNeeded()
        } else {
            stopMusic()
        }
    }

    private fun updateSoundIcon() {
        val icon = if (soundEnabled) {
            android.R.drawable.ic_lock_silent_mode_off
        } else {
            android.R.drawable.ic_lock_silent_mode
        }
        binding.storySoundButton.setImageResource(icon)
        binding.storySoundButton.imageTintList = ColorStateList.valueOf(getColor(R.color.text_primary))
        binding.storySoundButton.contentDescription =
            getString(if (soundEnabled) R.string.sound_toggle_on else R.string.sound_toggle_off)
    }

    private fun startMusicIfNeeded() {
        if (!soundEnabled || !gameStarted) return
        if (musicPlayer == null) {
            musicPlayer = MediaPlayer.create(this, R.raw.story_music)?.apply {
                isLooping = true
                setVolume(0.55f, 0.55f)
            }
        }
        setMusicSpeed(if (warningShown) 1.25f else 1.0f)
        musicPlayer?.start()
    }

    private fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer?.release()
        musicPlayer = null
        fastMusicApplied = false
    }

    private fun setMusicSpeed(multiplier: Float) {
        if (fastMusicApplied && multiplier > 1f) return
        fastMusicApplied = multiplier > 1f
        val player = musicPlayer ?: return
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return
        try {
            val params = player.playbackParams ?: android.media.PlaybackParams()
            player.playbackParams = params.setSpeed(multiplier)
        } catch (_: Exception) {
            // Ignore on devices where playback speed is not supported for this stream.
        }
    }

    private fun loadMap(index: Int) {
        val map = storyMaps[index]
        mapStartScore = scoreManager.score
        progressManager = StoryProgressManager(
            questionProvider,
            map,
            SettingsManager.getStoryGateTopics(this)
        )
        currentDialogGateId = null
        exitingDialog = false
        binding.storyGameView.setMap(map)
        binding.storyGameView.setGates(progressManager.gates)
        binding.storyGameView.randomizeSecretEntrance()
        val shouldPlaceSanti = !santiEncounterDone && completedMapsCount == santiChallengeRound
        if (shouldPlaceSanti) {
            val santiTile = map.trophyHiddenCandidates
                .filter { (x, y) -> map.isWalkable(x, y) && map.tileTypeAt(x, y) != StoryMap.TileType.TREE }
                .randomOrNull()
            binding.storyGameView.setSantiNpcTile(santiTile)
        } else {
            binding.storyGameView.setSantiNpcTile(null)
        }
        binding.storyGameView.setQuestionBlocking(false)
        binding.joystickView.resetStick()
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
