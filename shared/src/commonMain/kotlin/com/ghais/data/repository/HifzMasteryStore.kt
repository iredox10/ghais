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
)

/**
 * HifzMasteryStore:
 *
 * Persisted store for Quranic memorization progress (Hifz), weak ayah tracking,
 * daily review goals, and review streaks.
 *
 * Stored via multiplatform [Settings] with keys:
 * - Ayah status: `mastery_{surahId}_{ayahNo}` (one key per ayah)
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

    private val settings: Settings by lazy { Settings() }

    // In-memory cache of non-NEW mastery statuses: Pair(surahId, ayahNo) -> MasteryStatus
    private val _masteryMap = MutableStateFlow<Map<Pair<Int, Int>, MasteryStatus>>(emptyMap())
    val masteryMap: StateFlow<Map<Pair<Int, Int>, MasteryStatus>> = _masteryMap.asStateFlow()

    // Total mastered ayahs across the Quran
    private val _totalMasteredCount = MutableStateFlow(0)
    val totalMasteredCount: StateFlow<Int> = _totalMasteredCount.asStateFlow()

    // Cached sorted list of weak ayahs (REVIEW_NEEDED)
    private val _weakAyahs = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val weakAyahs: StateFlow<List<Pair<Int, Int>>> = _weakAyahs.asStateFlow()

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
        val goal = settings.getIntOrNull(scopedKey(owner, KEY_DAILY_GOAL))
        val streak = settings.getIntOrNull(scopedKey(owner, KEY_STREAK_DAYS))
        val reviewed = settings.getIntOrNull(scopedKey(owner, KEY_TODAY_REVIEWED_COUNT))
        val lastDay = settings.getLongOrNull(scopedKey(owner, KEY_LAST_REVIEW_DAY))
        if (statuses.isEmpty() && goal == null && streak == null && reviewed == null && lastDay == null) {
            null
        } else {
            HifzSnapshot(
                statuses = statuses,
                dailyGoalCount = if (goal != null && goal > 0) goal else DEFAULT_HIFZ_DAILY_GOAL,
                streakDays = streak ?: 0,
                todayReviewedCount = reviewed ?: 0,
                lastReviewDay = lastDay ?: 0L,
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
