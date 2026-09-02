package ac.iiit.rtltutor.ai

/**
 * BloomsTagger — classifies text against Bloom's Taxonomy levels (1–6).
 * Level 1=Remember, 2=Understand, 3=Apply, 4=Analyze, 5=Evaluate, 6=Create.
 */
class BloomsTagger {

    // Keyword heuristics per Bloom level (stub implementation)
    private val bloomKeywords = mapOf(
        1 to listOf("define", "list", "recall", "name", "state", "identify"),
        2 to listOf("explain", "describe", "summarize", "classify", "interpret"),
        3 to listOf("calculate", "solve", "apply", "use", "demonstrate", "compute"),
        4 to listOf("analyze", "compare", "distinguish", "differentiate", "examine"),
        5 to listOf("evaluate", "justify", "critique", "assess", "judge", "recommend"),
        6 to listOf("design", "create", "construct", "develop", "formulate", "invent")
    )

    /**
     * Tag [text] with a Bloom's Taxonomy level (1–6).
     * Falls back to level 1 if no keywords match.
     */
    fun tag(text: String): Int {
        val lower = text.lowercase()
        // Check from highest bloom level downward (most complex wins)
        for (level in 6 downTo 1) {
            val keywords = bloomKeywords[level] ?: continue
            if (keywords.any { lower.contains(it) }) {
                return level
            }
        }
        return 1 // default: Remember
    }
}
