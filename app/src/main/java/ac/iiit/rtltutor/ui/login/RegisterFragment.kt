package ac.iiit.rtltutor.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegisterUiState.Idle -> {
                    binding.btnRegister.isEnabled = true
                    binding.tvRegError.visibility = View.GONE
                }
                is RegisterUiState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.tvRegError.visibility = View.GONE
                }
                is RegisterUiState.Success -> {
                    // Navigate to home after successful registration
                    findNavController().navigate(R.id.action_register_to_home)
                    viewModel.resetState()
                }
                is RegisterUiState.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.tvRegError.text = state.message
                    binding.tvRegError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRegister.setOnClickListener {
            val username      = binding.etRegUsername.text?.toString().orEmpty().trim()
            val displayName   = binding.etRegDisplayName.text?.toString().orEmpty().trim()
            val password      = binding.etRegPassword.text?.toString().orEmpty()
            val confirmPass   = binding.etRegConfirmPassword.text?.toString().orEmpty()
            viewModel.register(username, displayName, password, confirmPass)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
