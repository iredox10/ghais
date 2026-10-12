package com.ghais.data.repository

import com.ghais.data.seed.QuranData
import com.russhwolf.settings.Settings
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Mastery level of an individual ayah in Hifz (memorization).
 *
 * Progression cycle:
 * [NEW] -> [REVIEW_NEEDED] -> [MASTERED] -> [NEW]
 */
enum class MasteryStatus {
    /** Ayah has not been memorized or engaged yet. Default state. */
    NEW,

    /** Ayah is weak, shaky, or flagged for revision. */
    REVIEW_NEEDED,

    /** Ayah is firmly memorized and mastered. */
    MASTERED;

    /**
     * Cycles to the next status in the progression:
     * NEW -> REVIEW_NEEDED -> MASTERED -> NEW.
     */
    fun next(): MasteryStatus = when (this) {
        NEW -> REVIEW_NEEDED
        REVIEW_NEEDED -> MASTERED
        MASTERED -> NEW
    }
}

/** Daily Hifz review goal used when nothing is persisted, and the [HifzSnapshot] default. */
const val DEFAULT_HIFZ_DAILY_GOAL = 5

/**
 * A complete capture of ONE owner's Hifz profile: every non-[MasteryStatus.NEW]
 * ayah status plus the four scalar counters, read verbatim (no day rollover
 * applied) by [HifzMasteryStore.exportSnapshot] and written verbatim by
 * [HifzMasteryStore.importSnapshot].
 *
 * [dueDays] / [intervals] carry the SM-2-lite SRS state (see [HifzMasteryStore.dueToday]):
 * per-ayah due epoch-day and spacing interval. They default to empty so older
 * callers and cloud documents without SRS data still restore.
 *
 * This is the unit of sync, not a wire format. A `hifz_mastery` document would
 * need [statuses] as rows of `surah_id` / `ayah_no` / `status` (the status as
 * its [MasteryStatus] name) plus `daily_goal_count`, `streak_days`,
 * `today_reviewed_count` and `last_review_day` — see the store KDoc.
 */
data class HifzSnapshot(
    val statuses: Map<Pair<Int, Int>, MasteryStatus> = emptyMap(),
    val dailyGoalCount: Int = DEFAULT_HIFZ_DAILY_GOAL,
    val streakDays: Int = 0,
    val todayReviewedCount: Int = 0,
    val lastReviewDay: Long = 0L,
    val dueDays: Map<Pair<Int, Int>, Long> = emptyMap(),
    val intervals: Map<Pair<Int, Int>, Int> = emptyMap(),
)

/**
 * HifzMasteryStore:
 *
 * Persisted store for Quranic memorization progress (Hifz), weak ayah tracking,
 * daily review goals, and review streaks.
 *
 * Stored via multiplatform [Settings] with keys:
 * - Ayah status: `mastery_{surahId}_{ayahNo}` (one key per ayah)
 * - SRS state (SM-2-lite, additive): `due_{surahId}_{ayahNo}` (Long epoch day
 *   the ayah is next due) and `interval_{surahId}_{ayahNo}` (Int spacing
 *   interval in days). Written on every non-NEW [setStatus], removed on NEW,
 *   backfilled on [load] for legacy rows (due = first-seen day, interval = 1).
 * - Goal & Streak: `hifz_daily_goal_count`, `hifz_today_reviewed_count`,
 *                  `hifz_last_review_day`, `hifz_streak_days`.
 *
 * ## Sync status: owner-namespaced, NOT synced
 * Everything lives under an owner namespace (`<userId>::` when bound, bare
 * while signed out) and [setOwner] now performs a safe first-owner adoption, so
 * the store is *ready* to be wired into the session collector like
 * `FavoritesStore` / `OnboardingStore`. But there is **no `hifz_mastery`
 * collection behind it yet**: nothing pushes a [HifzSnapshot] and nothing pulls
 * one, so a second device starts from an empty profile. [exportSnapshot] /
 * [importSnapshot] exist so the sync layer has a correct push/pull seam; they
 * are not called by any code in the app today. Do not assume a profile is
 * present on a fresh install — it will be empty until the pull runs.
 */
object HifzMasteryStore {

