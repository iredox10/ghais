package com.ghais.devotion

import com.russhwolf.settings.Settings
import com.ghais.analytics.HookAnalytics
import com.ghais.analytics.HookEvents
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AGENT A7 — gentle devotion streak system.
 *
 * Devotion framed positively, always:
 * - missed single days consume "grace days" (never "freezes");
 * - UI copy says "your N days are safe" (never guilt);
 * - longer breaks are recoverable: 3 gentle days restore the N-day
 *   devotion (restore, never reset-to-zero shame).
 *
 * Day math lives in the pure [evaluateStreak] below so it is unit-testable
 * without a clock. [DevotionStreak] is the persisted [Settings]-backed store
 * (owner-namespaced, same pattern as `HifzMasteryStore`), exposing the streak
 * as StateFlows for the Home card.
 */

/** A grace day is earned per this many consecutive devotion days. */
const val DEVOTION_GRACE_EVERY_DAYS = 7

/** Maximum banked grace days. */
const val DEVOTION_GRACE_MAX = 2

/** Gentle days needed to restore a broken devotion streak. */
const val DEVOTION_RESTORE_GENTLE_DAYS = 3

/** Milestone days that trigger the celebration dialog. */
val DEVOTION_MILESTONES: List<Int> = listOf(7, 30, 100, 365)

/**
 * Outcome of comparing the last active epoch-day against today.
 *
 * - [SAME_DAY]: already recorded today (or clock skew) — no-op.
 * - [FRESH_START]: no previous day recorded — streak begins at 1.
 * - [INCREMENT]: consecutive day — streak grows by 1.
 * - [GRACE_USED]: exactly one missed day, covered by a banked grace day —
 *   streak grows by 1 and one grace day is consumed.
 * - [BROKEN_RECOVERABLE]: longer gap — streak restarts at 1 but the previous
 *   count is kept as the restore target (see [DevotionStreak.restoreTarget]).
 */
enum class StreakResult {
    SAME_DAY,
    FRESH_START,
    INCREMENT,
    GRACE_USED,
    BROKEN_RECOVERABLE,
}

/**
 * Pure day-math for the devotion streak. No clock, no I/O — fully testable.
 *
 * @param lastDay last active epoch-day (`ms / 86_400_000`), `0` = never.
 * @param today current epoch-day on the same scale.
 * @param graceDays banked grace days available right now.
 */
fun evaluateStreak(lastDay: Long, today: Long, graceDays: Int): StreakResult {
    if (lastDay <= 0L) return StreakResult.FRESH_START
    if (today <= lastDay) return StreakResult.SAME_DAY
    if (today == lastDay + 1L) return StreakResult.INCREMENT
    if (today == lastDay + 2L && graceDays > 0) return StreakResult.GRACE_USED
    return StreakResult.BROKEN_RECOVERABLE
}

/**
 * Pure grace-economy helper: banking after a devotion day lands on a
 * week boundary. Returns the new balance, capped at [DEVOTION_GRACE_MAX].
 */
fun earnGraceForWeek(
    currentGrace: Int,
    streakCount: Int,
    every: Int = DEVOTION_GRACE_EVERY_DAYS,
    cap: Int = DEVOTION_GRACE_MAX,
): Int {
    if (streakCount > 0 && streakCount % every == 0) {
        return (currentGrace + 1).coerceAtMost(cap)
    }
    return currentGrace.coerceIn(0, cap)
}

/**
 * Pure restore math: after [gentleDone] gentle days, the devotion count picks
 * up where it left off ([restoreTarget]) plus the gentle days themselves —
 * restore, never reset-to-zero shame.
 */
fun restoredDevotionCount(restoreTarget: Int, gentleDone: Int): Int =
    restoreTarget.coerceAtLeast(0) + gentleDone.coerceAtLeast(0)

/**
 * Persisted gentle-devotion streak store.
 *
 * Keys (owner-namespaced, bare while signed out):
 * - `devotion_count` — current streak in days.
 * - `devotion_longest` — all-time longest streak.
 * - `devotion_last_day` — last active epoch-day.
 * - `devotion_grace` — banked grace-day balance.
 * - `devotion_restore_target` — pre-break count being restored (`0` = none).
 * - `devotion_restore_done` — gentle days completed toward the restore.
 * - `devotion_milestones` — celebrated milestones, comma-separated (`"7,30"`).
 *
 * Day rollover deliberately does NOT zero the count (unlike the Hifz streak):
 * a lapsed streak stays visible so the restore card can say "restore your
 * N-day devotion" instead of shaming with a zero.
 *
 * Sync status: owner-namespaced, NOT synced — local-only like the pre-sync
 * Hifz profile. No export/import seam yet; add one when a cloud counterpart
 * exists.
 */
object DevotionStreak {

