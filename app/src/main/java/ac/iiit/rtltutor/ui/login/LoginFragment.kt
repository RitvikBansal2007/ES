package ac.iiit.rtltutor.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
                is LoginUiState.Idle -> {
                    binding.btnSignIn.isEnabled = true
                    binding.tvError.visibility = View.GONE
                }
                is LoginUiState.Loading -> {
                    binding.btnSignIn.isEnabled = false
                    binding.tvError.visibility = View.GONE
                }
                is LoginUiState.StudentSuccess -> {
                    // Student → go to Home (clear login from back stack)
                    findNavController().navigate(R.id.action_login_to_home)
                    viewModel.resetState()
                }
                is LoginUiState.AdminSuccess -> {
                    // Admin → go to Admin Panel (clear login from back stack)
                    findNavController().navigate(R.id.action_login_to_admin)
                    viewModel.resetState()
                }
                is LoginUiState.Error -> {
                    binding.btnSignIn.isEnabled = true
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val username = binding.etUsername.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.login(username, password)
        }

        binding.btnCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
