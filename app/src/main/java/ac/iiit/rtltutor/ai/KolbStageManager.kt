package ac.iiit.rtltutor.ai

/**
 * KolbStageManager — tracks and transitions the learner's Kolb learning cycle stage.
 *
 * Kolb Stages:
 *   CE  = Concrete Experience (DO — run the experiment)
 *   RO  = Reflective Observation (REVIEW — observe & reflect)
 *   AC  = Abstract Conceptualization (LEARN — read theory)
 *   AE  = Active Experimentation (TRY — apply to new situation)
 */
class KolbStageManager {

    enum class KolbStage(val label: String) {
        CE("DO"),
        RO("REVIEW"),
        AC("LEARN"),
        AE("TRY")
    }

    private var _currentStage = KolbStage.AC

    /** The learner's current Kolb stage. */
    val currentStage: KolbStage get() = _currentStage

    /** Called when the learner starts an RTL experiment. Transitions to CE. */
    fun onExperimentStart() {
        _currentStage = KolbStage.CE
    }

    /** Called when the experiment ends. Transitions to RO (reflect). */
    fun onExperimentEnd() {
        _currentStage = KolbStage.RO
    }

    /** Called when the learner reads theory or asks conceptual questions. */
    fun onTheoryTriggered() {
        _currentStage = KolbStage.AC
    }

    /** Called when the learner attempts to apply knowledge. */
    fun onApplicationAttempt() {
        _currentStage = KolbStage.AE
    }

    fun stageDisplayName(): String = _currentStage.label

    fun stageCode(): String = _currentStage.name
}
