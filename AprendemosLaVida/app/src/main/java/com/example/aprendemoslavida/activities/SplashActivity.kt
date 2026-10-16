package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.AccountRepository
import com.example.aprendemoslavida.databinding.ActivitySplashBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient.getInstance(this)

        checkSessionAndNavigate()
    }

    private fun checkSessionAndNavigate() {
        if (!apiClient.tokenStorage.isLoggedIn()) {
            navigateToLogin()
            return
        }

        val accountRepository = AccountRepository(apiClient.accountApiService, apiClient.tokenStorage)
        CoroutineScope(Dispatchers.IO).launch {
            val result = accountRepository.getCurrentAccount()
            withContext(Dispatchers.Main) {
                when (result) {
                    is NetworkResult.Success -> {
                        val activeChildId = result.data.activeChildId
                        if (!activeChildId.isNullOrBlank()) {
                            apiClient.tokenStorage.saveActiveChildId(activeChildId)
                            startActivity(Intent(this@SplashActivity, MainMenuActivity::class.java))
                        } else {
                            startActivity(Intent(this@SplashActivity, ChildSelectorActivity::class.java))
                        }
                        finish()
                    }
                    is NetworkResult.Error -> {
                        if (result.statusCode == 401) {
                            apiClient.tokenStorage.clearTokens()
                            navigateToLogin()
                        } else {
                            startActivity(Intent(this@SplashActivity, ChildSelectorActivity::class.java))
                            finish()
                        }
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@SplashActivity, "Sin conexión al servidor aprenFun_back", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SplashActivity, ChildSelectorActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, AuthLoginActivity::class.java))
        finish()
    }
}
