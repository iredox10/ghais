package com.ghais.ui.screens.search

/**
 * Agent A12 — exploratory search intents.
 *
 * Pure, platform-free mapping from natural exploratory phrases
 * ("calm recitation for night", "short powerful surahs", ...) to a ranking
 * directive the search screen applies on top of [SearchEngine]'s literal
 * matches. No Compose, no repository, no player imports — safe for
 * commonTest. All inputs/outputs are primitives or plain data.
 */

/** Ranking directive for exploratory queries. */
enum class ExploreSort {
    /** Keep relevance order (engine order). */
    RELEVANCE,

    /** Surahs: fewest ayahs first. */
    SHORT_FIRST,

    /** Reciters: flagged-new voices first. */
    NEW_FIRST
}

/**
 * Parsed exploratory intent.
 *
 * @param mood free-form mood tag ("calm", "focus", "morning", "murattal").
 * @param tempo tempo hint matched substring-style against reciter metadata
 *   ("slow", "medium").
 * @param sort ranking directive.
 */
data class ExploreIntent(
    val mood: String? = null,
    val tempo: String? = null,
    val sort: ExploreSort = ExploreSort.RELEVANCE
)

/**
 * Minimal reciter metadata for intent ranking — primitives only, so callers
 * map from [com.ghais.domain.model.Reciter] (or fakes in tests) without
 * dragging the domain model into the pure core.
 */
data class ExploreReciterMeta(
    val slug: String,
    val name: String,
    val style: String,
    val tempo: String,
    val isNew: Boolean = false
)

/** Minimal surah metadata for intent ranking — primitives only. */
data class ExploreSurahMeta(
    val id: Int,
    val ayahsCount: Int
)

/**
 * Ordered intent outcome as stable identifiers (slugs / surah ids), so the
 * UI resolves display objects itself and this unit never imports them.
 */
data class ExploreOutcome(
    val reciterSlugs: List<String>,
    val surahIds: List<Int>
)

private val CALM_KEYS = listOf(
    "calm", "night", "sleep", "relax", "sooth", "peaceful", "bedtime",
    "rain", "rainy", "soft", "slow recit", "unwind", "insomnia"
)
private val FOCUS_KEYS = listOf(
    "focus", "study", "concentrat", "deep work", "working", "memoriz", "hifz"
)
private val MORNING_KEYS = listOf(
    "morning", "wake", "energy", "energiz", "uplift", "start the day", "fajr"
)
private val MURATTAL_KEYS = listOf("murattal")
private val SHORT_KEYS = listOf("short", "quick", "brief", "tiny", "one-minute", "1-minute")
private val NEW_KEYS = listOf(
    "new voice", "new reciter", "new qari", "fresh voice", "fresh reciter",
    "discover", "emerging", "underrated", "hidden gem", "try someone"
)

private fun String.containsAny(keys: List<String>): Boolean =
    keys.any { this.contains(it) }

/**
 * Maps a natural exploratory phrase to an [ExploreIntent], or null when the
 * query carries no exploratory signal (plain lookup — engine owns it).
 *
 * Matching is case-insensitive substring on the trimmed query. Mood and sort
 * compose: "short calm night" yields calm + [ExploreSort.SHORT_FIRST].
 * Bare reciter/surah names ("Mishary", "Yasin") return null.
 */
fun parseExploreIntent(query: String): ExploreIntent? {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return null

    var mood: String? = null
    var tempo: String? = null
    if (q.containsAny(MURATTAL_KEYS)) {
        mood = "murattal"
        tempo = "slow"
    } else if (q.containsAny(CALM_KEYS)) {
        mood = "calm"
        tempo = "slow"
    } else if (q.containsAny(FOCUS_KEYS)) {
        mood = "focus"
        tempo = "medium"
    } else if (q.containsAny(MORNING_KEYS)) {
        mood = "morning"
        tempo = "medium"
    }

    val sort = when {
        q.containsAny(NEW_KEYS) -> ExploreSort.NEW_FIRST
        q.containsAny(SHORT_KEYS) -> ExploreSort.SHORT_FIRST
        else -> ExploreSort.RELEVANCE
    }

    if (mood == null && tempo == null && sort == ExploreSort.RELEVANCE) return null
    return ExploreIntent(mood = mood, tempo = tempo, sort = sort)
}

/**
 * Applies [intent] to reciter/surah metadata, returning ordered identifiers.
 *
 * Reciters: single stable sort by (mood/tempo-match rank, new-voice rank).
 * Matching rows come first but non-matching rows are NEVER dropped —
 * exploration reorders browsing, it does not empty it. With no mood/tempo
 * constraint the first key is constant, so only [ExploreSort.NEW_FIRST]
 * reorders (isNew first, stable); a plain RELEVANCE intent with no
 * constraints preserves input order exactly.
 *
 * Surahs: [ExploreSort.SHORT_FIRST] orders by ayahsCount ascending
 * (ties by id, deterministic); any other sort preserves input order.
 *
 * Mood matching is substring-based because catalog tempos are free-form
 * ("Slow & Emotive", "Calm & Reflective", "Medium") — exact equality would
 * miss nearly everything.
 */
fun applyIntent(
    reciters: List<ExploreReciterMeta>,
    surahs: List<ExploreSurahMeta>,
    intent: ExploreIntent
): ExploreOutcome {
    val orderedReciters = reciters.sortedWith(
        compareBy(
            { meta ->
                if (intent.mood == null && intent.tempo == null) 0
                else if (reciterMatches(meta, intent)) 0 else 1
            },
            { meta -> if (intent.sort == ExploreSort.NEW_FIRST && meta.isNew) 0 else 1 }
        )
    )
    val orderedSurahs = if (intent.sort == ExploreSort.SHORT_FIRST) {
        surahs.sortedWith(compareBy({ it.ayahsCount }, { it.id }))
    } else {
        surahs.toList()
    }
    return ExploreOutcome(
        reciterSlugs = orderedReciters.map { it.slug },
        surahIds = orderedSurahs.map { it.id }
    )
}

private fun reciterMatches(meta: ExploreReciterMeta, intent: ExploreIntent): Boolean {
    val tempoOk = intent.tempo == null ||
        meta.tempo.contains(intent.tempo, ignoreCase = true)
    val moodOk = intent.mood == null || moodMatchesStyle(meta, intent.mood)
    return tempoOk && moodOk
}

private fun moodMatchesStyle(meta: ExploreReciterMeta, mood: String): Boolean {
    val style = meta.style.lowercase()
    val tempo = meta.tempo.lowercase()
    return when (mood.lowercase()) {
        "calm" -> tempo.contains("slow") || tempo.contains("calm") ||
            tempo.contains("measured") || tempo.contains("reflective") ||
            tempo.contains("serene") || tempo.contains("soft") ||
            tempo.contains("deliberate") || style.contains("mujawwad") ||
            style.contains("tadabbur")
        "focus" -> style.contains("murattal") || tempo.contains("medium") ||
            tempo.contains("steady") || tempo.contains("measured") ||
            tempo.contains("flow")
        "morning" -> style.contains("murattal") || style.contains("haramain") ||
            tempo.contains("medium") || tempo.contains("brisk") ||
            tempo.contains("flowing") || tempo.contains("melodic")
        "murattal" -> style.contains("murattal")
        else -> true
    }
}
