package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressActivity : BaseActivity() {
    private lateinit var output: TextView
    private lateinit var repository: ProgressRepository
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiClient = ApiClient.getInstance(this)
        repository = ProgressRepository(apiClient.progressApiService, apiClient.leaderboardApiService)
        output = TextView(this).apply { textSize = 18f }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            addView(TextView(this@ProgressActivity).apply { text = "Progreso y recompensas"; textSize = 24f })
            addView(output)
        })
    }

    override fun onResume() {
        super.onResume()
        loadRemoteProgress()
    }

    private fun loadRemoteProgress() {
        val childId = apiClient.tokenStorage.getActiveChildId()
        if (childId.isNullOrBlank()) {
            output.text = "Selecciona un perfil para consultar el progreso online."
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val progress = repository.getPlayerProgress(childId)
            val rewards = repository.getPlayerRewards(childId)
            withContext(Dispatchers.Main) {
                val progressText = when (progress) {
                    is NetworkResult.Success -> "Puntos: ${progress.data.pointsBalance}\nRacha actual: ${progress.data.currentStreak}\nTemas: ${progress.data.topics.size}"
                    is NetworkResult.Error -> "Error de progreso (${progress.statusCode}). Reintenta."
                    is NetworkResult.Exception -> "Sin conexión para consultar el progreso."
                }
                val rewardsText = when (rewards) {
                    is NetworkResult.Success -> "\nLogros: ${rewards.data.achievements.count { it.unlockedAt != null }}\nCosméticos desbloqueados: ${rewards.data.cosmetics.count { it.unlocked }}"
                    is NetworkResult.Error -> "\nError de recompensas (${rewards.statusCode})."
                    is NetworkResult.Exception -> "\nSin conexión para consultar recompensas."
                }
                output.text = progressText + rewardsText
            }
        }
    }
}
