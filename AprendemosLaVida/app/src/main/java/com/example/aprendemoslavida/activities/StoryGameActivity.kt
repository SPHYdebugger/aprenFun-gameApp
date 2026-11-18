package com.example.aprendemoslavida.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.model.PresentedQuestion
import com.example.aprendemoslavida.data.model.StoryAnswerResponse
import com.example.aprendemoslavida.data.model.StorySession
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.StorySessionRepository
import com.example.aprendemoslavida.data.storage.IdempotencyStorage
import com.example.aprendemoslavida.databinding.ActivityStoryGameBinding
import com.example.aprendemoslavida.story.StoryCastleDialogFragment
import com.example.aprendemoslavida.story.StoryGameView
import com.example.aprendemoslavida.story.StoryMap
import com.example.aprendemoslavida.story.StoryProgressManager
import com.example.aprendemoslavida.story.StoryQuestion
import com.example.aprendemoslavida.story.StoryQuestionDialogFragment
import com.example.aprendemoslavida.story.StoryQuestionProvider
import com.example.aprendemoslavida.story.StoryServerBridge
import com.example.aprendemoslavida.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Story mode: the map, character, joystick, music and dialogues run locally (as before the
 * backend), while the StorySession on the server decides questions, grading, points, time,
 * map progression and Santi (F-15). Local gate id == server gate position.
 *
 * Every server call is triggered by a player action (touching a gate, the exit or Santi,
 * answering, pressing a button); nothing is retried automatically (F-14).
 */
