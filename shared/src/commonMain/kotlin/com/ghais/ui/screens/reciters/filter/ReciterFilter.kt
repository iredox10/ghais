package com.ghais.ui.screens.reciters.filter

import com.ghais.data.repository.reciterCountryKey
import com.ghais.domain.model.Reciter

/**
 * AGENT A2 — taste-axis filter state for the Reciters tab.
 *
 * Pure Kotlin (no Compose): single-select per axis, every field nullable =
 * "no constraint on this axis". [nation] stores the flag-insensitive country
 * KEY (see [reciterCountryKey]), not the display label, so a bundled
 * "Egypt 🇪🇬" and an admin-created "Egypt" match the same filter.
 */
data class ReciterFilterState(
    val riwayah: String? = null,
    val style: String? = null,
    val tempo: String? = null,
    val mood: MoodFilter? = null,
    val nation: String? = null,
) {
    fun hasActiveFilters(): Boolean =
        riwayah != null || style != null || tempo != null || mood != null || nation != null

    fun activeCount(): Int =
        listOf(riwayah, style, tempo, mood, nation).count { it != null }

    fun cleared(): ReciterFilterState = ReciterFilterState()
}

/**
 * Taste-axis mood derived from [Reciter.style] + [Reciter.tempo] heuristics.
 * A reciter can carry several moods (e.g. slow + measured = calm AND sleep).
 */
enum class MoodFilter(val label: String) {
    CALM("Calm"),
    FOCUS("Focus"),
    SLEEP("Sleep"),
    UPLIFTING("Uplifting"),
}

/**
 * Heuristic mapping from free-form style/tempo text to taste moods.
 *
 * - slow / measured / calm / reflective / serene / deliberate / tearful /
 *   reverent / mujawwad / tadabbur / maqamat -> CALM
 * - slow / calm / tearful / tranquil / soothing / night -> SLEEP
 * - medium / steady / teacher / mu'allim / tajweed / educational / precise /
 *   murattal / melodic / resonant / flowing -> FOCUS (study default)
 * - fast / brisk / powerful / expressive / emotive / haramain / taraweeh ->
 *   UPLIFTING
 *
 * Falls back to FOCUS so every reciter is reachable through some mood chip.
 * Never throws.
 */
fun moodForReciter(reciter: Reciter): Set<MoodFilter> {
    val hay = "${reciter.style} ${reciter.tempo}".lowercase()
    val moods = mutableSetOf<MoodFilter>()
    if (hay.contains("slow") || hay.contains("measured") || hay.contains("calm") ||
        hay.contains("reflective") || hay.contains("serene") || hay.contains("deliberate") ||
        hay.contains("tearful") || hay.contains("reverent") || hay.contains("mujawwad") ||
        hay.contains("tadabbur") || hay.contains("maqamat")
    ) {
        moods += MoodFilter.CALM
    }
    if (hay.contains("slow") || hay.contains("calm") || hay.contains("reflective") ||
        hay.contains("tearful") || hay.contains("night") || hay.contains("tranquil") ||
        hay.contains("soothing")
    ) {
        moods += MoodFilter.SLEEP
    }
    if (hay.contains("medium") || hay.contains("steady") || hay.contains("teacher") ||
        hay.contains("mu'allim") || hay.contains("muallim") || hay.contains("tajweed") ||
        hay.contains("educational") || hay.contains("precise") || hay.contains("clean") ||
        hay.contains("melodic") || hay.contains("resonant") || hay.contains("flowing") ||
        hay.contains("murattal")
    ) {
        moods += MoodFilter.FOCUS
    }
    if (hay.contains("fast") || hay.contains("brisk") || hay.contains("powerful") ||
        hay.contains("expressive") || hay.contains("emotive") || hay.contains("emotional") ||
        hay.contains("haramain") || hay.contains("taraweeh") || hay.contains("rich") ||
        hay.contains("vocal")
    ) {
        moods += MoodFilter.UPLIFTING
    }
    if (moods.isEmpty()) moods += MoodFilter.FOCUS
    return moods
}

/**
 * AND across axes: a reciter must satisfy every non-null constraint.
 * Riwayah / style / tempo compare case-insensitively on trimmed text;
 * nation compares on [reciterCountryKey]; mood hits when the reciter's
 * heuristic mood set contains the selected mood. Empty state returns the
 * input list unchanged (same instance, no copy).
 */
fun applyFilters(reciters: List<Reciter>, state: ReciterFilterState): List<Reciter> {
    if (!state.hasActiveFilters()) return reciters
    return reciters.filter { reciter ->
        (state.riwayah == null || reciter.riwayah.trim().equals(state.riwayah.trim(), ignoreCase = true)) &&
            (state.style == null || reciter.style.trim().equals(state.style.trim(), ignoreCase = true)) &&
            (state.tempo == null || reciter.tempo.trim().equals(state.tempo.trim(), ignoreCase = true)) &&
            (state.mood == null || state.mood in moodForReciter(reciter)) &&
            (state.nation == null || reciterCountryKey(reciter.country) == state.nation)
    }
}

/**
 * Chip options derived from a catalog. Display spellings keep their
 * first-seen form and sort case-insensitively; [nations] keeps the richest
 * raw spelling per country key (same rule the nation reels use, so chip
 * labels match reel headers).
 */
data class ReciterFilterOptions(
    val riwayahs: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val tempos: List<String> = emptyList(),
    val moods: List<MoodFilter> = MoodFilter.values().toList(),
    val nations: List<String> = emptyList(),
)

fun deriveFilterOptions(reciters: List<Reciter>): ReciterFilterOptions {
    fun distinctDisplay(values: Iterable<String>): List<String> {
        val seen = LinkedHashMap<String, String>()
        for (raw in values) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            seen.putIfAbsent(trimmed.lowercase(), trimmed)
        }
        return seen.values.sortedBy { it.lowercase() }
    }
    val nationsByKey = LinkedHashMap<String, String>()
    for (reciter in reciters) {
        val raw = reciter.country.trim()
        if (raw.isEmpty()) continue
        val key = reciterCountryKey(raw)
        if (key.isEmpty()) continue
        val richness = raw.count { ch -> !ch.isLetterOrDigit() && ch != ' ' }
        val current = nationsByKey[key]
        if (current == null || richness > current.count { ch -> !ch.isLetterOrDigit() && ch != ' ' }) {
            nationsByKey[key] = raw
        }
    }
    return ReciterFilterOptions(
        riwayahs = distinctDisplay(reciters.map { it.riwayah }),
        styles = distinctDisplay(reciters.map { it.style }),
        tempos = distinctDisplay(reciters.map { it.tempo }),
        nations = nationsByKey.values.sortedBy { reciterCountryKey(it) },
    )
}
