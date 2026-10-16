package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.AuthRepository
import com.example.aprendemoslavida.databinding.ActivityAuthRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthRegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiClient = ApiClient.getInstance(this)
        authRepository = AuthRepository(apiClient.authApiService, apiClient.tokenStorage)

        binding.registerButton.setOnClickListener {
            performRegister()
        }

        binding.loginLinkButton.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEditText.text?.toString()?.trim().orEmpty()
        val consent = binding.parentalConsentCheckBox.isChecked

        if (email.isBlank() || password.length < 12) {
            Toast.makeText(this, "Introduce un email válido y contraseña de al menos 12 caracteres", Toast.LENGTH_LONG).show()
            return
        }

        if (!consent) {
            Toast.makeText(this, "Debes confirmar el consentimiento de tutor legal", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.registerButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.register(
                email = email,
                password = password,
                parentalConsent = consent
            )
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.registerButton.isEnabled = true
                when (result) {
                    is NetworkResult.Success -> {
                        Toast.makeText(this@AuthRegisterActivity, result.data.message, Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@AuthRegisterActivity, AuthVerifyEmailActivity::class.java))
                        finish()
                    }
                    is NetworkResult.Error -> {
                        val msg = result.problem.detail ?: result.problem.title ?: "Error en el registro"
                        Toast.makeText(this@AuthRegisterActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@AuthRegisterActivity, "Error de red: ${result.throwable.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
