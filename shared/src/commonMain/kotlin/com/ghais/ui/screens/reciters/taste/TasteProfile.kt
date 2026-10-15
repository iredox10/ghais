package com.ghais.ui.screens.reciters.taste

/**
 * Taste profile captured by the first-visit reciter quiz (AGENT A1).
 *
 * Nullable [pace]/[mood] = "not answered yet" (quiz skipped or unfinished).
 * [completed] flips true only when the user finishes step 3 (the A/B voice
 * test), which is what the RecitersScreen gate reads to stop prompting.
 */
enum class Pace {
    SLOW,
    MEASURED,
    FLOWING
}

enum class Mood {
    CALM,
    UPLIFTING,
    FOCUS
}

data class TasteProfile(
    val pace: Pace? = null,
    val mood: Mood? = null,
    val likedSlugs: Set<String> = emptySet(),
    val skippedSlugs: Set<String> = emptySet(),
    val completed: Boolean = false
)
