package com.ghais.hifz.quests

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * AGENT A9 — Hifz XP ledger.
 *
 * [xpFor] is the pure rate card; [HifzXpStore] persists a lifetime total plus
 * a weekly total that resets every Monday (device-local, no owner namespace —
 * unlike [com.ghais.data.repository.HifzMasteryStore], XP has no sync seam
 * yet, so it stays a single global counter pair).
 */

/** XP-yielding Hifz moments. */
enum class HifzEvent {
    REVIEW_DONE,
    AYAH_MASTERED,
    QUEST_DONE,
    SURAH_DONE,
}

/** Rate card: REVIEW_DONE=10, AYAH_MASTERED=20, QUEST_DONE=30, SURAH_DONE=200. */
fun xpFor(event: HifzEvent): Int = when (event) {
    HifzEvent.REVIEW_DONE -> 10
    HifzEvent.AYAH_MASTERED -> 20
    HifzEvent.QUEST_DONE -> 30
    HifzEvent.SURAH_DONE -> 200
}

/**
 * Pure Monday helper: epoch day 0 (1970-01-01) was a Thursday, so the
 * Monday-based weekday index is `(epochDay + 3) mod 7` (Mon=0..Sun=6).
 *
 * @return the epoch day of the Monday starting [epochDay]'s week.
 */
fun mondayOf(epochDay: Long): Long {
    val idx = ((epochDay + 3) % 7 + 7) % 7
    return epochDay - idx
}

/**
 * Pure weekly-reset predicate: true when [storedWeekStart] is not the Monday
 * of [todayEpochDay]'s week (covers first launch, where the sentinel differs,
 * and any week turnover).
 */
fun needsWeeklyReset(storedWeekStart: Long, todayEpochDay: Long): Boolean =
    storedWeekStart != mondayOf(todayEpochDay)

object HifzXpStore {

    private const val KEY_TOTAL_XP = "hifz_xp_total"
    private const val KEY_WEEKLY_XP = "hifz_xp_weekly"
    private const val KEY_WEEK_START = "hifz_xp_week_start"

    /** Sentinel: no week has ever been stored (no real Monday equals this). */
    internal const val NO_WEEK_STORED = Long.MIN_VALUE

    /** Test seam: Settings factory (unit tests install an in-memory store). */
    internal var settingsFactory: () -> Settings = { Settings() }
    private var cachedSettings: Settings? = null
    private val settings: Settings
        get() = cachedSettings ?: settingsFactory().also { cachedSettings = it }

    /** Drops the cached Settings so the next access rebuilds via [settingsFactory]. */
    internal fun resetSettingsCache() {
        cachedSettings = null
    }

    private val _totalXp = MutableStateFlow(0)
    val totalXp: StateFlow<Int> = _totalXp.asStateFlow()

    private val _weeklyXp = MutableStateFlow(0)
    val weeklyXp: StateFlow<Int> = _weeklyXp.asStateFlow()

    init {
        refresh()
    }

    /**
     * Awards [xpFor] for [event] onto the lifetime total and the current
     * week's total, rolling the week over first when [todayEpochDay] falls in
     * a new week. The [todayEpochDay] parameter (default: today) is the
     * test seam — pass explicit days to simulate week turnovers.
     *
     * @return the XP awarded for this event.
     */
    fun addXp(event: HifzEvent, todayEpochDay: Long = currentEpochDay()): Int {
        val awarded = xpFor(event)
        val weekStart = mondayOf(todayEpochDay)
        val storedStart = try {
            settings.getLong(KEY_WEEK_START, NO_WEEK_STORED)
        } catch (_: Exception) {
            NO_WEEK_STORED
        }
        val weekBase = if (needsWeeklyReset(storedStart, todayEpochDay)) {
            0
        } else {
            try {
                settings.getInt(KEY_WEEKLY_XP, 0)
            } catch (_: Exception) {
                0
            }
        }
        val totalBase = try {
            settings.getInt(KEY_TOTAL_XP, 0)
        } catch (_: Exception) {
            0
        }
        val newTotal = (totalBase + awarded).coerceAtLeast(0)
        val newWeekly = (weekBase + awarded).coerceAtLeast(0)
        try {
            settings.putInt(KEY_TOTAL_XP, newTotal)
            settings.putInt(KEY_WEEKLY_XP, newWeekly)
            settings.putLong(KEY_WEEK_START, weekStart)
        } catch (_: Exception) {
        }
        _totalXp.value = newTotal
        _weeklyXp.value = newWeekly
        return awarded
    }

