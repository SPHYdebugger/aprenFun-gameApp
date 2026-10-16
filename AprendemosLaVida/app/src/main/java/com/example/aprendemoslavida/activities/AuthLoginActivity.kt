package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.AuthRepository
import com.example.aprendemoslavida.databinding.ActivityAuthLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthLoginActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthLoginBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiClient = ApiClient.getInstance(this)
        authRepository = AuthRepository(apiClient.authApiService, apiClient.tokenStorage)

        binding.loginButton.setOnClickListener {
            performLogin()
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, AuthRegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEditText.text?.toString()?.trim().orEmpty()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.login(email, password)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.loginButton.isEnabled = true
                when (result) {
                    is NetworkResult.Success -> {
                        startActivity(Intent(this@AuthLoginActivity, ChildSelectorActivity::class.java))
                        finish()
                    }
                    is NetworkResult.Error -> {
                        val msg = result.problem.detail ?: result.problem.title ?: "Error de autenticación"
                        Toast.makeText(this@AuthLoginActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@AuthLoginActivity, "Error de red: ${result.throwable.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
