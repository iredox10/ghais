package com.ghais.ui.screens.reciters.today

import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.DEVOTIONAL_TRACK_SLUG

/** Ms per UTC day — same convention as `dailySeconds` and `currentEpochDay`. */
const val YESTERDAY_MILLIS_PER_DAY = 86_400_000L

/**
 * Yesterday's most heard, from device listening history.
 *
 * Pure, never throws: groups [history] rows whose [JumpBackInItem.lastPlayedTimestampMs]
 * falls in [yesterday 00:00, today 00:00) by reciter slug, ranked by play
 * count desc then recency desc, capped at [limit]. History rows are already
 * deduped per (surah, reciter), so "plays" = distinct surahs touched per
 * reciter yesterday — a session-breadth proxy, not raw ticks.
 *
 * Null when there is nothing to show (caller falls back to [voiceOfDay]).
 * Day math is UTC epoch-day division, matching `dailySeconds`/`currentEpochDay`;
 * true device-local midnight would need a timezone offset and would diverge
 * from streak/chart day boundaries.
 */
fun yesterdayTop(
    history: List<JumpBackInItem>,
    nowMs: Long,
    limit: Int = 5,
): List<String>? = try {
    if (limit <= 0 || history.isEmpty() || nowMs <= 0L) return null
    val todayDay = Math.floorDiv(nowMs, YESTERDAY_MILLIS_PER_DAY)
    val from = (todayDay - 1) * YESTERDAY_MILLIS_PER_DAY
    val to = todayDay * YESTERDAY_MILLIS_PER_DAY
    val counts = HashMap<String, Int>()
    val lastPlayed = HashMap<String, Long>()
    for (item in history) {
        val slug = item.reciterSlug.trim()
        if (slug.isEmpty() || slug == DEVOTIONAL_TRACK_SLUG) continue
        val ts = item.lastPlayedTimestampMs
        if (ts < from || ts >= to) continue
        counts[slug] = (counts[slug] ?: 0) + 1
        lastPlayed[slug] = maxOf(lastPlayed[slug] ?: Long.MIN_VALUE, ts)
    }
    if (counts.isEmpty()) return null
    counts.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenByDescending { lastPlayed[it.key] ?: Long.MIN_VALUE }
        )
        .take(limit)
        .map { it.key }
        .takeIf { it.isNotEmpty() }
} catch (_: Exception) {
    null
}
