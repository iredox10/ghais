package com.ghais.data.search

import com.ghais.data.repository.AyahVerse
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.TranslationPrefs
import com.ghais.ui.screens.search.AyahReference
import com.ghais.ui.screens.search.SearchEngine

/**
 * In-memory full-text index over ayah verses, fed surah-by-surah from
 * [QuranAyahRepository.getAyahsForSurah] (or [QuranAyahRepository.getAyahImmediate]
 * batches) by a caller in the UI wave. This object owns NO fetching, NO
 * observing and NO Compose state: it is a plain store + pure ranking function
 * so the UI wave can drive it from `remember(edition)` / `cacheGen` collectors.
 *
 * ## Mapping to [SearchEngine] / the Ayah quick-play card
 *
 * [SearchEngine] today returns a single [AyahReference] parsed from refs like
 * `2:255` — it has NO multi-hit ayah-text result shape. [Hit] is that new,
 * documented shape: `Hit(surahId, ayahNo)` converts 1:1 to
 * `AyahReference(surahId, ayahNo)` for the existing `AyahQuickPlayCard`
 * navigation/play wiring, while [Hit.snippet] feeds the future multi-hit list
 * row (one-line teaser, same single-line-ellipsis treatment as the devotional
 * rows' `ARABIC_SNIPPET_MAX` teaser in `SearchScreen`).
 *
 * ## Script routing
 *
 * A query containing ANY Arabic-script char is matched against the Arabic
 * `textUthmani` only; a pure-Latin query is matched against `translation` plus
 * `transliteration` (when populated — see below). Rationale: cross-script
 * substring matches are never meaningful, and routing keeps Arabic diacritic
 * folding from leaking into Latin lowercasing and vice versa.
 *
 * ## Normalization (Arabic field)
 *
 * | Rule | Range / mapping | Notes |
 * |------|-----------------|-------|
 * | Strip tashkeel | U+064B–U+0652, U+0670 | Exactly this set. The wider repo strip (U+064B–U+065F, U+06D6–U+06ED) is deliberately NOT applied: pause/ornament marks stay significant here. |
 * | Fold alef forms | أ (U+0623), إ (U+0625), آ (U+0622) → ا (U+0627) | Extension: alef wasla ٱ (U+0671) is folded too, mirroring `QuranAyahRepository.normalizeAlef` — Uthmani text spells it `ٱ` and a typed `ا` must still match. |
 * | Taa marbuta | NOT folded (ة stays ة, ه stays ه) | Documented choice: ة/ه are distinct letters in Uthmani orthography and folding them merges distinct words (false positives). Revisit only with a precision/recall case. |
 * | Latin | `lowercase()` on both sides | Translation + transliteration + query. Arabic has no case and is never lowercased. |
 *
 * Both the indexed text and the query go through the same normalization, so a
 * bare `الحمد` query matches a fully vocalized `ٱلْحَمْدُ` verse and vice versa.
 *
 * ## Transliteration verdict
 *
 * `transliteration` is populated ONLY in `QuranAyahRepository.BUNDLED_SEEDS`
 * (surahs 1, 108, 112, 113, 114, 18-partial). `fetchSurahFromApi` builds
 * [AyahVerse] WITHOUT transliteration, so every network-fetched verse carries
 * `transliteration = ""`. The index therefore treats it as a bonus field:
 * matched (lowercased substring) when non-blank, never required, never
 * trusted as the sole signal — a translation-only corpus still searches fine.
 *
 * ## Ranking (total order, lower wins)
 *
 * 1. Field group: Arabic hits (0) before translation/transliteration hits (1).
 *    Moot under script routing today; defined so mixed-script queries stay
 *    deterministic if routing ever relaxes.
 * 2. Match kind: exact-phrase (field == query) > prefix (field starts with
 *    query) > substring (query contained mid-field).
 * 3. Latin sub-field: translation before transliteration on kind ties.
 * 4. Tie-break: surahId, then ayahNo (mushaf order), stable.
 *
 * ## Snippet format
 *
 * Excerpt of the MATCHED field (authentic, un-normalized text — tashkeel kept
 * for Arabic), whitespace-collapsed to one line, windowed around the match
 * with up to ~40 chars of leading context, snapped to word boundaries so a
 * joined Arabic word is never cut mid-glyph, capped at [SNIPPET_MAX_CHARS]
 * with `…` on each trimmed side. Short fields pass through uncapped and
 * unadorned. Length guarantee: `snippet.length <= SNIPPET_MAX_CHARS + 2`.
 *
 * ## Edition scoping + invalidation
 *
 * The index holds ONE edition at a time ([editionId]). `indexSurah` with a new
 * edition id auto-clears as a backstop, but callers MUST still call [clear()]
 * explicitly on a [TranslationPrefs] edition switch (the UI wave's
 * `remember(edition)` hook) — the backstop only fires once the first surah of
 * the new edition is indexed, and until then stale-edition hits would serve.
 * Same staleness source: [QuranAyahRepository.cacheGen] bumps on every cache
 * write, so the UI wave should re-index surahs it reads after a bump.
 *
 * Placeholders (`AyahVerse.isPlaceholder`, e.g. offline `آية رقم …` rows) are
 * never indexed: they would otherwise surface generator text as search hits.
 *
 * Threading: plain mutable state, no locking — confine to the main thread like
 * [SearchEngine]. Every public function is total (never throws); corrupt input
 * degrades to empty results / no-ops.
 */
