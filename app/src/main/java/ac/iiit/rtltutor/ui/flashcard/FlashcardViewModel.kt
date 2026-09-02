package ac.iiit.rtltutor.ui.flashcard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class FlashCard(
    val id: String,
    val front: String,
    val back: String,
    val formula: String? = null,
    val bloomLevel: Int = 1
)

class FlashcardViewModel : ViewModel() {

    private val _currentCard = MutableLiveData<FlashCard?>()
    val currentCard: LiveData<FlashCard?> = _currentCard

    private val _cardIndex = MutableLiveData(0)
    val cardIndex: LiveData<Int> = _cardIndex

    private val _totalCards = MutableLiveData(0)
    val totalCards: LiveData<Int> = _totalCards

    private val _isFlipped = MutableLiveData(false)
    val isFlipped: LiveData<Boolean> = _isFlipped

    private val _showActions = MutableLiveData(false)
    val showActions: LiveData<Boolean> = _showActions

    private val cards = listOf(
        FlashCard("f1", "What is the time constant τ of an RC circuit?", "τ = RC — the time for voltage to reach 63.2% of its final value.", "τ = R × C", 1),
        FlashCard("f2", "What is Ohm's Law?", "V = IR — Voltage equals current times resistance.", "V = I × R", 1),
        FlashCard("f3", "What is Thevenin's Theorem?", "Any linear circuit can be replaced by a single voltage source Vth in series with Rth.", null, 2),
        FlashCard("f4", "What is the impedance of a capacitor?", "Zc = 1/(jωC) — impedance decreases with increasing frequency.", "Zc = 1/(jωC)", 3),
        FlashCard("f5", "Define resonance frequency of an LC circuit.", "f0 = 1/(2π√LC) — frequency at which inductive and capacitive reactances are equal.", "f₀ = 1/(2π√LC)", 3)
    )

    init {
        _totalCards.value = cards.size
        loadCard(0)
    }

    private fun loadCard(index: Int) {
        _currentCard.value = cards.getOrNull(index)
        _isFlipped.value = false
        _showActions.value = false
    }

    fun flipCard() {
        val flipped = !(_isFlipped.value ?: false)
        _isFlipped.value = flipped
        if (flipped) _showActions.value = true
    }

    fun rateCard(difficulty: String) {
        // TODO: update spaced-repetition schedule
        val nextIdx = ((_cardIndex.value ?: 0) + 1) % cards.size
        _cardIndex.value = nextIdx
        loadCard(nextIdx)
    }
}
