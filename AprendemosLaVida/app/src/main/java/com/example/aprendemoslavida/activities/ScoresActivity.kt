package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.ProgressRepository
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

        val mode = intent.getStringExtra(EXTRA_SCORE_MODE) ?: ScoreManager.MODE_NATURAL
        binding.scoresTitle.text = when (mode) {
            ScoreManager.MODE_MATH -> getString(R.string.scores_title_math)
            ScoreManager.MODE_ENGLISH -> getString(R.string.scores_title_english)
            ScoreManager.MODE_SOCIAL -> getString(R.string.scores_title_social)
            ScoreManager.MODE_LANGUAGE -> getString(R.string.scores_title_language)
            ScoreManager.MODE_STORY -> getString(R.string.scores_title_story)
            else -> getString(R.string.scores_title_natural)
        }

        binding.backButton.setOnClickListener { finish() }

        loadRemoteScoresAndLeaderboard(mode)
    }

    private fun loadRemoteScoresAndLeaderboard(mode: String) {
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNull_or_blank_custom()) {
            loadLocalFallbackScores(mode)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val scoresResult = progressRepository.getPlayerScores(childId!!)
            val leaderboardResult = progressRepository.getPublicLeaderboard(mode = mode)

            withContext(Dispatchers.Main) {
                val items = mutableListOf<String>()

                when (scoresResult) {
                    is NetworkResult.Success -> {
                        val playerScores = scoresResult.data.filter { it.mode.equals(mode, ignoreCase = true) || mode == ScoreManager.MODE_NATURAL }
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
                    else -> {}
                }

                if (items.isEmpty()) {
                    loadLocalFallbackScores(mode)
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

    private fun loadLocalFallbackScores(mode: String) {
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

    private fun String?.isNull_or_blank_custom(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    companion object {
        const val EXTRA_SCORE_MODE = "extra_score_mode"
    }
}
