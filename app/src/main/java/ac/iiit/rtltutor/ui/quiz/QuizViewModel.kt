package ac.iiit.rtltutor.ui.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ac.iiit.rtltutor.models.QuizQuestion

class QuizViewModel : ViewModel() {

    private val _currentQuestion = MutableLiveData<QuizQuestion?>()
    val currentQuestion: LiveData<QuizQuestion?> = _currentQuestion

    private val _questionIndex = MutableLiveData(0)
    val questionIndex: LiveData<Int> = _questionIndex

    private val _totalQuestions = MutableLiveData(10)
    val totalQuestions: LiveData<Int> = _totalQuestions

    private val _showHint = MutableLiveData(false)
    val showHint: LiveData<Boolean> = _showHint

    private val _feedback = MutableLiveData<String?>()
    val feedback: LiveData<String?> = _feedback

    private val _isAnswerSubmitted = MutableLiveData(false)
    val isAnswerSubmitted: LiveData<Boolean> = _isAnswerSubmitted

    private val stubQuestions = listOf(
        QuizQuestion(
            id = "q1",
            text = "In an RC circuit with R=1kΩ and C=100μF, what is the time constant? Explain its physical significance.",
            bloomLevel = 3,
            kolbStage = "AC",
            answer = "τ = RC = 1000 × 0.0001 = 0.1 seconds",
            hints = listOf("τ = R × C", "Units: Ω × F = seconds"),
            difficulty = 2
        ),
        QuizQuestion(
            id = "q2",
            text = "Why does a capacitor block DC but allow AC to pass? Explain using the concept of reactance.",
            bloomLevel = 4,
            kolbStage = "RO",
            answer = "Capacitive reactance Xc = 1/(2πfC) → ∞ as f→0 (DC)",
            hints = listOf("Think about Xc formula", "What happens to Xc when f=0?"),
            difficulty = 3
        )
    )

    init {
        loadNextQuestion()
    }

    fun loadNextQuestion() {
        val idx = _questionIndex.value ?: 0
        _currentQuestion.value = stubQuestions.getOrNull(idx % stubQuestions.size)
        _showHint.value = false
        _feedback.value = null
        _isAnswerSubmitted.value = false
    }

    fun toggleHint() {
        _showHint.value = !(_showHint.value ?: false)
    }

    fun submitAnswer(answer: String) {
        if (answer.isBlank()) return
        // TODO: evaluate with BloomsTagger / SocraticEngine
        _feedback.value = "Good attempt! The key idea is τ = RC. This tells us how quickly the capacitor charges — at t=τ, the voltage reaches 63.2% of its final value."
        _isAnswerSubmitted.value = true
    }

    fun nextQuestion() {
        _questionIndex.value = (_questionIndex.value ?: 0) + 1
        loadNextQuestion()
    }
}