    private const val KEY_DAILY_GOAL = "hifz_daily_goal_count"
    private const val KEY_TODAY_REVIEWED_COUNT = "hifz_today_reviewed_count"
    private const val KEY_LAST_REVIEW_DAY = "hifz_last_review_day"
    private const val KEY_STREAK_DAYS = "hifz_streak_days"

    /** Prefix of the per-ayah keys. Also the whole-namespace prefix for `"local"`. */
    private const val MASTERY_KEY_PREFIX = "mastery_"

    /** Prefixes of the per-ayah SM-2-lite SRS keys (see [dueToday]). */
    private const val DUE_KEY_PREFIX = "due_"
    private const val INTERVAL_KEY_PREFIX = "interval_"

    /** The unbound / pre-auth owner. [key] and [masteryPrefix] collapse to bare keys for it. */
    private const val LOCAL_OWNER = "local"

    private var ownerId: String = LOCAL_OWNER

    /**
     * Namespace prefix for [owner]. Empty for [LOCAL_OWNER], so an unbound
     * store reads and writes the bare base keys.
     *
     * This assumes no owner id can start with [MASTERY_KEY_PREFIX] (Appwrite
     * user ids are opaque uids), which is what keeps a bare `mastery_…` scan
     * from ever picking up another account's rows.
     */
    private fun ownerPrefix(owner: String): String =
        if (owner == LOCAL_OWNER) "" else "$owner::"

    private fun scopedKey(owner: String, base: String): String = ownerPrefix(owner) + base

    private fun key(base: String): String = scopedKey(ownerId, base)

    private fun masteryPrefix(owner: String): String = scopedKey(owner, MASTERY_KEY_PREFIX)

    private fun ayahKey(surahId: Int, ayahNo: Int): String =
        "$MASTERY_KEY_PREFIX${surahId}_$ayahNo"

    private fun dueKey(surahId: Int, ayahNo: Int): String =
        "$DUE_KEY_PREFIX${surahId}_$ayahNo"

    private fun intervalKey(surahId: Int, ayahNo: Int): String =
        "$INTERVAL_KEY_PREFIX${surahId}_$ayahNo"

    /** Test seam: Settings factory (unit tests install an in-memory store). */
    internal var settingsFactory: () -> Settings = { Settings() }
    private var cachedSettings: Settings? = null
    private val settings: Settings
        get() = cachedSettings ?: settingsFactory().also { cachedSettings = it }

    /** Drops the cached Settings so the next access rebuilds via [settingsFactory]. */
    internal fun resetSettingsCache() {
        cachedSettings = null
    }

    // In-memory cache of non-NEW mastery statuses: Pair(surahId, ayahNo) -> MasteryStatus
    private val _masteryMap = MutableStateFlow<Map<Pair<Int, Int>, MasteryStatus>>(emptyMap())
    val masteryMap: StateFlow<Map<Pair<Int, Int>, MasteryStatus>> = _masteryMap.asStateFlow()

    // Total mastered ayahs across the Quran
    private val _totalMasteredCount = MutableStateFlow(0)
    val totalMasteredCount: StateFlow<Int> = _totalMasteredCount.asStateFlow()

    // Cached sorted list of weak ayahs (REVIEW_NEEDED)
    private val _weakAyahs = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val weakAyahs: StateFlow<List<Pair<Int, Int>>> = _weakAyahs.asStateFlow()

    // SM-2-lite SRS state: per-ayah due epoch-day and spacing interval (days).
    // Only non-NEW ayahs are tracked; a missing entry means interval 1 and a
    // due day of first review (see [getDueDay] / [getInterval]).
    private val _dueDayMap = MutableStateFlow<Map<Pair<Int, Int>, Long>>(emptyMap())
    val dueDayMap: StateFlow<Map<Pair<Int, Int>, Long>> = _dueDayMap.asStateFlow()

    private val _intervalMap = MutableStateFlow<Map<Pair<Int, Int>, Int>>(emptyMap())
    val intervalMap: StateFlow<Map<Pair<Int, Int>, Int>> = _intervalMap.asStateFlow()

    // Daily memorization/review goal (default 5 ayahs)
    private val _dailyGoalCount = MutableStateFlow(DEFAULT_HIFZ_DAILY_GOAL)
    val dailyGoalCount: StateFlow<Int> = _dailyGoalCount.asStateFlow()

