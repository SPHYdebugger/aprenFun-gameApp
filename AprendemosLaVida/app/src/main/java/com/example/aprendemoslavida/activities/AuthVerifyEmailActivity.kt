package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.AuthRepository
import com.example.aprendemoslavida.databinding.ActivityAuthVerifyEmailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthVerifyEmailActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthVerifyEmailBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiClient = ApiClient.getInstance(this)
        authRepository = AuthRepository(apiClient.authApiService, apiClient.tokenStorage)

        binding.verifyButton.setOnClickListener {
            performVerify()
        }
    }

    private fun performVerify() {
        val token = binding.codeEditText.text?.toString()?.trim().orEmpty()
        if (token.isBlank()) {
            Toast.makeText(this, "Introduce el código de verificación", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.verifyButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.verifyEmail(token)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.verifyButton.isEnabled = true
                when (result) {
                    is NetworkResult.Success -> {
                        Toast.makeText(this@AuthVerifyEmailActivity, "Correo verificado. Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@AuthVerifyEmailActivity, AuthLoginActivity::class.java))
                        finish()
                    }
                    is NetworkResult.Error -> {
                        val msg = result.problem.detail ?: result.problem.title ?: "Código no válido"
                        Toast.makeText(this@AuthVerifyEmailActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@AuthVerifyEmailActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
