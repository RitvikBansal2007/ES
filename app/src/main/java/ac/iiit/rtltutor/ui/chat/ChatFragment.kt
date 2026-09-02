package ac.iiit.rtltutor.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val adapter = ChatAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()

        // Start server when Chat screen opens
        viewModel.startServer()
    }

    private fun setupRecyclerView() {
        val lm = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvMessages.layoutManager = lm
        binding.rvMessages.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages.toList()) {
                if (adapter.itemCount > 0) {
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        viewModel.isTyping.observe(viewLifecycleOwner) { typing ->
            binding.layoutThinking.visibility = if (typing) View.VISIBLE else View.GONE
        }

        viewModel.serverState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ServerState.Idle -> {
                    setStatus("Initialising…", R.color.text_muted, grey = true)
                    binding.btnSend.isEnabled = false
                }
                is ServerState.Starting -> {
                    setStatus("Starting llama-server…", R.color.accent_amber, grey = false)
                    binding.btnSend.isEnabled = false
                }
                is ServerState.Log -> {
                    // Show last meaningful log line (e.g. "Loading model…")
                    val short = state.line.take(60).trim()
                    if (short.isNotBlank()) setStatus(short, R.color.accent_amber, grey = false)
                }
                is ServerState.Ready -> {
                    setStatus("Llama 3.2 connected ✓", R.color.online_green, grey = false)
                    binding.btnSend.isEnabled = true
                }
                is ServerState.Error -> {
                    setStatus("Offline — tap to retry", R.color.bloom_5, grey = false)
                    binding.btnSend.isEnabled = true   // still let them try
                    binding.tvServerStatus.setOnClickListener { viewModel.startServer() }
                }
            }
        }
    }

    private fun setStatus(text: String, colorRes: Int, grey: Boolean) {
        binding.tvServerStatus.text = text
        val color = resources.getColor(colorRes, null)
        binding.tvServerStatus.setTextColor(color)
        if (grey) {
            binding.viewServerDot.setBackgroundColor(resources.getColor(R.color.text_muted, null))
        } else {
            binding.viewServerDot.setBackgroundColor(color)
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener { sendMessage() }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        binding.btnMic.setOnClickListener { /* TODO Week 4 */ }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString().orEmpty().trim()
        if (text.isNotBlank()) {
            viewModel.sendMessage(text)
            binding.etMessage.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
