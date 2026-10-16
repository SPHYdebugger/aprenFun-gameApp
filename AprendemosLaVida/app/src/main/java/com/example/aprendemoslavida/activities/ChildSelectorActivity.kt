package com.example.aprendemoslavida.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aprendemoslavida.data.model.ChildDto
import com.example.aprendemoslavida.data.model.GuardianTokenRequest
import com.example.aprendemoslavida.data.network.ApiClient
import com.example.aprendemoslavida.data.network.NetworkResult
import com.example.aprendemoslavida.data.repository.ProfileRepository
import com.example.aprendemoslavida.databinding.ActivityChildSelectorBinding
import com.example.aprendemoslavida.databinding.ItemChildProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChildSelectorActivity : BaseActivity() {

    private lateinit var binding: ActivityChildSelectorBinding
    private lateinit var profileRepository: ProfileRepository
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient.getInstance(this)
        profileRepository = ProfileRepository(apiClient.profileApiService, apiClient.tokenStorage)

        binding.childrenRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.manageProfilesButton.setOnClickListener {
            GuardianPasswordDialog.show(
                context = this,
                purpose = GuardianTokenRequest.PURPOSE_MANAGE_CHILDREN,
                onTokenSuccess = { token ->
                    val intent = Intent(this, ProfileManagementActivity::class.java)
                    intent.putExtra(ProfileManagementActivity.EXTRA_GUARDIAN_TOKEN, token)
                    startActivity(intent)
                }
            )
        }

        binding.accountPrivacyButton.setOnClickListener {
            startActivity(Intent(this, AccountPrivacyActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadChildrenProfiles()
    }

    private fun loadChildrenProfiles() {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val result = profileRepository.listChildren()
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                when (result) {
                    is NetworkResult.Success -> {
                        val children = result.data
                        val activeId = apiClient.tokenStorage.getActiveChildId()
                        binding.childrenRecyclerView.adapter = ChildrenAdapter(children, activeId) { child ->
                            activateProfile(child.id)
                        }
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this@ChildSelectorActivity, "Error al cargar perfiles", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@ChildSelectorActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun activateProfile(childId: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val result = profileRepository.activateChild(childId)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                when (result) {
                    is NetworkResult.Success -> {
                        startActivity(Intent(this@ChildSelectorActivity, MainMenuActivity::class.java))
                        finish()
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this@ChildSelectorActivity, "No se pudo activar el perfil", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Exception -> {
                        Toast.makeText(this@ChildSelectorActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private class ChildrenAdapter(
        private val list: List<ChildDto>,
        private val activeId: String?,
        private val onSelect: (ChildDto) -> Unit
    ) : RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder>() {

        class ChildViewHolder(val binding: ItemChildProfileBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
            val binding = ItemChildProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ChildViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
            val child = list[position]
            holder.binding.pseudonymTextView.text = child.pseudonym
            holder.binding.ageTextView.text = "${child.age} años"
            holder.binding.selectedCheckImageView.visibility = if (child.id == activeId) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onSelect(child) }
        }

        override fun getItemCount(): Int = list.size
    }
}
