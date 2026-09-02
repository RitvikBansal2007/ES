package ac.iiit.rtltutor.models

data class UserLearningProfile(
    val userId: String,
    val bloomMastery: Map<Int, Float>,     // level 1-6 → mastery 0f-1f
    val weakTopics: List<String>,
    val strongTopics: List<String>,
    val sessionCount: Int
)