    /**
     * Reloads both counters from Settings, zeroing the weekly total when
     * [todayEpochDay] falls in a new week. Call on foreground / day change.
     */
    fun refresh(todayEpochDay: Long = currentEpochDay()) {
        val weekStart = mondayOf(todayEpochDay)
        val storedStart = try {
            settings.getLong(KEY_WEEK_START, NO_WEEK_STORED)
        } catch (_: Exception) {
            NO_WEEK_STORED
        }
        val total = try {
            settings.getInt(KEY_TOTAL_XP, 0)
        } catch (_: Exception) {
            0
        }
        if (needsWeeklyReset(storedStart, todayEpochDay)) {
            try {
                settings.putInt(KEY_WEEKLY_XP, 0)
                settings.putLong(KEY_WEEK_START, weekStart)
            } catch (_: Exception) {
            }
            _totalXp.value = total.coerceAtLeast(0)
            _weeklyXp.value = 0
        } else {
            val weekly = try {
                settings.getInt(KEY_WEEKLY_XP, 0)
            } catch (_: Exception) {
                0
            }
            _totalXp.value = total.coerceAtLeast(0)
            _weeklyXp.value = weekly.coerceAtLeast(0)
        }
    }

    /** Clears lifetime + weekly XP (test / reset hook). */
    fun clearAll() {
        try {
            settings.remove(KEY_TOTAL_XP)
            settings.remove(KEY_WEEKLY_XP)
            settings.remove(KEY_WEEK_START)
        } catch (_: Exception) {
        }
        _totalXp.value = 0
        _weeklyXp.value = 0
    }

    private fun currentEpochDay(): Long =
        Clock.System.now().toEpochMilliseconds() / 86_400_000L
}

/**
 * Hifz XP levels (mirrors library CuratorLevels): lifetime-total tiers.
 * Thresholds scale to the rate card — a full short surah (~20/ayah +
 * 200 bonus) lands around SILVER, a Juz' of steady review reaches GOLD.
 */
enum class HifzXpLevel(val title: String, val minXp: Int) {
    BRONZE("Hifz I", 0),
    SILVER("Hifz II", 200),
    GOLD("Hifz III", 600),
    PLATINUM("Hifz IV", 1200),
}

/** Level for [totalXp] (negatives clamp to BRONZE). Never throws. */
fun hifzXpLevel(totalXp: Int): HifzXpLevel = try {
    HifzXpLevel.entries
        .filter { totalXp >= it.minXp }
        .maxByOrNull { it.minXp }
        ?: HifzXpLevel.BRONZE
} catch (_: Exception) {
    HifzXpLevel.BRONZE
}

/** Next level above [totalXp], or null at cap. Never throws. */
fun nextHifzXpLevel(totalXp: Int): HifzXpLevel? = try {
    HifzXpLevel.entries
        .filter { it.minXp > totalXp }
        .minByOrNull { it.minXp }
} catch (_: Exception) {
    null
}

/** Fraction toward the next level (1f at cap / on failure). Never throws. */
fun hifzXpProgress(totalXp: Int): Float = try {
    val level = hifzXpLevel(totalXp)
    val next = nextHifzXpLevel(totalXp) ?: return 1f
    val span = (next.minXp - level.minXp).coerceAtLeast(1)
    ((totalXp - level.minXp).toFloat() / span).coerceIn(0f, 1f)
} catch (_: Exception) {
    1f
}