object AyahSearchIndex {

    /**
     * One ranked verse hit. `(surahId, ayahNo)` maps 1:1 onto [AyahReference];
     * [snippet] is the matched-field excerpt per the format above.
     */
    data class Hit(
        val surahId: Int,
        val ayahNo: Int,
        val snippet: String
    )

    /** Excerpt budget for [Hit.snippet]; see the snippet-format section above. */
    const val SNIPPET_MAX_CHARS: Int = 120

    /** Leading context chars the snippet window aims for before the match. */
    private const val SNIPPET_LEAD_CONTEXT: Int = 40

    private class StoredVerse(
        val surahId: Int,
        val ayahNo: Int,
        /** Whitespace-collapsed authentic Arabic (tashkeel kept) — snippet source. */
        val displayArabic: String,
        val rawTranslation: String,
        val rawTransliteration: String,
        val normArabic: String,
        /** `normArabic[i]` came from `displayArabic[normArabicToRaw[i]]`. */
        val normArabicToRaw: List<Int>,
        val lowerTranslation: String,
        val lowerTransliteration: String
    )

    private class Scored(
        val field: Int,
        val kind: Int,
        val sub: Int,
        val surahId: Int,
        val ayahNo: Int,
        val snippet: String
    )

    private val index = mutableMapOf<Int, List<StoredVerse>>()
    private var currentEdition: String? = null

    /**
     * Index (or re-index) ONE surah. Re-indexing replaces that surah's entries
     * only; every other surah is untouched. Placeholder verses are dropped. An
     * empty surviving list removes the surah from the index.
     *
     * Edition backstop: a different [editionId] from the stored one clears the
     * whole index first (see class docs — callers must still [clear()] on a
     * [TranslationPrefs] switch rather than relying on this).
     */
    fun indexSurah(surahId: Int, verses: List<AyahVerse>, editionId: String) {
        try {
            val edition = editionId.trim()
            if (edition.isEmpty()) return
            val stored = currentEdition
            if (stored != null && stored != edition) clear()
            currentEdition = edition
            val kept = verses
                .filterNot { it.isPlaceholder }
                .map { v ->
                    val displayArabic = collapseWhitespace(v.textUthmani)
                    val rawTranslation = collapseWhitespace(v.translation)
                    val rawTransliteration = collapseWhitespace(v.transliteration)
                    val (norm, map) = normalizeArabic(displayArabic)
                    StoredVerse(
                        surahId = v.surahId,
                        ayahNo = v.ayahNo,
                        displayArabic = displayArabic,
                        rawTranslation = rawTranslation,
                        rawTransliteration = rawTransliteration,
                        normArabic = norm,
                        normArabicToRaw = map,
                        lowerTranslation = rawTranslation.lowercase(),
                        lowerTransliteration = rawTransliteration.lowercase()
                    )
                }
                .filterNot {
                    it.displayArabic.isEmpty() &&
                        it.rawTranslation.isEmpty() &&
                        it.rawTransliteration.isEmpty()
                }
            if (kept.isEmpty()) {
                index.remove(surahId)
                if (index.isEmpty()) currentEdition = null
            } else {
                index[surahId] = kept
            }
        } catch (_: Exception) {
            // Best-effort store: a bad verse must never break search.
        }
    }

