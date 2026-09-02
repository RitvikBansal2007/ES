package ac.iiit.rtltutor.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Seed ViewModel with the real logged-in user's name
        ac.iiit.rtltutor.data.UserRepository.currentUser?.let { user ->
            viewModel.setDisplayName(user.displayName)
        }
        observeViewModel()
        setupClickListeners()
        viewModel.refreshData()
    }

    private fun observeViewModel() {
        viewModel.displayName.observe(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = "Hello, $name"
        }

        viewModel.streakDays.observe(viewLifecycleOwner) { days ->
            binding.tvStreakCount.text = days.toString()
        }

        viewModel.sessionCount.observe(viewLifecycleOwner) { count ->
            binding.tvSessionCount.text = count.toString()
        }

        viewModel.currentBloomLevel.observe(viewLifecycleOwner) { level ->
            binding.tvBloomLevel.text = "L$level"
        }

        viewModel.currentKolbStage.observe(viewLifecycleOwner) { stage ->
            binding.tvKolbStage.text = stage
            binding.tvKolbStage.setTextColor(getKolbColor(stage))
        }

        viewModel.rtlConnected.observe(viewLifecycleOwner) { connected ->
            binding.tvRtlStatus.text = if (connected) "Simulator Active" else "Offline"
            binding.tvRtlStatus.setTextColor(
                resources.getColor(
                    if (connected) ac.iiit.rtltutor.R.color.online_green
                    else ac.iiit.rtltutor.R.color.offline_red,
                    null
                )
            )
        }

        viewModel.latestExperimentState.observe(viewLifecycleOwner) { state ->
            state?.let {
                binding.tvRtlVoltage.text = String.format("%.2fV", it.voltage)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnOpenRtl.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_rtl_live)
        }
        binding.cardQuickChat.setOnClickListener {
            findNavController().navigate(R.id.chatFragment)
        }
        binding.cardQuickQuiz.setOnClickListener {
            findNavController().navigate(R.id.quizFragment)
        }
        binding.cardAvatar.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }

    private fun getKolbColor(stage: String): Int {
        val colorRes = when (stage) {
            "CE" -> R.color.kolb_ce
            "RO" -> R.color.kolb_ro
            "AC" -> R.color.kolb_ac
            "AE" -> R.color.kolb_ae
            else -> R.color.text_secondary
        }
        return resources.getColor(colorRes, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
