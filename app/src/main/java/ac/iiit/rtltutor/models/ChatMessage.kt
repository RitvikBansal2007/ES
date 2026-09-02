package ac.iiit.rtltutor.models

data class ChatMessage(
    val id: String,
    val userId: String,
    val content: String,
    val isFromAI: Boolean,
    val bloomLevel: Int,           // 1-6
    val kolbStage: String,         // CE, RO, AC, AE
    val timestamp: Long
)
