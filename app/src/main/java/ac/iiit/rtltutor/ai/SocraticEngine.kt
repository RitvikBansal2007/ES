package ac.iiit.rtltutor.ai

import ac.iiit.rtltutor.models.UserLearningProfile

/**
 * SocraticEngine — generates Socratic guiding questions instead of direct answers.
 * Uses BloomsTagger + KolbStageManager to tailor the question.
 */
class SocraticEngine {

    private val bloomsTagger = BloomsTagger()

    /**
     * Generate a Socratic guide response for the given [question],
     * personalized to the user's [profile].
     *
     * @return a guiding question or hint (NOT a direct answer)
     */
    fun generateGuide(question: String, profile: UserLearningProfile): String {
        // TODO: use GenieWrapper to generate a Socratic response
        val bloomLevel = bloomsTagger.tag(question)
        return "Great question! Based on your current mastery at Bloom L$bloomLevel, " +
               "let me ask you this: What do you already know about this topic? " +
               "Think about your past experiments in the RTL lab."
    }
}