    private const val KEY_COUNT = "devotion_count"
    private const val KEY_LONGEST = "devotion_longest"
    private const val KEY_LAST_DAY = "devotion_last_day"
    private const val KEY_GRACE = "devotion_grace"
    private const val KEY_RESTORE_TARGET = "devotion_restore_target"
    private const val KEY_RESTORE_DONE = "devotion_restore_done"
    private const val KEY_MILESTONES = "devotion_milestones"

    private const val LOCAL_OWNER = "local"
    private var ownerId: String = LOCAL_OWNER

    private fun scopedKey(base: String): String =
        if (ownerId == LOCAL_OWNER) base else "$ownerId::$base"

    /** Test seam: Settings factory (unit tests install an in-memory store). */
    internal var settingsFactory: () -> Settings = { Settings() }
    private var cachedSettings: Settings? = null
    private val settings: Settings
        get() = cachedSettings ?: settingsFactory().also { cachedSettings = it }

    /** Drops the cached Settings so the next access rebuilds via [settingsFactory]. */
    internal fun resetSettingsCache() {
        cachedSettings = null
    }

    private val _devotionDays = MutableStateFlow(0)
    val devotionDays: StateFlow<Int> = _devotionDays.asStateFlow()

    private val _longestDays = MutableStateFlow(0)
    val longestDays: StateFlow<Int> = _longestDays.asStateFlow()

    private val _graceDays = MutableStateFlow(0)
    val graceDays: StateFlow<Int> = _graceDays.asStateFlow()

    private val _lastActiveDay = MutableStateFlow(0L)
    val lastActiveDay: StateFlow<Long> = _lastActiveDay.asStateFlow()

    /** Pre-break count being restored, or `0` when not recovering. */
    private val _restoreTarget = MutableStateFlow(0)
    val restoreTarget: StateFlow<Int> = _restoreTarget.asStateFlow()

    /** Gentle days completed toward the current restore. */
    private val _restoreDone = MutableStateFlow(0)
    val restoreDone: StateFlow<Int> = _restoreDone.asStateFlow()

    private val _celebratedMilestones = MutableStateFlow<Set<Int>>(emptySet())
    val celebratedMilestones: StateFlow<Set<Int>> = _celebratedMilestones.asStateFlow()

    init {
        load()
    }