    /**
     * Ranked hits for [query], capped at [limit]. Blank queries, queries that
     * normalize to nothing (e.g. tashkeel marks alone), an empty index and
     * `limit <= 0` all yield `emptyList()`. Never throws.
     */
    fun search(query: String, limit: Int = 20): List<Hit> {
        try {
            if (limit <= 0) return emptyList()
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return emptyList()
            val scored = if (trimmed.any { isArabicScript(it) }) {
                searchArabic(trimmed)
            } else {
                searchLatin(trimmed.lowercase())
            }
            return scored
                .sortedWith(
                    compareBy<Scored>({ it.field }, { it.kind }, { it.sub }, { it.surahId }, { it.ayahNo })
                )
                .take(limit)
                .map { Hit(surahId = it.surahId, ayahNo = it.ayahNo, snippet = it.snippet) }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    /** Drops every indexed surah and forgets the edition. */
    fun clear() {
        try {
            index.clear()
            currentEdition = null
        } catch (_: Exception) {
        }
    }

    /** Number of surahs currently indexed. */
    fun indexedSurahCount(): Int = try {
        index.size
    } catch (_: Exception) {
        0
    }

    /** Edition the current contents were indexed under, or null when empty. */
    fun editionId(): String? = try {
        currentEdition
    } catch (_: Exception) {
        null
    }

    // ------------------------------------------------------------------
    // Matching
    // ------------------------------------------------------------------

    private fun searchArabic(rawQuery: String): List<Scored> {
        val (normQuery, _) = normalizeArabic(collapseWhitespace(rawQuery))
        if (normQuery.isEmpty()) return emptyList()
        val out = ArrayList<Scored>()
        for (verses in index.values) {
            for (v in verses) {
                if (v.normArabic.isEmpty()) continue
                val pos = v.normArabic.indexOf(normQuery)
                if (pos < 0) continue
                val kind = when {
                    v.normArabic == normQuery -> 0
                    v.normArabic.startsWith(normQuery) -> 1
                    else -> 2
                }
                val rawStart = v.normArabicToRaw.getOrNull(pos) ?: 0
                val rawEnd = v.normArabicToRaw
                    .getOrNull(pos + normQuery.length - 1)
                    ?.plus(1) ?: v.displayArabic.length
                out.add(
                    Scored(
                        field = 0,
                        kind = kind,
                        sub = 0,
                        surahId = v.surahId,
                        ayahNo = v.ayahNo,
                        snippet = excerpt(v.displayArabic, rawStart, rawEnd)
                    )
                )
            }
        }
        return out
    }

    private fun searchLatin(lowerQuery: String): List<Scored> {
        if (lowerQuery.isEmpty()) return emptyList()
        val out = ArrayList<Scored>()
        for (verses in index.values) {
            for (v in verses) {
                var bestKind = Int.MAX_VALUE
                var bestSub = Int.MAX_VALUE
                var bestSnippet = ""
                if (v.lowerTranslation.isNotEmpty()) {
                    val pos = v.lowerTranslation.indexOf(lowerQuery)
                    if (pos >= 0) {
                        bestKind = matchKind(v.lowerTranslation, lowerQuery)
                        bestSub = 0
                        bestSnippet = excerpt(
                            v.rawTranslation,
                            pos.coerceIn(0, v.rawTranslation.length),
                            (pos + lowerQuery.length).coerceIn(0, v.rawTranslation.length)
                        )
                    }
                }
                if (v.lowerTransliteration.isNotEmpty()) {
                    val pos = v.lowerTransliteration.indexOf(lowerQuery)
                    if (pos >= 0) {
                        val kind = matchKind(v.lowerTransliteration, lowerQuery)
                        if (kind < bestKind || (kind == bestKind && 1 < bestSub)) {
                            bestKind = kind
                            bestSub = 1
                            bestSnippet = excerpt(
                                v.rawTransliteration,
                                pos.coerceIn(0, v.rawTransliteration.length),
                                (pos + lowerQuery.length).coerceIn(0, v.rawTransliteration.length)
                            )
                        }
                    }
                }
                if (bestKind == Int.MAX_VALUE) continue
                out.add(
                    Scored(
                        field = 1,
                        kind = bestKind,
                        sub = bestSub,
                        surahId = v.surahId,
                        ayahNo = v.ayahNo,
                        snippet = bestSnippet
                    )
                )
            }
        }
        return out
    }

    private fun matchKind(field: String, query: String): Int = when {
        field == query -> 0
        field.startsWith(query) -> 1
        else -> 2
    }

    // ------------------------------------------------------------------
    // Normalization
    // ------------------------------------------------------------------

    private fun isTashkeelMark(c: Char): Boolean {
        val v = c.code
        return v in 0x064B..0x0652 || v == 0x0670
    }

    /**
     * أ (U+0623), إ (U+0625), آ (U+0622) → ا, plus alef wasla ٱ (U+0671) →
     * ا as a documented extension mirroring `QuranAyahRepository.normalizeAlef`.
     * Taa marbuta is deliberately NOT folded (see class docs).
     */
    private fun foldAlef(c: Char): Char = when (c.code) {
        0x0622, 0x0623, 0x0625, 0x0671 -> 'ا'
        else -> c
    }

    private fun isArabicScript(c: Char): Boolean {
        val v = c.code
        return v in 0x0600..0x06FF ||
            v in 0x0750..0x077F ||
            v in 0xFB50..0xFDFF ||
            v in 0xFE70..0xFEFF
    }

    private val whitespaceRegex = Regex("\\s+")

    private fun collapseWhitespace(s: String): String =
        s.replace(whitespaceRegex, " ").trim()

    /**
     * Returns the normalized form plus the raw-index map (see [StoredVerse]).
     * Tashkeel marks are dropped (no map entry); every kept char maps 1:1.
     */
    private fun normalizeArabic(raw: String): Pair<String, List<Int>> {
        val out = StringBuilder(raw.length)
        val map = ArrayList<Int>(raw.length)
        for (i in raw.indices) {
            val c = raw[i]
            if (isTashkeelMark(c)) continue
            out.append(foldAlef(c))
            map.add(i)
        }
        return out.toString() to map
    }

    // ------------------------------------------------------------------
    // Snippets
    // ------------------------------------------------------------------

    /**
     * One-line window of [raw] around `[matchStart, matchEndExclusive)`,
     * capped per the class-docs format. Total: clamps all indices, never throws.
     */
    private fun excerpt(raw: String, matchStart: Int, matchEndExclusive: Int): String {
        try {
            if (raw.length <= SNIPPET_MAX_CHARS) return raw
            val max = SNIPPET_MAX_CHARS
            var start = matchStart.coerceIn(0, raw.length)
            // Aim for lead context before the match, then fit the max window.
            start = (start - SNIPPET_LEAD_CONTEXT).coerceIn(0, raw.length)
            var end = (start + max).coerceIn(0, raw.length)
            start = (end - max).coerceIn(0, raw.length)
            if (start > 0) {
                val nextSpace = raw.indexOf(' ', start)
                if (nextSpace != -1 && nextSpace + 1 < end) start = nextSpace + 1
            }
            if (end < raw.length) {
                val prevSpace = raw.lastIndexOf(' ', end - 1)
                if (prevSpace > start) end = prevSpace
            }
            if (start >= end) {
                start = matchStart.coerceIn(0, raw.length)
                end = (start + max).coerceIn(start, raw.length)
            }
            var out = raw.substring(start, end).trim()
            if (out.isEmpty()) out = raw.take(max).trim()
            if (start > 0) out = "…$out"
            if (end < raw.length) out = "$out…"
            return out
        } catch (_: Exception) {
            return raw.take(SNIPPET_MAX_CHARS).trim()
        }
    }
}
