package com.ghais.ui.screens.home.moments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.UserUsageRepository
import com.ghais.analytics.HookAnalytics
import com.ghais.analytics.HookEvents
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.theme.GhaisNoir

/**
 * Agent A8 — Monday week-recap model + card (pure core, thin shelf).
 *
 * The recap reads `UserUsageRepository.dailySeconds` (per-day epoch-day →
 * seconds buckets, this device) and a caller-supplied streak/new-voices
 * pair. Streak OWNERSHIP stays with Agent A7: the shelf below defaults to
 * `stats.daysStreak` (listening streak) but accepts A7's
 * `DevotionStreak.stats` value as an override — this file never records
 * or mutates any streak.
 *
 * Tone doctrine: celebratory of return, zero guilt. A quiet week gets
 * "every return is a beginning", never "you missed N days".
 */

/** Monday in `java.time.DayOfWeek` numbering (1 = Monday .. 7 = Sunday). */
const val MONDAY_DOW: Int = 1

/**
 * Pure visibility gate: the recap card renders on Mondays only.
 *
 * @param todayDow day of week in `java.time.DayOfWeek` numbering
 *   (1 = Monday .. 7 = Sunday). The caller supplies it from the platform
 *   clock — commonMain has no timezone-aware clock; see the Home
 *   insertion snippet (Android: `((Calendar.DAY_OF_WEEK + 5) % 7) + 1`).
 */
fun shouldShowRecap(todayDow: Int): Boolean = todayDow == MONDAY_DOW

/** Render-ready weekly summary. All fields are honest zeros when empty. */
data class WeekRecap(
    /** Whole minutes listened across the 7-day window. */
    val totalMin: Int,
    /** Weekday label ("Mon".."Sun") of the strongest day, "" when none. */
    val bestDayLabel: String,
    /** Whole minutes on the strongest day. */
    val bestDayMin: Int,
    /** New reciter voices tried this week (caller-supplied). */
    val newVoices: Int,
    /** Streak value to display (caller-supplied, A7-owned upstream). */
    val streak: Int,
    /** Headline copy — gentle, celebratory, zero guilt. */
    val headline: String,
)

/**
 * Pure recap builder over a 7-day window.
 *
 * @param dailySeconds up to 7 per-day seconds, oldest → newest. Zeros and
 *   negatives are treated as quiet days, never dropped (a quiet day still
 *   counts toward the week).
 * @param newVoices new reciter voices tried this week.
 * @param streak streak value to echo (A7-owned upstream).
 */
fun buildWeekRecap(
    dailySeconds: List<Long>,
    newVoices: Int,
    streak: Int,
): WeekRecap {
    val window = dailySeconds.takeLast(7)
    val totalMin = (window.sum().coerceAtLeast(0L) / 60L).toInt()
    val bestIndex = window.indices.maxByOrNull { window[it].coerceAtLeast(0L) }
    val bestSecs = bestIndex?.let { window[it].coerceAtLeast(0L) } ?: 0L
    val bestDayMin = (bestSecs / 60L).toInt()
    // Position-only label ("Day 3 of 7"): a bare list carries no weekday
    // anchor, so naming a weekday would be fabrication. Use
    // [buildWeekRecapFromMap] when epoch-day keys are available.
    val bestDayLabel = if (bestIndex != null && bestSecs > 0L && window.size > 1) {
        "Day ${bestIndex + 1} of ${window.size}"
    } else {
        ""
    }
    return WeekRecap(
        totalMin = totalMin,
        bestDayLabel = bestDayLabel,
        bestDayMin = bestDayMin,
        newVoices = newVoices.coerceAtLeast(0),
        streak = streak.coerceAtLeast(0),
        headline = recapHeadline(totalMin, bestDayMin, newVoices.coerceAtLeast(0), streak.coerceAtLeast(0)),
    )
}

/**
 * Recap builder over epoch-day → seconds buckets (the exact shape of
 * `UserUsageRepository.dailySeconds`). Takes the 7 newest days and labels
 * the best day with its real weekday name, derived purely from the epoch
 * day number (1970-01-01 was a Thursday, hence `+ 3`; UTC-based, matching
 * the bucketing in `UserUsageRepository`).
 */