    /**
     * Binds the store to [ownerId]'s namespace (`"local"` unbinds) and reloads.
     * Plain rebind with no data carry-over: each account keeps its own gentle
     * devotion. Idempotent — rebinding the same owner is a no-op.
     */
    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { LOCAL_OWNER }
        if (normalized == this.ownerId) return
        this.ownerId = normalized
        load()
    }

    /**
     * Records one devotion day (a qualifying listen / review / reflection).
     *
     * Applies [evaluateStreak] against the persisted last-active day, then:
     * - [StreakResult.GRACE_USED] consumes one banked grace day, streak + 1;
     * - [StreakResult.BROKEN_RECOVERABLE] restarts at 1 and keeps the old
     *   count as [restoreTarget] with the first gentle day done;
     * - while recovering, consecutive days grow [restoreDone]; completing
     *   [DEVOTION_RESTORE_GENTLE_DAYS] restores the count to
     *   `target + gentleDays` and clears the restore state;
     * - landing on a 7-day boundary banks a grace day (cap
     *   [DEVOTION_GRACE_MAX]).
     *
     * @return the [StreakResult] that was applied.
     */
    fun recordDevotionDay(nowEpochDay: Long = currentEpochDay()): StreakResult {
        val lastDay = readLong(KEY_LAST_DAY, 0L)
        var count = readInt(KEY_COUNT, 0).coerceAtLeast(0)
        var longest = readInt(KEY_LONGEST, 0).coerceAtLeast(0)
        var grace = readInt(KEY_GRACE, 0).coerceIn(0, DEVOTION_GRACE_MAX)
        var target = readInt(KEY_RESTORE_TARGET, 0).coerceAtLeast(0)
        var done = readInt(KEY_RESTORE_DONE, 0).coerceAtLeast(0)

        val outcome = evaluateStreak(lastDay, nowEpochDay, grace)
        when (outcome) {
            StreakResult.SAME_DAY -> return StreakResult.SAME_DAY
            StreakResult.FRESH_START -> {
                count = 1
                target = 0
                done = 0
            }
            StreakResult.INCREMENT -> {
                count = if (count <= 0) 1 else count + 1
                if (target > 0) {
                    done += 1
                    if (done >= DEVOTION_RESTORE_GENTLE_DAYS) {
                        // Restored: pick up where you left off, plus the
                        // gentle days — never just the small post-break count.
                        count = restoredDevotionCount(target, done)
                        target = 0
                        done = 0
                    }
                }
                grace = earnGraceForWeek(grace, count)
            }
            StreakResult.GRACE_USED -> {
                grace = (grace - 1).coerceAtLeast(0)
                count = if (count <= 0) 1 else count + 1
                if (target > 0) {
                    done += 1
                    if (done >= DEVOTION_RESTORE_GENTLE_DAYS) {
                        count = restoredDevotionCount(target, done)
                        target = 0
                        done = 0
                    }
                }
                grace = earnGraceForWeek(grace, count)
            }
            StreakResult.BROKEN_RECOVERABLE -> {
                // Keep the old count as the restore target so the UI can say
                // "restore your N-day devotion" — never a shaming zero.
                target = count.coerceAtLeast(0)
                count = 1
                done = if (target > 0) 1 else 0
                if (target <= 0) target = 0
            }
        }
        longest = maxOf(longest, count, target)

        writeInt(KEY_COUNT, count)
        writeInt(KEY_LONGEST, longest)
        writeLong(KEY_LAST_DAY, nowEpochDay)
        writeInt(KEY_GRACE, grace)
        writeInt(KEY_RESTORE_TARGET, target)
        writeInt(KEY_RESTORE_DONE, done)

        _devotionDays.value = count
        _longestDays.value = longest
        _lastActiveDay.value = nowEpochDay
        _graceDays.value = grace
        _restoreTarget.value = target
        _restoreDone.value = done

        return outcome
    }

    /**
     * The milestone ([DEVOTION_MILESTONES]) just reached that has not been
     * celebrated yet, or `null` when there is nothing new to celebrate.
     */
    fun pendingMilestone(): Int? {
        val count = _devotionDays.value
        return if (count in DEVOTION_MILESTONES && count !in _celebratedMilestones.value) {
            count
        } else {
            null
        }
    }

    /** Marks [days] as celebrated so the dialog shows once per milestone. */
    fun markMilestoneCelebrated(days: Int) {
        if (days !in DEVOTION_MILESTONES) return
        if (_celebratedMilestones.value.contains(days)) return
        val updated = _celebratedMilestones.value + days
        _celebratedMilestones.value = updated
        try {
            settings.putString(scopedKey(KEY_MILESTONES), updated.sorted().joinToString(","))
        } catch (_: Exception) {
        }
        HookAnalytics.track(
            HookEvents.Event.STREAK_MILESTONE,
            mapOf(HookEvents.Prop.STREAK_DAYS to days.toString()),
        )
    }

    /** True while a broken streak is being gently restored. */
    fun isRestoring(): Boolean = _restoreTarget.value > 0

    /** Clears the currently bound owner's devotion state. */
    fun clearAll() {
        for (base in listOf(
            KEY_COUNT, KEY_LONGEST, KEY_LAST_DAY, KEY_GRACE,
            KEY_RESTORE_TARGET, KEY_RESTORE_DONE, KEY_MILESTONES,
        )) {
            try {
                settings.remove(scopedKey(base))
            } catch (_: Exception) {
            }
        }
        _devotionDays.value = 0
        _longestDays.value = 0
        _graceDays.value = 0
        _lastActiveDay.value = 0L
        _restoreTarget.value = 0
        _restoreDone.value = 0
        _celebratedMilestones.value = emptySet()
    }

    private fun currentEpochDay(): Long =
        try {
            Clock.System.now().toEpochMilliseconds() / 86_400_000L
        } catch (_: Exception) {
            0L
        }

    private fun readInt(base: String, default: Int): Int = try {
        settings.getInt(scopedKey(base), default)
    } catch (_: Exception) {
        default
    }

    private fun readLong(base: String, default: Long): Long = try {
        settings.getLong(scopedKey(base), default)
    } catch (_: Exception) {
        default
    }

    private fun writeInt(base: String, value: Int) {
        try {
            settings.putInt(scopedKey(base), value)
        } catch (_: Exception) {
        }
    }

    private fun writeLong(base: String, value: Long) {
        try {
            settings.putLong(scopedKey(base), value)
        } catch (_: Exception) {
        }
    }

    private fun parseMilestones(raw: String): Set<Int> =
        raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in DEVOTION_MILESTONES }
            .toSet()

    private fun load() {
        // No zeroing on lapse: the count stays on screen so a break reads as
        // "pick up where you left off", not as a reset. Only the restore
        // bookkeeping and clamped scalars are re-read verbatim.
        _devotionDays.value = readInt(KEY_COUNT, 0).coerceAtLeast(0)
        _longestDays.value = readInt(KEY_LONGEST, 0).coerceAtLeast(0)
        _graceDays.value = readInt(KEY_GRACE, 0).coerceIn(0, DEVOTION_GRACE_MAX)
        _lastActiveDay.value = readLong(KEY_LAST_DAY, 0L).coerceAtLeast(0L)
        _restoreTarget.value = readInt(KEY_RESTORE_TARGET, 0).coerceAtLeast(0)
        _restoreDone.value = readInt(KEY_RESTORE_DONE, 0).coerceAtLeast(0)
        val rawMilestones = try {
            settings.getString(scopedKey(KEY_MILESTONES), "")
        } catch (_: Exception) {
            ""
        }
        _celebratedMilestones.value = parseMilestones(rawMilestones)
    }
}
