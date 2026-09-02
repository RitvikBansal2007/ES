package ac.iiit.rtltutor.ui.flashcard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ac.iiit.rtltutor.databinding.FragmentFlashcardBinding

class FlashcardFragment : Fragment() {

    private var _binding: FragmentFlashcardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FlashcardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlashcardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewModel.currentCard.observe(viewLifecycleOwner) { card ->
            card ?: return@observe
            binding.tvCardFront.text = card.front
            binding.tvCardBack.text = card.back
            binding.tvFormula.text = card.formula ?: ""
            binding.tvFormula.visibility = if (card.formula != null) View.VISIBLE else View.GONE
        }

        viewModel.cardIndex.observe(viewLifecycleOwner) { idx ->
            val total = viewModel.totalCards.value ?: 0
            binding.tvCardCounter.text = "Card ${idx + 1} of $total"
            binding.progressFlashcard.progress = if (total > 0) ((idx + 1) * 100) / total else 0
        }

        viewModel.isFlipped.observe(viewLifecycleOwner) { flipped ->
            if (flipped) showBack() else showFront()
        }

        viewModel.showActions.observe(viewLifecycleOwner) { show ->
            binding.llActions.visibility = if (show) View.VISIBLE else View.GONE
            binding.llTapHint.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.flCardContainer.setOnClickListener { viewModel.flipCard() }
        binding.cardFront.setOnClickListener { viewModel.flipCard() }
        binding.cardBack.setOnClickListener { viewModel.flipCard() }

        binding.btnEasy.setOnClickListener { viewModel.rateCard("EASY") }
        binding.btnMedium.setOnClickListener { viewModel.rateCard("MEDIUM") }
        binding.btnHard.setOnClickListener { viewModel.rateCard("HARD") }
    }

    private fun showFront() {
        binding.cardFront.visibility = View.VISIBLE
        // Flip animation: front comes in
        val flipIn = ObjectAnimator.ofFloat(binding.cardFront, "rotationY", -90f, 0f).apply {
            duration = 200
        }
        flipIn.start()
        binding.cardBack.visibility = View.GONE
    }

    private fun showBack() {
        binding.cardBack.visibility = View.VISIBLE
        // Flip animation: back comes in
        val flipOut = ObjectAnimator.ofFloat(binding.cardFront, "rotationY", 0f, 90f).apply {
            duration = 150
        }
        val flipIn = ObjectAnimator.ofFloat(binding.cardBack, "rotationY", -90f, 0f).apply {
            duration = 200
            startDelay = 150
        }
        flipOut.start()
        flipIn.start()
        binding.cardFront.postDelayed({ binding.cardFront.visibility = View.GONE }, 150)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
