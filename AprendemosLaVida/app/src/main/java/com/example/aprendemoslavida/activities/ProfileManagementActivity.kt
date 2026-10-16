package com.example.aprendemoslavida.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.ProfileRepository
import com.example.aprendemoslavida.databinding.ActivityProfileManagementBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileManagementActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileManagementBinding
    private lateinit var profileRepository: ProfileRepository
    private var guardianToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guardianToken = intent.getStringExtra(EXTRA_GUARDIAN_TOKEN).orEmpty()
        val apiClient = ApiClient.getInstance(this)
        profileRepository = ProfileRepository(apiClient.profileApiService, apiClient.tokenStorage)

        binding.saveButton.setOnClickListener {
            saveChildProfile()
        }
    }

    private fun saveChildProfile() {
        val pseudonym = binding.pseudonymEditText.text?.toString()?.trim().orEmpty()
        val ageStr = binding.ageEditText.text?.toString()?.trim().orEmpty()
        val age = ageStr.toIntOrNull() ?: 0

        if (pseudonym.length < 2 || age < 5 || age > 13) {
            Toast.makeText(this, "El pseudónimo debe tener al menos 2 caracteres y la edad entre 5 y 13 años", Toast.LENGTH_LONG).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = profileRepository.createChild(
                guardianToken = guardianToken,
                pseudonym = pseudonym,
                age = age
            )
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                when (result) {
                    is NetworkResult.Success -> {
                        Toast.makeText(this@ProfileManagementActivity, "Perfil creado con éxito", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is NetworkResult.Error -> {
                        val msg = result.problem.detail ?: result.problem.title ?: "Error al guardar el perfil"
                        Toast.makeText(this@ProfileManagementActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@ProfileManagementActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_GUARDIAN_TOKEN = "extra_guardian_token"
    }
}
