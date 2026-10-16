package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.AccountRepository
import com.example.aprendemoslavida.data.repository.AuthRepository
import com.example.aprendemoslavida.databinding.ActivityAccountPrivacyBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountPrivacyActivity : BaseActivity() {

    private lateinit var binding: ActivityAccountPrivacyBinding
    private lateinit var accountRepository: AccountRepository
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiClient = ApiClient.getInstance(this)
        accountRepository = AccountRepository(apiClient.accountApiService, apiClient.tokenStorage)
        authRepository = AuthRepository(apiClient.authApiService, apiClient.tokenStorage)

        loadAccountInfo()

        binding.logoutButton.setOnClickListener {
            performLogout()
        }

        binding.logoutAllButton.setOnClickListener {
            performLogoutAll()
        }

        binding.deleteAccountButton.setOnClickListener {
            confirmDeleteAccount()
        }
    }

    private fun loadAccountInfo() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val result = accountRepository.getCurrentAccount()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                when (result) {
                    is NetworkResult.Success -> {
                        binding.emailTextView.text = getString(R.string.account_privacy_email_format, result.data.email)
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this@AccountPrivacyActivity, "Error al obtener cuenta", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@AccountPrivacyActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun performLogout() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.logout()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                redirectToLogin()
            }
        }
    }

    private fun performLogoutAll() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.logoutAll()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                redirectToLogin()
            }
        }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(R.string.account_privacy_delete_account)
            .setMessage(R.string.account_privacy_delete_confirm)
            .setPositiveButton(R.string.exit_confirm_yes) { _, _ ->
                GuardianPasswordDialog.show(
                    context = this,
                    purpose = GuardianTokenRequest.PURPOSE_DELETE_ACCOUNT,
                    onTokenSuccess = { token ->
                        deleteAccount(token)
                    }
                )
            }
            .setNegativeButton(R.string.exit_confirm_no, null)
            .show()
    }

    private fun deleteAccount(guardianToken: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val result = accountRepository.scheduleAccountDeletion(guardianToken)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                when (result) {
                    is NetworkResult.Success -> {
                        Toast.makeText(this@AccountPrivacyActivity, "Borrado de cuenta programado", Toast.LENGTH_LONG).show()
                        redirectToLogin()
                    }
                    is NetworkResult.Error -> {
                        val msg = result.problem.detail ?: result.problem.title ?: "No se pudo borrar la cuenta"
                        Toast.makeText(this@AccountPrivacyActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@AccountPrivacyActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, AuthLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