class StoryGameActivity : BaseActivity(), StoryGameView.Listener, StoryQuestionDialogFragment.Listener,
    StoryCastleDialogFragment.Listener {

    private enum class DialogueClip { NONE, SOFIA_1, SOFIA_2, SANTI_1, SANTI_2 }

    private lateinit var binding: ActivityStoryGameBinding
    private lateinit var apiClient: ApiClient
    private lateinit var repository: StorySessionRepository
    private lateinit var idempotency: IdempotencyStorage
    private lateinit var questionProvider: StoryQuestionProvider
    private var progressManager: StoryProgressManager? = null

    private val storyMaps: List<StoryMap> =
        listOf(StoryMap.createDefault()) +
            StoryMap.createAllVariants() +
            StoryMap.createAllCityVariants() +
            StoryMap.createAllClassicCityVariants()

    // Server state
    private var session: StorySession? = null
    private var loadedMapId: String? = null
    private var actionInFlight = false

    // Question in progress
    private var currentDialogGateId: Int? = null
    private var currentServerGateId: String? = null
    private var currentQuestion: PresentedQuestion? = null

    // Santi
    private var santiEncounterActive = false
    private var santiQuestion: PresentedQuestion? = null

    // Flow
    private var gameStarted = false
    private var introDeclined = false
    private var exitingDialog = false
    private var storyPausedInBackground = false
    private var mapStartScore = 0

    // Timer (display only; the server owns the clock)
    private var countdownTimer: CountDownTimer? = null
    private var timeLeftMs = 0L
    private var warningShown = false
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    // Audio
    private var musicPlayer: MediaPlayer? = null
    private val musicHandler = Handler(Looper.getMainLooper())
    private var globalSoundEnabled = true
    private var storyMusicEnabled = true
    private var fastMusicApplied = false
    private var musicTargetVolume = 0.55f
    private var musicSpeedMultiplier = 1.0f
    private var musicLoopFadeOutArmed = false
    private var musicFadeRunnable: Runnable? = null
    private var dialoguePlayer: MediaPlayer? = null

    // Typewriter overlay
    private val typewriterHandler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null
    private var typewriterFullText: String? = null
    private var typewriterOnFinished: (() -> Unit)? = null
    private var overlayTapAction: (() -> Unit)? = null

    private val musicLoopWatcher = object : Runnable {
        override fun run() {
            val player = musicPlayer ?: return
            if (!player.isPlaying) return
            val duration = player.duration
            val position = player.currentPosition
            if (duration > 0) {
                val remaining = duration - position
                if (!musicLoopFadeOutArmed && remaining in 1..MUSIC_LOOP_FADE_MS) {
                    musicLoopFadeOutArmed = true
                    fadeMusicVolume(from = musicTargetVolume, to = 0f, durationMs = MUSIC_LOOP_FADE_MS)
                }
                if (musicLoopFadeOutArmed && position < 120) musicLoopFadeOutArmed = false
            }
            musicHandler.postDelayed(this, MUSIC_LOOP_WATCHER_INTERVAL_MS)
        }
    }

    companion object {
        private const val SANTI_CASTLE_GATE_ID = -999
        private const val MUSIC_LOOP_FADE_MS = 220L
        private const val MUSIC_LOOP_WATCHER_INTERVAL_MS = 70L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apiClient = ApiClient.getInstance(this)
        repository = StorySessionRepository(apiClient.storySessionApiService)
        idempotency = IdempotencyStorage(this)
        questionProvider = StoryQuestionProvider(this)
        applyGameplayLayoutForOrientation(resources.configuration.orientation)

        binding.storyGameView.listener = this
        binding.storyGameView.setQuestionBlocking(true)
        binding.topBar.visibility = View.GONE
        binding.mapContainer.visibility = View.GONE
        binding.controlsContainer.visibility = View.GONE
        binding.storySoundButton.visibility = View.GONE
        globalSoundEnabled = SettingsManager.isSoundEnabled(this)
        storyMusicEnabled = SettingsManager.isStoryMusicEnabled(this)
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
                if (!gameStarted) {
                    finish()
                    return
                }
                showExitDialog()
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyGameplayLayoutForOrientation(newConfig.orientation)
    }

    // ---------------------------------------------------------------- session start

    private fun requestStorySession() {
        if (actionInFlight) return
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNullOrBlank()) {
            Toast.makeText(this, "Selecciona un perfil infantil para jugar online", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ChildSelectorActivity::class.java))
            finish()
            return
        }
        actionInFlight = true
        binding.introButtonsRow.visibility = View.GONE
        val mapCount = SettingsManager.getStoryMapCount(this)
        val action = "story_create_${childId}_$mapCount"
        val key = idempotency.getOrCreateKey(action)
        val topics = SettingsManager.getStoryGateTopics(this).map { it.apiCode }
        val durationSeconds = SettingsManager.getStoryGameTimeMs(this) / 1000
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.createStorySession(
                childProfileId = childId,
                mapCount = mapCount,
                durationSeconds = durationSeconds,
                gateTopics = topics,
                idempotencyKey = key
            )
            withContext(Dispatchers.Main) {
                actionInFlight = false
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        cancelTypewriter()
                        binding.introOverlay.visibility = View.GONE
                        startStoryGame(result.data)
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        binding.introButtonsRow.visibility = View.VISIBLE
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo crear la historia. Reintenta; se conservará la misma clave.")
                        binding.introButtonsRow.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun startStoryGame(created: StorySession) {
        gameStarted = true
        warningShown = false
        binding.topBar.visibility = View.VISIBLE
        binding.mapContainer.visibility = View.VISIBLE
        binding.controlsContainer.visibility = View.VISIBLE
        binding.storySoundButton.visibility = View.VISIBLE
        binding.timeText.setTextColor(getColor(R.color.text_primary))
        applySession(created)
        startMusicIfNeeded()
    }

    /** Single entry point for every server snapshot: HUD, clock, map and gates follow the server. */
    private fun applySession(snapshot: StorySession, fromRefresh: Boolean = false) {
        // A response without status/map is not a snapshot (unknown shape): never end the game on it.
        // Ask once for the real state; if GET is also incomplete, stop and tell the player.
        @Suppress("SENSELESS_COMPARISON")
        if (snapshot.status == null || snapshot.id == null) {
            if (fromRefresh) showRetry("Respuesta del servidor no reconocida. Reintenta más tarde.")
            else refreshSession()
            return
        }
        session = snapshot
        if (snapshot.status != "ACTIVE") {
            goToResults(snapshot)
            return
        }
        val serverMap = snapshot.currentMap
        if (serverMap == null) {
            finishStorySession()
            return
        }
        if (serverMap.id != loadedMapId) loadMap(snapshot) else syncGates()
        timeLeftMs = snapshot.remainingTimeMs
        startCountdown()
        updateHud()
    }

    private fun loadMap(snapshot: StorySession) {
        val serverMap = snapshot.currentMap ?: return
        val map = storyMaps[StoryServerBridge.localMapIndex(storyMaps, serverMap)]
        map.randomizeSecretEntrance()
        loadedMapId = serverMap.id
        mapStartScore = snapshot.score
        val manager = StoryProgressManager(questionProvider, map, StoryServerBridge.gateTopics(serverMap))
        progressManager = manager
        currentDialogGateId = null
        exitingDialog = false
        binding.storyGameView.setMap(map)
        syncGates()
        val santiTile = if (StoryServerBridge.santiOnMap(snapshot)) {
            map.trophyHiddenCandidates.filter { (x, y) ->
                map.hiddenZoneAtTile(x, y) >= 0 && map.isWalkable(x, y) &&
                    map.tileTypeAt(x, y) != StoryMap.TileType.TREE
            }.randomOrNull()
        } else null
        binding.storyGameView.setSantiNpcTile(santiTile)
        binding.storyGameView.setQuestionBlocking(false)
        binding.joystickView.resetStick()
    }

    /** Trophy (gate) state comes from the server: a gate is open once it is RESOLVED there. */
    private fun syncGates() {
        val manager = progressManager ?: return
        val serverMap = session?.currentMap
        manager.gates.forEach { gate ->
            val serverGate = StoryServerBridge.serverGate(serverMap, gate.id)
            // A local gate without a server counterpart must not block the player.
            gate.unlocked = serverGate == null || StoryServerBridge.isResolved(serverGate)
        }
        binding.storyGameView.setGates(manager.gates)
    }

    // ---------------------------------------------------------------- gates

    override fun onGateBlocked(gateId: Int) {
        if (!gameStarted || santiEncounterActive || actionInFlight) return
        if (currentDialogGateId != null || exitingDialog) return
        val serverGate = StoryServerBridge.serverGate(session?.currentMap, gateId) ?: return

        currentDialogGateId = gateId
        currentServerGateId = serverGate.id
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()

        val pending = serverGate.currentQuestion
        if (serverGate.status == "ACTIVE" && pending != null) {
            askQuestion(pending)
        } else {
            openGate(serverGate.id)
        }
    }

    private fun openGate(serverGateId: String) {
        val sessionId = session?.id ?: return
        actionInFlight = true
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.openStoryGate(sessionId, serverGateId)
            withContext(Dispatchers.Main) {
                actionInFlight = false
                when (result) {
                    is NetworkResult.Success -> {
                        // F-16: the server may answer with only the opened gate; merge it into the
                        // known session instead of treating it as a full snapshot.
                        session?.let { applySession(result.data.mergeInto(it)) }
                        val q = result.data.questionForGate(serverGateId)
                        if (q != null) askQuestion(q) else {
                            showStoryToast("No se pudo cargar la pregunta de esta puerta")
                            closeQuestionState()
                        }
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        closeQuestionState()
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo abrir la puerta. Vuelve a tocarla para reintentar.")
                        closeQuestionState()
                    }
                }
            }
        }
    }

    private fun askQuestion(question: PresentedQuestion) {
        val gateId = currentDialogGateId ?: return
        currentQuestion = question
        val castle = question.castle
        when {
            question.options.isNotEmpty() -> {
                if (supportFragmentManager.findFragmentByTag(StoryQuestionDialogFragment.TAG) != null) return
                val local = StoryQuestion(question.prompt, question.options.map { it.text }, correctIndex = -1)
                StoryQuestionDialogFragment.newInstance(gateId, local)
                    .show(supportFragmentManager, StoryQuestionDialogFragment.TAG)
            }
            castle != null -> showServerCastle(gateId, castle.top, castle.bottom, castle.operation, castle.columns)
            else -> showNumericDialog(question.prompt) { value -> submitGateAnswer(numericAnswer = value) }
        }
    }

    private fun showServerCastle(gateId: Int, top: Int, bottom: Int, operation: String, columns: Int, title: String? = null) {
        if (supportFragmentManager.findFragmentByTag(StoryCastleDialogFragment.TAG) != null) return
        StoryCastleDialogFragment.newInstanceForServer(
            gateId, top, bottom, subtraction = operation.equals("SUBTRACT", ignoreCase = true), columns = columns, title = title
        ).show(supportFragmentManager, StoryCastleDialogFragment.TAG)
    }

    private fun showNumericDialog(prompt: String, onAnswer: (Int) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            hint = "Respuesta"
        }
        AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.story_checkpoint_title))
            .setMessage(prompt)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Responder") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value == null) {
                    showStoryToast("Escribe una respuesta")
                    binding.root.postDelayed({ showNumericDialog(prompt, onAnswer) }, 120L)
                } else onAnswer(value)
            }
            .show()
    }

    override fun onStoryQuestionAnswered(gateId: Int, selectedIndex: Int) {
        val option = currentQuestion?.options?.getOrNull(selectedIndex) ?: return
        submitGateAnswer(selectedOptionId = option.id)
    }

    override fun onStoryCastleSubmitted(gateId: Int, value: Int) {
        if (gateId == SANTI_CASTLE_GATE_ID) submitSantiAnswer(value) else submitGateAnswer(numericAnswer = value)
    }

    // Local-grading callbacks are not used in server mode.
    override fun onStoryCastleResolved(gateId: Int, points: Int) = Unit
    override fun onStoryCastleFailed(gateId: Int) = Unit

    private fun submitGateAnswer(selectedOptionId: String? = null, numericAnswer: Int? = null) {
        val current = session ?: return
        val serverGateId = currentServerGateId ?: return
        val question = currentQuestion ?: return
        val action = "story_answer_${current.id}_${question.id}"
        val key = idempotency.getOrCreateKey(action)
        actionInFlight = true
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.submitStoryAnswer(current.id, serverGateId, question.id, selectedOptionId, numericAnswer, key)
            withContext(Dispatchers.Main) {
                actionInFlight = false
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        onGateAnswered(result.data)
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        closeQuestionState()
                    }
                    is NetworkResult.Exception -> {
                        // Same key on retry: touching the gate again re-asks the pending question.
                        showRetry("No se pudo enviar la respuesta. Vuelve a tocar la puerta.")
                        closeQuestionState()
                    }
                }
            }
        }
    }

    private fun onGateAnswered(answer: StoryAnswerResponse) {
        showStoryToast(
            if (answer.correct) getString(R.string.story_correct_points_format, answer.points)
            else "Respuesta incorrecta"
        )
        val next = answer.nextQuestion
        if (!answer.correct && next != null && answer.gateStatus != "RESOLVED") {
            binding.root.postDelayed({ askQuestion(next) }, 400L)
            return
        }
        closeQuestionState()
        refreshSession()
    }

    private fun closeQuestionState() {
        currentDialogGateId = null
        currentServerGateId = null
        currentQuestion = null
        exitingDialog = false
        binding.joystickView.resetStick()
        binding.storyGameView.setQuestionBlocking(false)
    }

    private fun refreshSession(onDone: (() -> Unit)? = null) {
        val id = session?.id ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.getStorySession(id)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        applySession(result.data, fromRefresh = true)
                        onDone?.invoke()
                    }
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo actualizar la historia.")
                }
            }
        }
    }

    // ---------------------------------------------------------------- exit / map change

    override fun onExitReached() {
        if (!gameStarted || santiEncounterActive || actionInFlight) return
        if (currentDialogGateId != null || exitingDialog) return
        val serverMap = session?.currentMap ?: return
        if (!StoryServerBridge.canExit(serverMap)) {
            showStoryToast(getString(R.string.story_need_min_trophies))
            return
        }
        val current = session ?: return
        actionInFlight = true
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.exitStoryMap(current.id, serverMap.id)
            withContext(Dispatchers.Main) {
                actionInFlight = false
                when (result) {
                    is NetworkResult.Success -> {
                        @Suppress("SENSELESS_COMPARISON")
                        val complete = result.data.status != null && result.data.id != null
                        if (complete) showWorldSummaryDialog(result.data, timeUp = false)
                        else fetchSessionForSummary()
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        binding.storyGameView.setQuestionBlocking(false)
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo salir del mapa. Vuelve a la meta para reintentar.")
                        binding.storyGameView.setQuestionBlocking(false)
                    }
                }
            }
        }
    }

    /** Exit answered with an unknown shape: read the real state (once) before showing the summary. */
    private fun fetchSessionForSummary() {
        val id = session?.id ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.getStorySession(id)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> showWorldSummaryDialog(result.data, timeUp = false)
                    is NetworkResult.Error -> showNetworkError(result.statusCode, result.problem.detail)
                    is NetworkResult.Exception -> showRetry("No se pudo actualizar la historia.")
                }
                if (result !is NetworkResult.Success) binding.storyGameView.setQuestionBlocking(false)
            }
        }
    }

    private fun showWorldSummaryDialog(after: StorySession, timeUp: Boolean, titleOverride: String? = null) {
        countdownTimer?.cancel()
        val worldScore = (after.score - mapStartScore).coerceAtLeast(0)
        val title = titleOverride ?: getString(if (timeUp) R.string.story_time_up_uppercase else R.string.story_completed)
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(title)
            .setView(buildWorldSummaryView(worldScore, after.score, after.completedMaps, after.mapCount))
            .setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                val finished = after.status != "ACTIVE" || after.currentMap == null || timeUp || titleOverride != null
                if (finished) {
                    stopMusic()
                    if (after.status == "ACTIVE") finishStorySession() else goToResults(after)
                } else {
                    applySession(after)
                }
            }
            .setCancelable(false)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
    }

    private fun finishStorySession() {
        val id = session?.id ?: return
        countdownTimer?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.finishStorySession(id)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> goToResults(result.data)
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        session?.let { goToResults(it) }
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo cerrar la historia en el servidor.")
                        session?.let { goToResults(it) }
                    }
                }
            }
        }
    }

    private fun goToResults(finalSession: StorySession) {
        if (isFinishing) return
        countdownTimer?.cancel()
        stopMusic()
        val allMaps = finalSession.mapCount > 0 && finalSession.completedMaps >= finalSession.mapCount
        val target = if (allMaps) StoryCongratulationsActivity::class.java else ResultActivity::class.java
        startActivity(Intent(this, target).apply {
            putExtra(ResultActivity.EXTRA_SCORE, finalSession.score)
            putExtra(ResultActivity.EXTRA_TOTAL_TIME, (SettingsManager.getStoryGameTimeMs(this@StoryGameActivity) - finalSession.remainingTimeMs).toInt().coerceAtLeast(0))
            putExtra(ResultActivity.EXTRA_TOTAL_QUESTIONS, finalSession.mapCount * 10)
            putExtra(ResultActivity.EXTRA_GAME_MODE, SubjectMapper.CODE_STORY)
            putExtra(ResultActivity.EXTRA_SESSION_ID, finalSession.id)
        })
        finish()
    }

    private fun showExitDialog() {
        if (exitingDialog) return
        exitingDialog = true
        binding.storyGameView.setQuestionBlocking(true)
        val dialog = AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(getString(R.string.exit_confirm_title))
            .setMessage(getString(R.string.exit_confirm_game_message))
            .setPositiveButton(getString(R.string.exit_confirm_yes)) { _, _ ->
                binding.joystickView.resetStick()
                val current = session
                if (current == null) finish()
                else showWorldSummaryDialog(current, timeUp = false, titleOverride = getString(R.string.story_exit_summary_title))
            }
            .setNegativeButton(getString(R.string.exit_confirm_no)) { _, _ ->
                exitingDialog = false
                if (currentDialogGateId == null && !santiEncounterActive) binding.storyGameView.setQuestionBlocking(false)
            }
            .setCancelable(false)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.accent))
    }

    // ---------------------------------------------------------------- Santi

    override fun onSantiNpcReached() {
        if (!gameStarted || santiEncounterActive || actionInFlight) return
        if (currentDialogGateId != null || exitingDialog) return
        val current = session ?: return
        santiEncounterActive = true
        binding.storyGameView.setSantiNpcTile(null)
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        // OFFER marks the encounter on the server; if it is refused the offer is still shown
        // and ACCEPT/DECLINE will report the real state.
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateSantiEncounter(current.id, "OFFER")
            withContext(Dispatchers.Main) { showSantiOfferOverlay() }
        }
    }

    private fun showSantiOfferOverlay() {
        overlayTapAction = null
        binding.introOverlay.visibility = View.VISIBLE
        binding.introButtonsRow.visibility = View.GONE
        binding.introBackButton.visibility = View.GONE
        binding.introYesButton.text = getString(R.string.story_intro_yes)
        binding.introNoButton.text = getString(R.string.story_intro_no)
        binding.introCharacter.setImageResource(R.drawable.story_player2_front)
        startTypewriter(getString(R.string.story_santi_offer_text), clip = DialogueClip.SANTI_1) {
            if (santiEncounterActive) binding.introButtonsRow.visibility = View.VISIBLE
        }
    }

    private fun answerSantiOffer(accept: Boolean) {
        val current = session ?: return
        binding.introButtonsRow.visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.updateSantiEncounter(current.id, if (accept) "ACCEPT" else "DECLINE")
            withContext(Dispatchers.Main) {
                val challenge = (result as? NetworkResult.Success)?.data?.currentChallenge
                when {
                    !accept -> showSantiClosingOverlay(
                        getString(R.string.story_santi_decline_text), getString(R.string.story_santi_decline_toast)
                    )
                    challenge != null -> {
                        binding.introOverlay.visibility = View.GONE
                        askSantiQuestion(challenge)
                    }
                    else -> {
                        if (result is NetworkResult.Error) showNetworkError(result.statusCode, result.problem.detail)
                        else showRetry("El reto de Santi no está disponible ahora.")
                        binding.introOverlay.visibility = View.GONE
                        finishSantiEncounter()
                    }
                }
            }
        }
    }

    private fun askSantiQuestion(question: PresentedQuestion) {
        santiQuestion = question
        val castle = question.castle
        if (castle != null) {
            showServerCastle(
                SANTI_CASTLE_GATE_ID, castle.top, castle.bottom, castle.operation, castle.columns,
                title = getString(R.string.story_santi_challenge_title)
            )
        } else {
            showNumericDialog(question.prompt) { submitSantiAnswer(it) }
        }
    }

    private fun submitSantiAnswer(value: Int) {
        val current = session ?: return
        val question = santiQuestion ?: return
        val action = "story_santi_answer_${current.id}_${question.id}"
        val key = idempotency.getOrCreateKey(action)
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.submitSantiAnswer(current.id, question.id, value, key)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        val next = result.data.nextChallenge
                        when {
                            result.data.correct -> showSantiClosingOverlay(
                                getString(R.string.story_santi_success_text), getString(R.string.story_santi_success_toast)
                            )
                            next != null -> {
                                showStoryToast("Respuesta incorrecta, prueba otra vez")
                                binding.root.postDelayed({ askSantiQuestion(next) }, 400L)
                            }
                            else -> showSantiClosingOverlay(
                                getString(R.string.story_santi_fail_text), getString(R.string.story_santi_fail_toast)
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        showNetworkError(result.statusCode, result.problem.detail)
                        finishSantiEncounter()
                    }
                    is NetworkResult.Exception -> {
                        showRetry("No se pudo enviar la respuesta de Santi.")
                        askSantiQuestion(question)
                    }
                }
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
        startTypewriter(text, clip = DialogueClip.SANTI_2)
    }

    private fun finishSantiEncounter() {
        santiEncounterActive = false
        santiQuestion = null
        binding.joystickView.resetStick()
        binding.storyGameView.setQuestionBlocking(false)
        refreshSession()
    }

    // ---------------------------------------------------------------- intro

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
                answerSantiOffer(accept = true)
                return@setOnClickListener
            }
            if (gameStarted) return@setOnClickListener
            requestStorySession()
        }
        binding.introNoButton.setOnClickListener {
            if (santiEncounterActive) {
                answerSantiOffer(accept = false)
                return@setOnClickListener
            }
            if (introDeclined || gameStarted) return@setOnClickListener
            introDeclined = true
            binding.introButtonsRow.visibility = View.GONE
            startTypewriter(getString(R.string.story_intro_no_text), clip = DialogueClip.SOFIA_2) {
                binding.introBackButton.visibility = View.VISIBLE
            }
        }
        startTypewriter(getString(R.string.story_intro_text), clip = DialogueClip.SOFIA_1) {
            if (!introDeclined) binding.introButtonsRow.visibility = View.VISIBLE
        }
    }

    private fun goToMainMenu() {
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    // ---------------------------------------------------------------- HUD and timer

    private fun updateHud() {
        val current = session
        binding.scoreText.text = getString(R.string.story_score_format, current?.score ?: 0)
        binding.timeText.text = formatTime(timeLeftMs)
        val gates = current?.currentMap?.gates.orEmpty()
        binding.trophyCountText.text = getString(
            R.string.story_trophy_progress_format,
            gates.count { StoryServerBridge.isResolved(it) },
            if (gates.isEmpty()) 10 else gates.size
        )
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        if (timeLeftMs <= 0L) {
            onTimeUp()
            return
        }
        countdownTimer = object : CountDownTimer(timeLeftMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished
                if (!warningShown && timeLeftMs <= 60_000L) {
                    warningShown = true
                    if (globalSoundEnabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    showStoryToast(getString(R.string.story_time_warning))
                    setMusicSpeed(1.25f)
                }
                val color = if (timeLeftMs <= 60_000L && (timeLeftMs / 1000L) % 2 == 0L) {
                    getColor(R.color.time_warning)
                } else getColor(R.color.text_primary)
                binding.timeText.setTextColor(color)
                binding.timeText.text = formatTime(timeLeftMs)
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
        if (!gameStarted || isFinishing) return
        binding.storyGameView.setQuestionBlocking(true)
        binding.joystickView.resetStick()
        countdownTimer?.cancel()
        supportFragmentManager.fragments.forEach { (it as? androidx.fragment.app.DialogFragment)?.dismissAllowingStateLoss() }
        val current = session ?: return
        showWorldSummaryDialog(current, timeUp = true)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000L).toInt().coerceAtLeast(0)
        return getString(R.string.story_time_format, totalSeconds / 60, totalSeconds % 60)
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

    private fun showRetry(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun showNetworkError(status: Int, detail: String?) {
        val message = when (status) {
            401 -> "La sesión ha caducado. Vuelve a iniciar sesión."
            403 -> "No tienes permiso para esta acción."
            409 -> detail ?: "La historia cambió en el servidor."
            429 -> "Demasiadas solicitudes. Espera y reintenta."
            in 500..599 -> "El servidor no está disponible. Reintenta."
            else -> detail ?: getString(R.string.server_error_generic, status)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ---------------------------------------------------------------- world summary view

    private fun buildWorldSummaryView(worldScore: Int, totalScore: Int, completedWorlds: Int, mapCount: Int): View {
        val padding = dp(16)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        container.addView(android.widget.TextView(this).apply {
            text = getString(R.string.story_world_score_format, worldScore)
            setTextColor(getColor(R.color.text_primary))
        })
        container.addView(android.widget.TextView(this).apply {
            text = getString(R.string.story_total_score_format, totalScore)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, dp(4), 0, 0)
        })
        container.addView(android.widget.TextView(this).apply {
            text = getString(R.string.story_trophies_label)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, dp(12), 0, dp(8))
        })
        val size = dp(28)
        val trophies = progressManager?.gates?.sortedBy { it.id }.orEmpty()
        listOf(trophies.take(5), trophies.drop(5).take(5)).forEach { rowTrophies ->
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            rowTrophies.forEach { gate ->
                row.addView(android.widget.ImageView(this).apply {
                    setImageBitmap(buildTrophyBitmap(size, gate.unlocked))
                    if (!gate.unlocked) alpha = 0.35f
                    layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(6) }
                })
            }
            container.addView(row)
        }
        container.addView(buildProgressSection(completedWorlds, mapCount))
        return container
    }

    private fun buildProgressSection(completedWorlds: Int, mapCount: Int): View {
        val wrapper = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        wrapper.addView(android.widget.TextView(this).apply {
            text = getString(R.string.story_progress_title)
            setTextColor(getColor(R.color.text_primary))
            setPadding(0, 0, 0, dp(8))
        })
        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val dotSize = dp(14)
        val lineHeight = dp(4)
        val doneColor = Color.parseColor("#33AA33")
        val pendingColor = Color.parseColor("#E53935")
        val safeCompleted = completedWorlds.coerceIn(0, mapCount)
        for (i in 0 until mapCount) {
            row.addView(View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (i < safeCompleted) doneColor else pendingColor)
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(dotSize, dotSize)
            })
            if (i < mapCount - 1) {
                row.addView(View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = lineHeight / 2f
                        setColor(if (i < safeCompleted - 1) doneColor else pendingColor)
                    }
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp(36), lineHeight).apply {
                        gravity = Gravity.CENTER_VERTICAL
                    }
                })
            }
        }
        wrapper.addView(row)
        return wrapper
    }

    private fun buildTrophyBitmap(sizePx: Int, colored: Boolean): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val gold = Color.parseColor(if (colored) "#F2C94C" else "#B0B0B0")
        val darkGold = Color.parseColor(if (colored) "#C9A33A" else "#8F8F8F")
        val base = Color.parseColor(if (colored) "#8C6B2A" else "#7A7A7A")
        paint.color = gold
        canvas.drawRect(sizePx * 0.28f, sizePx * 0.2f, sizePx * 0.72f, sizePx * 0.55f, paint)
        canvas.drawOval(sizePx * 0.18f, sizePx * 0.25f, sizePx * 0.35f, sizePx * 0.45f, paint)
        canvas.drawOval(sizePx * 0.65f, sizePx * 0.25f, sizePx * 0.82f, sizePx * 0.45f, paint)
        paint.color = darkGold
        canvas.drawRect(sizePx * 0.45f, sizePx * 0.55f, sizePx * 0.55f, sizePx * 0.7f, paint)
        paint.color = base
        canvas.drawRect(sizePx * 0.3f, sizePx * 0.72f, sizePx * 0.7f, sizePx * 0.85f, paint)
        return bitmap
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // ---------------------------------------------------------------- layout

    private fun applyGameplayLayoutForOrientation(orientation: Int) {
        val set = ConstraintSet()
        set.clone(binding.root)
        val margin = dp(12)
        val map = binding.mapContainer.id
        val controls = binding.controlsContainer.id
        val sound = binding.storySoundButton.id
        listOf(map, controls, sound).forEach { id ->
            set.clear(id, ConstraintSet.TOP)
            set.clear(id, ConstraintSet.START)
            set.clear(id, ConstraintSet.END)
            set.clear(id, ConstraintSet.BOTTOM)
        }
        set.constrainWidth(map, 0)
        set.constrainHeight(map, 0)
        set.constrainWidth(controls, dp(190))
        set.constrainHeight(controls, dp(190))
        set.connect(map, ConstraintSet.TOP, binding.topBar.id, ConstraintSet.BOTTOM, margin)
        set.connect(map, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            set.connect(map, ConstraintSet.END, controls, ConstraintSet.START, margin)
            set.connect(map, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
            set.connect(controls, ConstraintSet.TOP, binding.topBar.id, ConstraintSet.BOTTOM, dp(30))
            set.connect(controls, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dp(20))
            set.connect(sound, ConstraintSet.TOP, controls, ConstraintSet.BOTTOM, dp(14))
            set.connect(sound, ConstraintSet.START, controls, ConstraintSet.START, 0)
            set.connect(sound, ConstraintSet.END, controls, ConstraintSet.END, 0)
        } else {
            set.connect(map, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
            set.connect(map, ConstraintSet.BOTTOM, controls, ConstraintSet.TOP, margin)
            set.connect(controls, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp(16))
            set.connect(controls, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
            set.connect(controls, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
            set.connect(sound, ConstraintSet.TOP, map, ConstraintSet.BOTTOM, dp(6))
            set.connect(sound, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp(20))
        }
        set.applyTo(binding.root)
    }

    // ---------------------------------------------------------------- lifecycle

    override fun onPause() {
        super.onPause()
        if (gameStarted && !storyPausedInBackground) {
            storyPausedInBackground = true
            countdownTimer?.cancel()
            binding.joystickView.resetStick()
            binding.storyGameView.setQuestionBlocking(true)
        }
        stopMusicLoopWatcher()
        cancelMusicFade()
        musicPlayer?.pause()
        stopDialogueAudio()
    }

    override fun onResume() {
        super.onResume()
        if (gameStarted && storyPausedInBackground) {
            storyPausedInBackground = false
            val canMove = !exitingDialog && currentDialogGateId == null && !santiEncounterActive &&
                binding.introOverlay.visibility != View.VISIBLE
            binding.storyGameView.setQuestionBlocking(!canMove)
            // The server clock kept running while paused: resync time, score and gates.
            refreshSession()
        }
        globalSoundEnabled = SettingsManager.isSoundEnabled(this)
        storyMusicEnabled = SettingsManager.isStoryMusicEnabled(this)
        updateSoundIcon()
        if (gameStarted && canPlayStoryMusic()) {
            try {
                musicPlayer?.start()
            } catch (_: IllegalStateException) {
                startMusicIfNeeded()
            }
        } else {
            stopMusic()
        }
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        cancelTypewriter()
        stopMusicLoopWatcher()
        cancelMusicFade()
        stopMusic()
        stopDialogueAudio()
        tone.release()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- audio

    private fun canPlayStoryMusic(): Boolean = globalSoundEnabled && storyMusicEnabled

    private fun toggleStorySound() {
        storyMusicEnabled = !storyMusicEnabled
        SettingsManager.setStoryMusicEnabled(this, storyMusicEnabled)
        updateSoundIcon()
        if (canPlayStoryMusic()) startMusicIfNeeded() else stopMusic()
    }

    private fun updateSoundIcon() {
        val on = canPlayStoryMusic()
        binding.storySoundButton.setImageResource(
            if (on) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode
        )
        binding.storySoundButton.imageTintList = ColorStateList.valueOf(getColor(R.color.text_primary))
        binding.storySoundButton.contentDescription =
            getString(if (on) R.string.sound_toggle_on else R.string.sound_toggle_off)
    }

    private fun startMusicIfNeeded() {
        if (!canPlayStoryMusic() || !gameStarted) return
        if (musicPlayer == null) {
            musicPlayer = MediaPlayer.create(this, R.raw.story_music)?.apply {
                isLooping = false
                setVolume(musicTargetVolume, musicTargetVolume)
                setOnCompletionListener { player ->
                    musicLoopFadeOutArmed = false
                    player.seekTo(0)
                    player.setVolume(0f, 0f)
                    player.start()
                    setMusicSpeed(musicSpeedMultiplier)
                    fadeMusicVolume(from = 0f, to = musicTargetVolume, durationMs = MUSIC_LOOP_FADE_MS)
                }
            }
        }
        val player = musicPlayer ?: return
        setMusicSpeed(if (warningShown) 1.25f else 1.0f)
        if (!player.isPlaying) player.start()
        player.setVolume(musicTargetVolume, musicTargetVolume)
        musicLoopFadeOutArmed = false
        startMusicLoopWatcher()
    }

    private fun stopMusic() {
        stopMusicLoopWatcher()
        cancelMusicFade()
        musicPlayer?.stop()
        musicPlayer?.release()
        musicPlayer = null
        fastMusicApplied = false
        musicSpeedMultiplier = 1.0f
        musicLoopFadeOutArmed = false
    }

    private fun setMusicSpeed(multiplier: Float) {
        musicSpeedMultiplier = multiplier
        if (fastMusicApplied && multiplier > 1f) return
        fastMusicApplied = multiplier > 1f
        val player = musicPlayer ?: return
        try {
            val params = player.playbackParams ?: android.media.PlaybackParams()
            player.playbackParams = params.setSpeed(multiplier)
        } catch (_: Exception) {
            // Playback speed not supported for this stream.
        }
    }

    private fun startMusicLoopWatcher() {
        musicHandler.removeCallbacks(musicLoopWatcher)
        musicHandler.post(musicLoopWatcher)
    }

    private fun stopMusicLoopWatcher() = musicHandler.removeCallbacks(musicLoopWatcher)

    private fun cancelMusicFade() {
        musicFadeRunnable?.let { musicHandler.removeCallbacks(it) }
        musicFadeRunnable = null
    }

    private fun fadeMusicVolume(from: Float, to: Float, durationMs: Long) {
        val player = musicPlayer ?: return
        cancelMusicFade()
        val safeDuration = durationMs.coerceAtLeast(1L)
        val startTime = SystemClock.elapsedRealtime()
        val runnable = object : Runnable {
            override fun run() {
                val t = ((SystemClock.elapsedRealtime() - startTime).toFloat() / safeDuration).coerceIn(0f, 1f)
                val v = from + (to - from) * t
                player.setVolume(v, v)
                if (t < 1f && player == musicPlayer) {
                    musicHandler.postDelayed(this, 16L)
                } else {
                    player.setVolume(to, to)
                    if (musicFadeRunnable === this) musicFadeRunnable = null
                }
            }
        }
        musicFadeRunnable = runnable
        musicHandler.post(runnable)
    }

    // ---------------------------------------------------------------- typewriter

    private fun startTypewriter(text: String, clip: DialogueClip = DialogueClip.NONE, onFinished: (() -> Unit)? = null) {
        cancelTypewriter()
        binding.introText.text = ""
        typewriterFullText = text
        typewriterOnFinished = onFinished
        playDialogueClip(clip)
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
        stopDialogueAudio()
    }

    private fun completeTypewriterImmediately() {
        val fullText = typewriterFullText ?: return
        val callback = typewriterOnFinished
        typewriterRunnable?.let { typewriterHandler.removeCallbacks(it) }
        typewriterRunnable = null
        typewriterFullText = null
        typewriterOnFinished = null
        binding.introText.text = fullText
        stopDialogueAudio()
        callback?.invoke()
    }

    private fun playDialogueClip(clip: DialogueClip) {
        stopDialogueAudio()
        if (!globalSoundEnabled) return
        val resId = when (clip) {
            DialogueClip.SOFIA_1 -> R.raw.sofia1
            DialogueClip.SOFIA_2 -> R.raw.sofia2
            // Santi audio files have inverted numbering: texto1 -> santi2, texto2 -> santi1.
            DialogueClip.SANTI_1 -> R.raw.santi2
            DialogueClip.SANTI_2 -> R.raw.santi1
            DialogueClip.NONE -> 0
        }
        if (resId == 0) return
        dialoguePlayer = MediaPlayer.create(this, resId)?.apply {
            setVolume(1f, 1f)
            setOnCompletionListener { player ->
                player.release()
                if (dialoguePlayer == player) dialoguePlayer = null
            }
            start()
        }
    }

    private fun stopDialogueAudio() {
        dialoguePlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Exception) {
            }
            player.release()
        }
        dialoguePlayer = null
    }
}
