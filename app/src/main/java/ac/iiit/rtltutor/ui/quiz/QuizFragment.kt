package ac.iiit.rtltutor.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ac.iiit.rtltutor.R
import ac.iiit.rtltutor.databinding.FragmentQuizBinding

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewModel.currentQuestion.observe(viewLifecycleOwner) { q ->
            q ?: return@observe
            binding.tvQuestionText.text = q.text
            binding.tvQuizBloom.text = "Bloom L${q.bloomLevel}"
            binding.tvQuizBloom.setTextColor(getBloomColor(q.bloomLevel))
        }

        viewModel.questionIndex.observe(viewLifecycleOwner) { idx ->
            val total = viewModel.totalQuestions.value ?: 10
            binding.tvQuizProgressLabel.text = "Question ${idx + 1} of $total"
            binding.progressQuiz.progress = ((idx + 1) * 100) / total
        }

        viewModel.showHint.observe(viewLifecycleOwner) { show ->
            binding.cardHint.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.currentQuestion.observe(viewLifecycleOwner) { q ->
            binding.tvHintText.text = q?.hints?.firstOrNull() ?: "No hint available"
        }

        viewModel.feedback.observe(viewLifecycleOwner) { feedback ->
            if (feedback != null) {
                binding.cardFeedback.visibility = View.VISIBLE
                binding.tvFeedback.text = feedback
            } else {
                binding.cardFeedback.visibility = View.GONE
            }
        }

        viewModel.isAnswerSubmitted.observe(viewLifecycleOwner) { submitted ->
            binding.btnSubmit.isEnabled = !submitted
            binding.etAnswer.isEnabled = !submitted
        }
    }

    private fun setupClickListeners() {
        binding.btnHint.setOnClickListener { viewModel.toggleHint() }

        binding.btnSubmit.setOnClickListener {
            val answer = binding.etAnswer.text?.toString().orEmpty().trim()
            viewModel.submitAnswer(answer)
        }

        binding.btnNext.setOnClickListener {
            binding.etAnswer.text?.clear()
            viewModel.nextQuestion()
        }
    }

    private fun getBloomColor(level: Int): Int {
        val res = when (level) {
            1 -> R.color.bloom_1; 2 -> R.color.bloom_2; 3 -> R.color.bloom_3
            4 -> R.color.bloom_4; 5 -> R.color.bloom_5; 6 -> R.color.bloom_6
            else -> R.color.text_secondary
        }
        return resources.getColor(res, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
