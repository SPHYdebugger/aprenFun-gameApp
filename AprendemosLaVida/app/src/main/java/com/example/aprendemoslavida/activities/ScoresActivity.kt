package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.ProgressRepository
import com.example.aprendemoslavida.data.model.SubjectMapper
import com.example.aprendemoslavida.databinding.ActivityScoresBinding
import com.example.aprendemoslavida.utils.ScoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoresActivity : BaseActivity() {

    private lateinit var binding: ActivityScoresBinding
    private lateinit var progressRepository: ProgressRepository
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient.getInstance(this)
        progressRepository = ProgressRepository(apiClient.progressApiService, apiClient.leaderboardApiService)

        val localMode = intent.getStringExtra(EXTRA_SCORE_MODE) ?: ScoreManager.MODE_NATURAL
        val mode = SubjectMapper.toBackendSubjectCode(localMode)
        binding.scoresTitle.text = when (mode) {
            SubjectMapper.CODE_MATH -> getString(R.string.scores_title_math)
            SubjectMapper.CODE_ENGLISH -> getString(R.string.scores_title_english)
            SubjectMapper.CODE_SOCIAL -> getString(R.string.scores_title_social)
            SubjectMapper.CODE_LANGUAGE -> getString(R.string.scores_title_language)
            SubjectMapper.CODE_STORY -> getString(R.string.scores_title_story)
            else -> getString(R.string.scores_title_natural)
        }

        binding.backButton.setOnClickListener { finish() }

        loadRemoteScoresAndLeaderboard(mode)
    }

    private fun loadRemoteScoresAndLeaderboard(mode: String) {
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNull_or_blank_custom()) {
            showRemoteError("Selecciona un perfil para consultar tus partidas online.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val scoresResult = progressRepository.getPlayerScores(childId!!)
            val leaderboardResult = progressRepository.getPublicLeaderboard(mode = SubjectMapper.toBackendSubjectCode(mode))

            withContext(Dispatchers.Main) {
                val items = mutableListOf<String>()

                when (scoresResult) {
                    is NetworkResult.Success -> {
                        val playerScores = scoresResult.data.filter {
                            it.mode.equals(SubjectMapper.toBackendSubjectCode(mode), ignoreCase = true)
                        }
                        if (playerScores.isNotEmpty()) {
                            items.add("--- Mis Últimas Partidas ---")
                            playerScores.take(5).forEachIndexed { index, s ->
                                items.add("${index + 1}. ${s.score} pts (${s.correctAnswers}/${s.totalQuestions}) - ${s.playedAt.take(10)}")
                            }
                        }
                    }
                    else -> {}
                }

                when (leaderboardResult) {
                    is NetworkResult.Success -> {
                        val entries = leaderboardResult.data.entries
                        if (entries.isNotEmpty()) {
                            items.add("\n--- Ranking Público Semanal ---")
                            entries.forEachIndexed { index, e ->
                                items.add("${index + 1}. ${e.pseudonym} - ${e.score} pts")
                            }
                        }
                    }
                    is NetworkResult.Error -> items.add("Error del ranking (${leaderboardResult.statusCode}). Pulsa atrás y reintenta.")
                    is NetworkResult.Exception -> items.add("No se pudo conectar con el ranking. Pulsa atrás y reintenta.")
                }

                if (items.isEmpty()) {
                    showRemoteError("No hay datos remotos para este modo.")
                } else {
                    binding.scoresList.adapter = ArrayAdapter(
                        this@ScoresActivity,
                        android.R.layout.simple_list_item_1,
                        items
                    )
                }
            }
        }
    }

    private fun showRemoteError(message: String) {
        binding.scoresList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listOf(message)
        )
    }

    private fun String?.isNull_or_blank_custom(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    companion object {
        const val EXTRA_SCORE_MODE = "extra_score_mode"
    }
}
