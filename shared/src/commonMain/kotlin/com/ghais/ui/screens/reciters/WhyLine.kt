package com.ghais.ui.screens.reciters

import com.ghais.data.repository.reciterCountryKey
import com.ghais.domain.model.Reciter

private val STYLE_SPLIT = Regex("[^\\p{L}\\p{N}]+")
private val COUNTRY_DECORATION = Regex("[\\p{So}\\p{Cf}\\uFE0E\\uFE0F]+")
private val WHITESPACE = Regex("\\s+")

/**
 * One-line recommendation explanation behind the "More like {name}" rail.
 *
 * Lists the attributes [candidate] shares with [seed], in display order:
 * tempo phrase, riwayah, style tag, country — joined with " · ".
 * Example: `"Same measured pace · Hafs · Egypt"`.
 *
 * Matches [RelatedQaris] semantics: country comparison reuses
 * [reciterCountryKey] (so `"Egypt 🇪🇬"` matches `"Egypt"`), riwayah is
 * trimmed case-insensitive, style is lowercased word-token overlap.
 * A signal scores only when both sides are non-blank.
 *
 * Falls back to `"Voices like {seed}"` when nothing is shared.
 *
 * Pure function: no I/O, never throws on ordinary input.
 */
fun whyLine(seed: Reciter, candidate: Reciter): String {
    val parts = mutableListOf<String>()
    sharedTempoPhrase(seed.tempo, candidate.tempo)?.let(parts::add)
    sharedRiwayah(seed.riwayah, candidate.riwayah)?.let(parts::add)
    sharedStyleTag(seed.style, candidate.style)?.let(parts::add)
    sharedCountry(seed.country, candidate.country)?.let(parts::add)
    if (parts.isEmpty()) return "Voices like ${seed.nameEn}"
    return parts.joinToString(" · ")
}

private fun sharedTempoPhrase(seedTempo: String, candidateTempo: String): String? {
    val seed = seedTempo.trim().lowercase()
    val candidate = candidateTempo.trim().lowercase()
    if (seed.isBlank() || candidate.isBlank() || seed != candidate) return null
    return when (seed) {
        "slow" -> "Same slow pace"
        "medium", "measured", "moderate", "steady" -> "Same measured pace"
        "fast", "brisk", "lively", "quick" -> "Same brisk pace"
        else -> "Same ${seedTempo.trim()} pace"
    }
}

private fun sharedRiwayah(seedRiwayah: String, candidateRiwayah: String): String? {
    val seed = seedRiwayah.trim()
    val candidate = candidateRiwayah.trim()
    if (seed.isBlank() || candidate.isBlank()) return null
    if (!seed.equals(candidate, ignoreCase = true)) return null
    // Display keeps the candidate's spelling, trimmed + capitalized
    // (" hafs " renders "Hafs", never the raw lowercase bleed).
    return candidate.replaceFirstChar { it.uppercase() }
}

private fun styleTags(style: String): List<String> =
    style.lowercase().split(STYLE_SPLIT).filter { it.isNotBlank() }

private fun sharedStyleTag(seedStyle: String, candidateStyle: String): String? {
    if (seedStyle.isBlank() || candidateStyle.isBlank()) return null
    if (seedStyle.trim().equals(candidateStyle.trim(), ignoreCase = true)) {
        return candidateStyle.trim().replaceFirstChar { it.uppercase() }
    }
    val seedTags = styleTags(seedStyle).toSet()
    if (seedTags.isEmpty()) return null
    val shared = styleTags(candidateStyle).firstOrNull { it in seedTags } ?: return null
    return shared.replaceFirstChar { it.uppercase() }
}

private fun displayCountry(raw: String): String =
    raw.replace(COUNTRY_DECORATION, " ").replace(WHITESPACE, " ").trim()

private fun sharedCountry(seedCountry: String, candidateCountry: String): String? {
    val seedKey = reciterCountryKey(seedCountry)
    val candidateKey = reciterCountryKey(candidateCountry)
    if (seedKey.isBlank() || candidateKey.isBlank() || seedKey != candidateKey) return null
    return displayCountry(candidateCountry)
}
