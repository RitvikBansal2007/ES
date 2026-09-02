package ac.iiit.rtltutor.models

data class ExperimentState(
    val voltage: Double,
    val current: Double,
    val frequency: Double,
    val timestamp: Long,
    val rawJson: String
)
