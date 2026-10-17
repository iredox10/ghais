package com.ghais.ui.screens.reciters.today

import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.DEVOTIONAL_TRACK_SLUG

/**
 * Ms per 7-day week, built from the same UTC-millisecond day convention as
 * [YESTERDAY_MILLIS_PER_DAY] (`7 * 86_400_000`). No epoch-day division is
 * involved — see [weeklyTopEntries] for why.
 */
const val WEEK_MILLIS = 7L * YESTERDAY_MILLIS_PER_DAY

/**
 * The week's most heard, from device listening history.
 *
 * Pure, never throws: groups [history] rows whose [JumpBackInItem.lastPlayedTimestampMs]
 * falls in the rolling window `[nowMs - 7 days, nowMs)` by reciter slug, ranked
 * by play count desc then recency desc, capped at [limit]. History rows are
 * already deduped per (surah, reciter), so "plays" = distinct surahs touched
 * per reciter over the week — a session-breadth proxy, not raw ticks.
 *
 * Window choice: a rolling 7-day window rather than week-start-aligned or
 * UTC-day-aligned buckets. Unlike yesterday's day-boundary math in
 * [yesterdayTopEntries] (which floors `nowMs` to UTC midnight to anchor
 * yesterday), this window is anchored directly on `nowMs` and slides with it:
 * "the last seven days" stays meaningful at any hour, so a morning listener
 * and a late-night listener see a window of the same real length, and results
 * never go stale mid-day. It also keeps the rule simple and timezone-free —
 * only the shared UTC-millisecond convention is assumed, never local midnight.
 *
 * Half-open boundaries: timestamps at exactly `nowMs - WEEK_MILLIS` count as
 * played; timestamps at `nowMs` or later are excluded (the future is not
 * "heard yet"). A row at exactly `nowMs - WEEK_MILLIS` is one week old, not
 * "beyond one week", while anything at `nowMs` belongs to the next instant.
 *
 * Null when there is nothing to show (caller falls back to other cards).
 */
fun weeklyTop(
    history: List<JumpBackInItem>,
    nowMs: Long,
    limit: Int = 5,
): List<String>? = weeklyTopEntries(history, nowMs, limit)?.map { it.slug }

/** Ranked weekly entry with play counts. Same contract as [weeklyTop]. */
data class WeeklyTopEntry(
    val slug: String,
    val plays: Int,
    val lastPlayedMs: Long,
)

fun weeklyTopEntries(
    history: List<JumpBackInItem>,
    nowMs: Long,
    limit: Int = 5,
): List<WeeklyTopEntry>? = try {
    if (limit <= 0 || history.isEmpty() || nowMs <= 0L) return null
    val from = nowMs - WEEK_MILLIS
    val to = nowMs
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
        .map { WeeklyTopEntry(it.key, it.value, lastPlayed[it.key] ?: 0L) }
        .takeIf { it.isNotEmpty() }
} catch (_: Exception) {
    null
}
