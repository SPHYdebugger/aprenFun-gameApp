package com.example.aprendemoslavida.activities

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.AuthRepository
import com.example.aprendemoslavida.databinding.DialogGuardianPasswordBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object GuardianPasswordDialog {

    fun show(
        context: Context,
        purpose: String,
        onTokenSuccess: (guardianToken: String) -> Unit
    ) {
        val binding = DialogGuardianPasswordBinding.inflate(LayoutInflater.from(context))
        val apiClient = ApiClient.getInstance(context)
        val authRepository = AuthRepository(apiClient.authApiService, apiClient.tokenStorage)

        val dialog = AlertDialog.Builder(context, R.style.ThemeOverlay_Aprendemos_AlertDialog)
            .setTitle(context.getString(R.string.guardian_dialog_title))
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.guardian_dialog_confirm), null)
            .setNegativeButton(context.getString(R.string.cancel_button), null)
            .create()

        dialog.setOnShowListener {
            val positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setTextColor(context.getColor(R.color.primary))
            positiveBtn.setOnClickListener {
                val password = binding.passwordEditText.text?.toString()?.trim().orEmpty()
                if (password.isBlank()) {
                    binding.passwordInputLayout.error = context.getString(R.string.auth_password_hint)
                    return@setOnClickListener
                }
                binding.passwordInputLayout.error = null
                positiveBtn.isEnabled = false

                CoroutineScope(Dispatchers.IO).launch {
                    val result = authRepository.createGuardianActionToken(password, purpose)
                    withContext(Dispatchers.Main) {
                        positiveBtn.isEnabled = true
                        when (result) {
                            is NetworkResult.Success -> {
                                dialog.dismiss()
                                onTokenSuccess(result.data.token)
                            }
                            is NetworkResult.Error -> {
                                val detail = result.problem.detail ?: context.getString(R.string.time_up)
                                binding.passwordInputLayout.error = detail
                            }
                            is NetworkResult.Exception -> {
                                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(context.getColor(R.color.accent))
        }

        dialog.show()
    }
}
