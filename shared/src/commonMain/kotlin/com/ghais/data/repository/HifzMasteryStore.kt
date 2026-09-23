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

/**
 * HifzMasteryStore:
 *
 * Persisted store for Quranic memorization progress (Hifz), weak ayah tracking,
 * daily review goals, and review streaks.
 *
 * Stored via multiplatform [Settings] with keys:
 * - Ayah status: `mastery_{surahId}_{ayahNo}`
 * - Goal & Streak: `hifz_daily_goal_count`, `hifz_today_reviewed_count`,
 *                  `hifz_last_review_day`, `hifz_streak_days`.
 */
object HifzMasteryStore {

    private const val KEY_DAILY_GOAL = "hifz_daily_goal_count"
    private const val KEY_TODAY_REVIEWED_COUNT = "hifz_today_reviewed_count"
    private const val KEY_LAST_REVIEW_DAY = "hifz_last_review_day"
    private const val KEY_STREAK_DAYS = "hifz_streak_days"

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private fun ayahKey(surahId: Int, ayahNo: Int): String =
        "mastery_${surahId}_${ayahNo}"

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
    private val _dailyGoalCount = MutableStateFlow(5)
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
     * Sets the account owner namespace for multi-user isolation and sync integration.
     */
    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { "local" }
        if (normalized == this.ownerId) return
        this.ownerId = normalized
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
     */
    fun clearAll() {
        val prefix = if (ownerId == "local") "mastery_" else "$ownerId::mastery_"
        try {
            val keysToRemove = settings.keys.filter { it.startsWith(prefix) }
            for (k in keysToRemove) {
                settings.remove(k)
            }
            settings.remove(key(KEY_DAILY_GOAL))
            settings.remove(key(KEY_TODAY_REVIEWED_COUNT))
            settings.remove(key(KEY_LAST_REVIEW_DAY))
            settings.remove(key(KEY_STREAK_DAYS))
        } catch (_: Exception) {}

        _masteryMap.value = emptyMap()
        _totalMasteredCount.value = 0
        _weakAyahs.value = emptyList()
        _todayReviewedCount.value = 0
        _streakDays.value = 0
        _dailyGoalCount.value = 5
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun currentEpochDay(): Long = currentTimeMs() / 86_400_000L

    private fun load() {
        val prefix = if (ownerId == "local") "mastery_" else "$ownerId::mastery_"
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
            settings.getInt(key(KEY_DAILY_GOAL), 5)
        } catch (_: Exception) {
            5
        }
        _dailyGoalCount.value = if (savedGoal > 0) savedGoal else 5

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
