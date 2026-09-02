package ac.iiit.rtltutor.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.data.UserRepository
import ac.iiit.rtltutor.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentUser()
        observeViewModel()
        setupClickListeners()
    }

    /** Populate with real user from UserRepository if logged in. */
    private fun loadCurrentUser() {
        val user = UserRepository.currentUser
        if (user != null) {
            viewModel.setUser(user.displayName, user.role.name.lowercase().replaceFirstChar { it.uppercase() })
        }
    }

    private fun observeViewModel() {
        viewModel.displayName.observe(viewLifecycleOwner) { name ->
            binding.tvSettingsUsername.text = name
        }
        viewModel.role.observe(viewLifecycleOwner) { role ->
            binding.tvSettingsRole.text = role
        }
        viewModel.aiModelName.observe(viewLifecycleOwner) { model ->
            binding.tvAiModelValue.text = model
        }
        viewModel.rtlUrl.observe(viewLifecycleOwner) { url ->
            binding.tvRtlUrl.text = url
        }
        viewModel.notificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchNotifications.isChecked = enabled
        }
    }

    private fun setupClickListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, checked ->
            viewModel.toggleNotifications(checked)
        }

        binding.btnSignOut.setOnClickListener {
            UserRepository.logout()
            // Navigate to login and clear the entire back stack
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