    // Ayahs reviewed today
    private val _todayReviewedCount = MutableStateFlow(0)
    val todayReviewedCount: StateFlow<Int> = _todayReviewedCount.asStateFlow()

    // Review streak in days
    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    init {
        load()
    }

    /**
     * Binds the store to [ownerId]'s namespace (`"local"` unbinds) and reloads.
     *
     * **First-owner adoption.** Because there is no cloud counterpart yet,
     * every key in a new namespace is empty, so a plain rebind would hide a
     * returning user's memorization progress (and reset their streak) with no
     * pull to restore it. So a bind that LEAVES the unbound namespace also
     * carries the unbound data over: see [adoptLocalNamespace]. Data is only
     * ever moved *out of* the unbound namespace and *into an empty one*, so an
     * account -> account switch can never move one account's Hifz profile
     * under another. Idempotent — rebinding the same owner is a no-op.
     */
    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { LOCAL_OWNER }
        if (normalized == this.ownerId) return
        // Only a bind away from the unbound namespace may adopt; switching
        // straight from one account to another carries nothing at all.
        val carried = if (this.ownerId == LOCAL_OWNER) readNamespace(LOCAL_OWNER) else null
        this.ownerId = normalized
        load()
        carried?.let { adoptLocalNamespace(it) }
    }

    /**
     * Moves a snapshot captured from the unbound namespace into the namespace
     * that was just bound, then deletes the source.
     *
     * Three properties make it safe to leave in the bind path:
     * - it runs only when the store was unbound a moment ago (see [setOwner]),
     *   so an account -> account switch cannot move data;
     * - it refuses to touch an account that already has Hifz data of its own,
     *   so a returning user's real progress is never overwritten by whatever
     *   happened while signed out;
     * - it deletes the `local` keys only after they are written, so a failed
     *   write leaves the source intact, and once it has succeeded there is
     *   nothing left for a later account to re-adopt. Adoption therefore
     *   happens at most once per install.
     *
     * The scalars are copied verbatim (goal, streak, `last_review_day` and
     * today's count as one unit) and the reload that follows is the ordinary
     * [load], so the usual day rollover ages a restored streak instead of the
     * copy itself truncating or resetting it.
     */
    private fun adoptLocalNamespace(carried: HifzSnapshot) {
        // Nothing but untouched defaults: adopting would only copy noise.
        if (carried.isPristine()) return
        // The incoming namespace is not empty: never overwrite real data.
        if (readNamespace(ownerId) != null) return
        writeNamespace(ownerId, carried)
        clearNamespace(LOCAL_OWNER)
        load()
    }

    /**
     * Reads one namespace verbatim, or `null` when that namespace holds no keys
     * at all — which is what "the incoming namespace is empty" means here.
     *
     * Both halves of the store are covered: the per-ayah
     * `mastery_{surahId}_{ayahNo}` rows and the four scalars. [MasteryStatus.NEW]
     * rows are dropped exactly as [load] drops them, and an absent scalar falls
     * back to the same default [load] would use.
     */
    private fun readNamespace(owner: String): HifzSnapshot? = try {
        val prefix = masteryPrefix(owner)
        val statuses = mutableMapOf<Pair<Int, Int>, MasteryStatus>()
        for (k in settings.keys) {
            if (!k.startsWith(prefix)) continue
            val parts = k.removePrefix(prefix).split("_")
            if (parts.size != 2) continue
            val surahId = parts[0].toIntOrNull() ?: continue
            val ayahNo = parts[1].toIntOrNull() ?: continue
            val status = try {
                MasteryStatus.valueOf(settings.getString(k, ""))
            } catch (_: Exception) {
                MasteryStatus.NEW
            }
            if (status != MasteryStatus.NEW) statuses[surahId to ayahNo] = status
        }
        // SRS halves: same per-ayah shape under their own prefixes.
        val dueDays = mutableMapOf<Pair<Int, Int>, Long>()
        val duePrefix = scopedKey(owner, DUE_KEY_PREFIX)
        for (k in settings.keys) {
            if (!k.startsWith(duePrefix)) continue
            val parts = k.removePrefix(duePrefix).split("_")
            if (parts.size != 2) continue
            val surahId = parts[0].toIntOrNull() ?: continue
            val ayahNo = parts[1].toIntOrNull() ?: continue
            val due = try {
                settings.getLongOrNull(k)
            } catch (_: Exception) {
                null
            } ?: continue
            dueDays[surahId to ayahNo] = due
        }
        val intervals = mutableMapOf<Pair<Int, Int>, Int>()
        val intervalPrefix = scopedKey(owner, INTERVAL_KEY_PREFIX)
        for (k in settings.keys) {
            if (!k.startsWith(intervalPrefix)) continue
            val parts = k.removePrefix(intervalPrefix).split("_")
            if (parts.size != 2) continue
            val surahId = parts[0].toIntOrNull() ?: continue
            val ayahNo = parts[1].toIntOrNull() ?: continue
            val interval = try {
                settings.getIntOrNull(k)
            } catch (_: Exception) {
                null
            } ?: continue
            if (interval > 0) intervals[surahId to ayahNo] = interval
        }
        val goal = settings.getIntOrNull(scopedKey(owner, KEY_DAILY_GOAL))
        val streak = settings.getIntOrNull(scopedKey(owner, KEY_STREAK_DAYS))
        val reviewed = settings.getIntOrNull(scopedKey(owner, KEY_TODAY_REVIEWED_COUNT))
        val lastDay = settings.getLongOrNull(scopedKey(owner, KEY_LAST_REVIEW_DAY))
        if (statuses.isEmpty() && dueDays.isEmpty() && intervals.isEmpty() && goal == null && streak == null && reviewed == null && lastDay == null) {
            null
        } else {
            HifzSnapshot(
                statuses = statuses,
                dailyGoalCount = if (goal != null && goal > 0) goal else DEFAULT_HIFZ_DAILY_GOAL,
                streakDays = streak ?: 0,
                todayReviewedCount = reviewed ?: 0,
                lastReviewDay = lastDay ?: 0L,
                dueDays = dueDays,
                intervals = intervals,
            )
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Writes a snapshot into [owner]'s namespace verbatim, per-ayah rows and
     * scalars alike, so a streak always lands together with the
     * `hifz_last_review_day` that gives it meaning. Never clears: callers
     * decide that ([adoptLocalNamespace] writes into a namespace it has just
     * proved empty, [importSnapshot] clears first).
     */
    private fun writeNamespace(owner: String, snapshot: HifzSnapshot) {
        for ((coord, status) in snapshot.statuses) {
            if (status == MasteryStatus.NEW) continue
            try {
                settings.putString(scopedKey(owner, ayahKey(coord.first, coord.second)), status.name)
            } catch (_: Exception) {
            }
        }
        for ((coord, due) in snapshot.dueDays) {
            try {
                settings.putLong(scopedKey(owner, dueKey(coord.first, coord.second)), due)
            } catch (_: Exception) {
            }
        }
        for ((coord, interval) in snapshot.intervals) {
            if (interval <= 0) continue
            try {
                settings.putInt(scopedKey(owner, intervalKey(coord.first, coord.second)), interval)
            } catch (_: Exception) {
            }
        }
        try {
            settings.putInt(scopedKey(owner, KEY_DAILY_GOAL), snapshot.dailyGoalCount.coerceAtLeast(1))
        } catch (_: Exception) {
        }
        try {
            settings.putInt(scopedKey(owner, KEY_STREAK_DAYS), snapshot.streakDays.coerceAtLeast(0))
        } catch (_: Exception) {
        }
        try {
            settings.putInt(
                scopedKey(owner, KEY_TODAY_REVIEWED_COUNT),
                snapshot.todayReviewedCount.coerceAtLeast(0),
            )
        } catch (_: Exception) {
        }
        try {
            settings.putLong(scopedKey(owner, KEY_LAST_REVIEW_DAY), snapshot.lastReviewDay.coerceAtLeast(0L))
        } catch (_: Exception) {
        }
    }

    /**
     * Removes every key belonging to [owner] — per-ayah rows *and* scalars. No
     * in-memory state is touched; callers reload or reset the flows themselves.
     */
    private fun clearNamespace(owner: String) {
        try {
            val prefix = masteryPrefix(owner)
            for (k in settings.keys.filter { it.startsWith(prefix) }) {
                settings.remove(k)
            }
            val duePrefix = scopedKey(owner, DUE_KEY_PREFIX)
            for (k in settings.keys.filter { it.startsWith(duePrefix) }) {
                settings.remove(k)
            }
            val intervalPrefix = scopedKey(owner, INTERVAL_KEY_PREFIX)
            for (k in settings.keys.filter { it.startsWith(intervalPrefix) }) {
                settings.remove(k)
            }
            settings.remove(scopedKey(owner, KEY_DAILY_GOAL))
            settings.remove(scopedKey(owner, KEY_TODAY_REVIEWED_COUNT))
            settings.remove(scopedKey(owner, KEY_LAST_REVIEW_DAY))
            settings.remove(scopedKey(owner, KEY_STREAK_DAYS))
        } catch (_: Exception) {
        }
    }

    /** A snapshot indistinguishable from a freshly installed store. */
    private fun HifzSnapshot.isPristine(): Boolean =
        statuses.isEmpty() &&
            dueDays.isEmpty() &&
            intervals.isEmpty() &&
            dailyGoalCount == DEFAULT_HIFZ_DAILY_GOAL &&
            streakDays == 0 &&
            todayReviewedCount == 0 &&
            lastReviewDay == 0L

    /**
     * Captures the currently bound owner's profile for a push.
     *
     * [statuses] is ordered by surah then ayah so two exports of the same state
     * produce the same document, and carries only non-[MasteryStatus.NEW] rows
     * (an unset ayah and a `NEW` ayah are the same thing, per [getStatus]).
     * `streakDays` / `todayReviewedCount` are the day-rollover-corrected values
     * the UI is showing; `lastReviewDay` is the raw persisted epoch day, so a
     * restored copy can still age correctly on another device.
     *
     * Unbound, this reports the device-global `local` profile — read it only
     * once an owner is bound, which is what the push path wants.
     */
    fun exportSnapshot(): HifzSnapshot = HifzSnapshot(
        statuses = _masteryMap.value.entries
            .asSequence()
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .associate { it.key to it.value },
        dailyGoalCount = _dailyGoalCount.value,
        streakDays = _streakDays.value,
        todayReviewedCount = _todayReviewedCount.value,
        lastReviewDay = try {
            settings.getLong(key(KEY_LAST_REVIEW_DAY), 0L)
        } catch (_: Exception) {
            0L
        },
        dueDays = _dueDayMap.value.entries
            .asSequence()
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .associate { it.key to it.value },
        intervals = _intervalMap.value.entries
            .asSequence()
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .associate { it.key to it.value },
    )

    /**
     * Replaces the currently bound owner's profile with [snapshot] — the pull
     * direction, for the sync layer to call once it has decided the cloud copy
     * wins. It is authoritative: ayahs present locally but absent from
     * [HifzSnapshot.statuses] are dropped, so this is not a merge.
     *
     * Two ordering rules, both the caller's to honour:
     * - bind the owner FIRST. Unbound, this would write into the device-global
     *   `local` namespace, where the next sign-in would adopt it;
     * - decide the direction before writing, and prefer merging over calling
     *   this when a local change is newer than the document.
     *
     * [HifzSnapshot.lastReviewDay] is authoritative for streak validity, exactly
     * as it is locally: a snapshot with `lastReviewDay == 0` restores with an
     * empty streak, and one whose last review was days ago is aged by the same
     * day rollover [load] applies, so a cross-device restore cannot resurrect a
     * streak that has already lapsed.
     */
    fun importSnapshot(snapshot: HifzSnapshot) {
        clearAll()
        writeNamespace(ownerId, snapshot)
        load()
    }

    /**
     * Returns the current [MasteryStatus] for the given [surahId] and [ayahNo].
     * Returns [MasteryStatus.NEW] if not set.
     */
    fun getStatus(surahId: Int, ayahNo: Int): MasteryStatus {
        _masteryMap.value[surahId to ayahNo]?.let { return it }
        val raw = try {
            settings.getString(key(ayahKey(surahId, ayahNo)), "")
        } catch (_: Exception) {
            ""
        }
        return if (raw.isNotBlank()) {
            try {
                MasteryStatus.valueOf(raw)
            } catch (_: Exception) {
                MasteryStatus.NEW
            }
        } else {
            MasteryStatus.NEW
        }
    }

    /**
     * Cycles the status of the specified ayah through:
     * NEW -> REVIEW_NEEDED -> MASTERED -> NEW
     *
     * Persists the new value to Settings and updates StateFlows.
     * @return the updated [MasteryStatus].
     */
    fun cycleStatus(surahId: Int, ayahNo: Int): MasteryStatus {
        val next = getStatus(surahId, ayahNo).next()
        setStatus(surahId, ayahNo, next)
        return next
    }

    /**
     * Explicitly sets the [MasteryStatus] for the specified ayah.
     * Persists to Settings under `mastery_{surahId}_{ayahNo}`.
     *
     * SM-2-lite scheduling runs on the same path (additive, no signature
     * change): MASTERED doubles the per-ayah interval (default 1) and pushes
     * the due day to `today + max(1, interval * 2)`; REVIEW_NEEDED resets the
     * interval to 1 with the due day tomorrow; NEW drops the SRS rows.
     */
    fun setStatus(surahId: Int, ayahNo: Int, status: MasteryStatus) {
        val storageKey = key(ayahKey(surahId, ayahNo))
        try {
            settings.putString(storageKey, status.name)
        } catch (_: Exception) {}

        _masteryMap.update { current ->
            if (status == MasteryStatus.NEW) {
                current - (surahId to ayahNo)
            } else {
                current + ((surahId to ayahNo) to status)
            }
        }
        updateDerivedFlows()
        updateSrsOnSetStatus(surahId, ayahNo, status)
    }

    /**
     * SM-2-lite half of [setStatus]: maintains the per-ayah `due_` /
     * `interval_` rows and their in-memory mirrors. Kept separate so the
     * status path above stays byte-for-byte what it was.
     */
    private fun updateSrsOnSetStatus(surahId: Int, ayahNo: Int, status: MasteryStatus) {
        val coord = surahId to ayahNo
        when (status) {
            MasteryStatus.MASTERED -> {
                val today = currentEpochDay()
                val nextInterval = maxOf(1, (_intervalMap.value[coord] ?: 1) * 2)
                val nextDue = today + nextInterval
                try {
                    settings.putInt(key(intervalKey(surahId, ayahNo)), nextInterval)
                    settings.putLong(key(dueKey(surahId, ayahNo)), nextDue)
                } catch (_: Exception) {}
                _intervalMap.update { it + (coord to nextInterval) }
                _dueDayMap.update { it + (coord to nextDue) }
            }
            MasteryStatus.REVIEW_NEEDED -> {
                val nextDue = currentEpochDay() + 1L
                try {
                    settings.putInt(key(intervalKey(surahId, ayahNo)), 1)
                    settings.putLong(key(dueKey(surahId, ayahNo)), nextDue)
                } catch (_: Exception) {}
                _intervalMap.update { it + (coord to 1) }
                _dueDayMap.update { it + (coord to nextDue) }
            }
            MasteryStatus.NEW -> {
                try {
                    settings.remove(key(intervalKey(surahId, ayahNo)))
                    settings.remove(key(dueKey(surahId, ayahNo)))
                } catch (_: Exception) {}
                _intervalMap.update { it - coord }
                _dueDayMap.update { it - coord }
            }
        }
    }

    /**
     * Returns the spacing interval in days for the given ayah. Defaults to 1
     * (an ayah never graded has no spacing history).
     */
    fun getInterval(surahId: Int, ayahNo: Int): Int =
        _intervalMap.value[surahId to ayahNo] ?: 1

    /**
     * Returns the epoch day the given ayah is next due for review. Defaults
     * to the first-review day: a tracked ayah with no stored due row is due
     * the day it is first seen (today on a fresh load).
     */
    fun getDueDay(surahId: Int, ayahNo: Int): Long =
        _dueDayMap.value[surahId to ayahNo] ?: currentEpochDay()

    /**
     * Ayahs due for review on [nowEpochDay] (defaults to today): every tracked
     * (non-NEW) ayah whose due day is on or before that day.
     *
     * Sorted overdue-first (ascending due day), ties in surah/ayah order, so
     * the most lapsed ayah heads a "due today" queue.
     */
    fun dueToday(nowEpochDay: Long = currentEpochDay()): List<Pair<Int, Int>> {
        val today = currentEpochDay()
        val dues = _dueDayMap.value
        return _masteryMap.value.keys
            .filter { (dues[it] ?: today) <= nowEpochDay }
            .sortedWith(
                compareBy(
                    { dues[it] ?: today },
                    { it.first },
                    { it.second },
                ),
            )
    }

    /**
     * Returns all ayahs flagged with [MasteryStatus.REVIEW_NEEDED].
     *
     * @param surahId optional filter for a single Surah. If null, returns all weak ayahs across the Quran.
     * @return sorted list of (surahId, ayahNo) pairs.
     */
    fun getWeakAyahs(surahId: Int? = null): List<Pair<Int, Int>> {
        val allWeak = _weakAyahs.value
        return if (surahId == null) {
            allWeak
        } else {
            allWeak.filter { it.first == surahId }
        }
    }

    /**
     * Returns the count of ayahs flagged with [MasteryStatus.MASTERED].
     *
     * @param surahId optional filter for a single Surah. If null, returns total mastered count.
     */
    fun getMasteredCount(surahId: Int? = null): Int {
        return if (surahId == null) {
            _totalMasteredCount.value
        } else {
            _masteryMap.value.entries.count { (coord, st) ->
                coord.first == surahId && st == MasteryStatus.MASTERED
            }
        }
    }

    /**
     * Records that an ayah was reviewed today.
     *
     * - Advances the daily reviewed count.
     * - Maintains or increments the daily streak.
     * - Automatically handles midnight day rollovers.
     */
    fun recordAyahReviewed(surahId: Int, ayahNo: Int) {
        val today = currentEpochDay()
        val lastDay = try {
            settings.getLong(key(KEY_LAST_REVIEW_DAY), 0L)
        } catch (_: Exception) {
            0L
        }

        val currentStreak = _streakDays.value
        val newStreak = when {
            lastDay == 0L -> 1
            today == lastDay -> currentStreak.coerceAtLeast(1)
            today == lastDay + 1L -> currentStreak + 1
            else -> 1 // Gap > 1 day: restart streak
        }

        val baseTodayCount = if (lastDay == today) _todayReviewedCount.value else 0
        val newTodayCount = baseTodayCount + 1

        _todayReviewedCount.value = newTodayCount
        _streakDays.value = newStreak

        try {
            settings.putLong(key(KEY_LAST_REVIEW_DAY), today)
            settings.putInt(key(KEY_STREAK_DAYS), newStreak)
            settings.putInt(key(KEY_TODAY_REVIEWED_COUNT), newTodayCount)
        } catch (_: Exception) {}
    }

    /**
     * Configures the user's daily review goal in number of ayahs.
     */
    fun setDailyGoal(count: Int) {
        val clamped = count.coerceAtLeast(1)
        _dailyGoalCount.value = clamped
        try {
            settings.putInt(key(KEY_DAILY_GOAL), clamped)
        } catch (_: Exception) {}
    }

    /**
     * Checks if today's review goal has been reached or exceeded.
     */
    fun isDailyGoalReached(): Boolean = _todayReviewedCount.value >= _dailyGoalCount.value

    /**
     * Returns memorization progress ratio (0.0f..1.0f) for a surah.
     */
    fun getSurahMasteredPercentage(surahId: Int): Float {
        val surah = QuranData.SURAHS.find { it.id == surahId } ?: return 0f
        if (surah.ayahsCount <= 0) return 0f
        return (getMasteredCount(surahId).toFloat() / surah.ayahsCount.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Clears all local mastery data, resetting goals, streaks, and statuses.
     * Only the currently bound owner's namespace is touched.
     */
    fun clearAll() {
        clearNamespace(ownerId)

        _masteryMap.value = emptyMap()
        _totalMasteredCount.value = 0
        _weakAyahs.value = emptyList()
        _dueDayMap.value = emptyMap()
        _intervalMap.value = emptyMap()
        _todayReviewedCount.value = 0
        _streakDays.value = 0
        _dailyGoalCount.value = DEFAULT_HIFZ_DAILY_GOAL
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun currentEpochDay(): Long = currentTimeMs() / 86_400_000L

    private fun load() {
        val prefix = masteryPrefix(ownerId)
        val loadedMap = mutableMapOf<Pair<Int, Int>, MasteryStatus>()

        try {
            for (k in settings.keys) {
                if (k.startsWith(prefix)) {
                    val suffix = k.removePrefix(prefix)
                    val parts = suffix.split("_")
                    if (parts.size == 2) {
                        val sId = parts[0].toIntOrNull()
                        val aNo = parts[1].toIntOrNull()
                        if (sId != null && aNo != null) {
                            val statusStr = settings.getString(k, "")
                            val status = try {
                                MasteryStatus.valueOf(statusStr)
                            } catch (_: Exception) {
                                MasteryStatus.NEW
                            }
                            if (status != MasteryStatus.NEW) {
                                loadedMap[sId to aNo] = status
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        _masteryMap.value = loadedMap
        updateDerivedFlows()
        loadSrsState(loadedMap.keys)

        val savedGoal = try {
            settings.getInt(key(KEY_DAILY_GOAL), DEFAULT_HIFZ_DAILY_GOAL)
        } catch (_: Exception) {
            DEFAULT_HIFZ_DAILY_GOAL
        }
        _dailyGoalCount.value = if (savedGoal > 0) savedGoal else DEFAULT_HIFZ_DAILY_GOAL

        val today = currentEpochDay()
        val lastDay = try {
            settings.getLong(key(KEY_LAST_REVIEW_DAY), 0L)
        } catch (_: Exception) {
            0L
        }

        val savedStreak = try {
            settings.getInt(key(KEY_STREAK_DAYS), 0)
        } catch (_: Exception) {
            0
        }

        val savedTodayCount = try {
            settings.getInt(key(KEY_TODAY_REVIEWED_COUNT), 0)
        } catch (_: Exception) {
            0
        }

        when {
            lastDay == 0L -> {
                _streakDays.value = 0
                _todayReviewedCount.value = 0
            }
            lastDay == today -> {
                _streakDays.value = savedStreak
                _todayReviewedCount.value = savedTodayCount
            }
            today == lastDay + 1L -> {
                _streakDays.value = savedStreak
                _todayReviewedCount.value = 0
                try {
                    settings.putInt(key(KEY_TODAY_REVIEWED_COUNT), 0)
                } catch (_: Exception) {}
            }
            else -> {
                // Streak broken (> 1 day gap)
                _streakDays.value = 0
                _todayReviewedCount.value = 0
                try {
                    settings.putInt(key(KEY_STREAK_DAYS), 0)
                    settings.putInt(key(KEY_TODAY_REVIEWED_COUNT), 0)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Restores the SM-2-lite mirrors for [tracked] coords. Rows graded before
     * this feature existed have a status but no `due_` / `interval_` keys;
     * those backfill to the first-review-day default (due = today, interval =
     * 1) and persist it, so a legacy profile is due-today honest on first
     * load and roundtrips afterwards.
     */
    private fun loadSrsState(tracked: Set<Pair<Int, Int>>) {
        val srsToday = currentEpochDay()
        val dues = mutableMapOf<Pair<Int, Int>, Long>()
        val intervals = mutableMapOf<Pair<Int, Int>, Int>()
        for (coord in tracked) {
            val (surahId, ayahNo) = coord
            val storedDue = try {
                settings.getLongOrNull(key(dueKey(surahId, ayahNo)))
            } catch (_: Exception) {
                null
            }
            val storedInterval = try {
                settings.getIntOrNull(key(intervalKey(surahId, ayahNo)))
            } catch (_: Exception) {
                null
            }
            if (storedDue != null) {
                dues[coord] = storedDue
            } else {
                dues[coord] = srsToday
                try {
                    settings.putLong(key(dueKey(surahId, ayahNo)), srsToday)
                } catch (_: Exception) {}
            }
            if (storedInterval != null && storedInterval > 0) {
                intervals[coord] = storedInterval
            } else {
                intervals[coord] = 1
                try {
                    settings.putInt(key(intervalKey(surahId, ayahNo)), 1)
                } catch (_: Exception) {}
            }
        }
        _dueDayMap.value = dues
        _intervalMap.value = intervals
    }

    private fun updateDerivedFlows() {
        val entries = _masteryMap.value.entries
        _totalMasteredCount.value = entries.count { it.value == MasteryStatus.MASTERED }
        _weakAyahs.value = entries
            .asSequence()
            .filter { it.value == MasteryStatus.REVIEW_NEEDED }
            .map { it.key }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .toList()
    }
}
