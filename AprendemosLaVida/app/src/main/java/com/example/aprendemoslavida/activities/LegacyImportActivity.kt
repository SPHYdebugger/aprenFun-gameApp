package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.avatar.AvatarProfileStore
import com.example.aprendemoslavida.data.model.LegacyImport
import com.example.aprendemoslavida.data.model.LegacyImportAvatar
import com.example.aprendemoslavida.data.model.LegacyImportPreferences
import com.example.aprendemoslavida.data.model.LegacyImportScore
import com.example.aprendemoslavida.data.model.LegacyImportStreak
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.LegacyImportRepository
import com.example.aprendemoslavida.data.storage.IdempotencyStorage
import com.example.aprendemoslavida.utils.ScoreManager
import com.example.aprendemoslavida.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Adult confirmed, resumable migration of the active local profile. */
class LegacyImportActivity : BaseActivity() {
    private lateinit var apiClient: ApiClient
    private lateinit var repository: LegacyImportRepository
    private lateinit var idempotency: IdempotencyStorage
    private lateinit var statusText: TextView
    private var importButton: com.google.android.material.button.MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiClient = ApiClient.getInstance(this)
        repository = LegacyImportRepository(apiClient.legacyImportApiService)
        idempotency = IdempotencyStorage(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        statusText = TextView(this).apply { textSize = 18f }
        val childId = apiClient.tokenStorage.getActiveChildId()
        root.addView(TextView(this).apply {
            text = "Importar datos locales al perfil infantil activo"
            textSize = 22f
        })
        root.addView(statusText)
        importButton = com.google.android.material.button.MaterialButton(this).apply {
            text = "Confirmar e importar"
            setOnClickListener { requestAdultConfirmation() }
        }
        root.addView(importButton)
        setContentView(root)

        if (childId.isNullOrBlank()) {
            statusText.text = "Selecciona primero el perfil destino."
            importButton?.isEnabled = false
            return
        }
        val localScores = ScoreManager(this).getScoresForLegacyImport()
        statusText.text = "Perfil destino: $childId\nPuntos: ${SettingsManager.getGlobalPoints(this)}\nPartidas locales: ${localScores.size}\nLos nombres locales no se enviarán."
    }

    private fun requestAdultConfirmation() {
        GuardianPasswordDialog.show(
            context = this,
            purpose = GuardianTokenRequest.PURPOSE_MANAGE_CHILDREN
        ) {
            submitImport()
        }
    }

    private fun submitImport() {
        val childId = apiClient.tokenStorage.getActiveChildId() ?: return
        importButton?.isEnabled = false
        val action = "legacy_import_${childId}_schema_1"
        val key = idempotency.getOrCreateKey(action)
        val payload = buildPayload()
        CoroutineScope(Dispatchers.IO).launch {
            val result = repository.importLegacyData(childId, payload, key)
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        idempotency.clearKey(action)
                        importButton?.isEnabled = false
                        statusText.text = when (result.data.status) {
                            "IMPORTED" -> "Importación completada: ${result.data.scoresImported} partidas y ${result.data.pointsImported} puntos."
                            "ALREADY_IMPORTED" -> "Este perfil ya había sido importado. No se han duplicado los datos."
                            else -> "Estado de importación: ${result.data.status}"
                        }
                    }
                    is NetworkResult.Error -> {
                        importButton?.isEnabled = true
                        statusText.text = when (result.statusCode) {
                            409 -> "El servidor rechazó el snapshot. No se han borrado datos locales; revisa el perfil y reintenta."
                            401 -> "La sesión ha caducado. Vuelve a iniciar sesión."
                            403 -> "La confirmación adulta no tiene permisos para este perfil."
                            429 -> "Demasiadas solicitudes. Espera y reintenta."
                            in 500..599 -> "Servidor no disponible. Reintenta; se conservará la misma clave."
                            else -> result.problem.detail ?: "No se pudo importar. Reintenta."
                        }
                    }
                    is NetworkResult.Exception -> {
                        importButton?.isEnabled = true
                        statusText.text = "Sin conexión. Reintenta; la misma clave se conserva y los datos locales no se borran."
                    }
                }
            }
        }
    }

    private fun buildPayload(): LegacyImport {
        val streak = SettingsManager.previewStoryStreak(this)
        val avatar = AvatarProfileStore.load(this)
        val preferences = LegacyImportPreferences(
            soundEnabled = SettingsManager.isSoundEnabled(this),
            storyMusicEnabled = SettingsManager.isStoryMusicEnabled(this),
            questionTimeMs = SettingsManager.getQuestionTimeMs(this),
            storyGameTimeMs = SettingsManager.getStoryGameTimeMs(this),
            storyMapCount = SettingsManager.getStoryMapCount(this),
            language = com.example.aprendemoslavida.utils.LocaleManager.getSavedLanguage(this),
            storyGateTopics = SettingsManager.getStoryGateTopics(this).map {
                when (it) {
                    com.example.aprendemoslavida.story.StoryTopic.NATURAL -> SubjectMapper.CODE_NATURAL
                    com.example.aprendemoslavida.story.StoryTopic.MATH_MULTIPLICATION -> SubjectMapper.TOPIC_MULTIPLICATION
                    com.example.aprendemoslavida.story.StoryTopic.MATH_ADD_SUB -> SubjectMapper.TOPIC_ADD_SUB
                    com.example.aprendemoslavida.story.StoryTopic.ENGLISH -> SubjectMapper.CODE_ENGLISH
                    com.example.aprendemoslavida.story.StoryTopic.SOCIAL -> SubjectMapper.CODE_SOCIAL
                    com.example.aprendemoslavida.story.StoryTopic.LANGUAGE -> SubjectMapper.CODE_LANGUAGE
                    com.example.aprendemoslavida.story.StoryTopic.CASTLES -> SubjectMapper.TOPIC_CASTLES
                }
            }
        )
        return LegacyImport(
            sourceSchemaVersion = 1,
            sourceAppVersion = com.example.aprendemoslavida.BuildConfig.VERSION_NAME,
            globalPoints = SettingsManager.getGlobalPoints(this),
            preferences = preferences,
            avatar = LegacyImportAvatar(
                gender = avatar.gender,
                hairStyle = avatar.hairStyle,
                hairColor = avatar.hairColor,
                eyeShape = avatar.eyeShape,
                eyeColor = avatar.eyeColor,
                mouthShape = avatar.mouthShape,
                mouthColor = avatar.mouthColor,
                skinTone = avatar.skinTone,
                outfit = avatar.outfit
            ),
            streak = LegacyImportStreak(streak.streakDays, SettingsManager.getLastCompletedStoryDay(this)),
            scores = ScoreManager(this).getScoresForLegacyImport().map {
                LegacyImportScore(
                    mode = SubjectMapper.toBackendSubjectCode(it.mode),
                    score = it.score,
                    date = it.date
                )
            }
        )
    }
}