fun buildWeekRecapFromMap(
    daily: Map<Long, Long>,
    newVoices: Int,
    streak: Int,
): WeekRecap {
    val window = daily.entries.sortedBy { it.key }.takeLast(7)
    val base = buildWeekRecap(window.map { it.value }, newVoices, streak)
    val best = window.maxByOrNull { it.value.coerceAtLeast(0L) }
    val label = if (best != null && best.value > 0L && window.size > 1) {
        weekdayName(best.key)
    } else {
        ""
    }
    return base.copy(bestDayLabel = label)
}

private fun weekdayName(epochDay: Long): String {
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val idx = (((epochDay + 3) % 7) + 7) % 7
    return names[idx.toInt()]
}

private fun recapHeadline(totalMin: Int, bestDayMin: Int, newVoices: Int, streak: Int): String {
    if (totalMin <= 0) {
        return "A quiet week — every return is a beginning."
    }
    val parts = mutableListOf<String>()
    when {
        streak >= 7 -> parts += "A full week of return — beautiful consistency."
        totalMin >= 60 -> parts += "Over ${totalMin / 60}h with the Quran this week."
        else -> parts += "$totalMin minutes gathered, moment by moment."
    }
    if (newVoices > 0) {
        parts += if (newVoices == 1) "You welcomed a new voice." else "You welcomed $newVoices new voices."
    } else if (bestDayMin > 0) {
        parts += "Your strongest day held $bestDayMin minutes."
    }
    return parts.joinToString(" ")
}

/**
 * Pure Monday-visible recap card. Renders NOTHING unless
 * [shouldShowRecap] passes — mirroring the discovery shelves'
 * render-nothing-when-empty contract (including its own trailing
 * spacing, so a hidden card leaves zero gap).
 */
@Composable
fun WeekRecapCard(
    recap: WeekRecap,
    todayDow: Int,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowRecap(todayDow)) return
    NoirHeroCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "WEEKLY RECAP",
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = recap.headline,
                color = GhaisNoir.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecapStat(value = "${recap.totalMin}", label = "minutes")
                RecapStat(
                    value = if (recap.bestDayLabel.isNotEmpty()) recap.bestDayLabel else "—",
                    label = "best day",
                )
                RecapStat(value = "${recap.newVoices}", label = "new voices")
                RecapStat(value = "${recap.streak}", label = "day streak")
            }
        }
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun RecapStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
        )
    }
}

/**
 * Self-contained shelf: reads `UserUsageRepository.dailySeconds` +
 * `stats`, builds the recap, renders [WeekRecapCard].
 *
 * @param todayDow platform-supplied day of week (1 = Monday .. 7 = Sunday).
 * @param newVoices weekly-new voices. Defaults to 0: the stats API exposes
 *   all-time `uniqueRecitersCount` only, so a true weekly-new count needs
 *   a week-range query from the stats owner — pass it when available.
 * @param streak streak override. Defaults to `stats.daysStreak`
 *   (listening streak); pass A7's `DevotionStreak` stats value to show the
 *   devotion streak instead.
 */
@Composable
fun WeekRecapShelf(
    todayDow: Int,
    newVoices: Int = 0,
    streak: Int = -1,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowRecap(todayDow)) return
    LaunchedEffect(todayDow) {
        try {
            HookAnalytics.track(
                HookEvents.Event.RECAP_VIEWED,
                mapOf(HookEvents.Prop.SOURCE to "monday_recap"),
            )
        } catch (_: Exception) {
        }
    }
    val daily by UserUsageRepository.dailySeconds.collectAsState()
    val stats by UserUsageRepository.stats.collectAsState()
    val recap = remember(daily, newVoices, streak, stats.daysStreak) {
        try {
            buildWeekRecapFromMap(
                daily = daily,
                newVoices = newVoices,
                streak = if (streak >= 0) streak else stats.daysStreak,
            )
        } catch (_: Exception) {
            WeekRecap(0, "", 0, 0, 0, "A quiet week — every return is a beginning.")
        }
    }
    WeekRecapCard(recap = recap, todayDow = todayDow, modifier = modifier)
}
