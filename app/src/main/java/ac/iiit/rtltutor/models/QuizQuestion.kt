package ac.iiit.rtltutor.models

data class QuizQuestion(
    val id: String,
    val text: String,
    val bloomLevel: Int,          // 1-6
    val kolbStage: String,        // CE, RO, AC, AE
    val answer: String,
    val hints: List<String>,
    val difficulty: Int           // 1-5
)
