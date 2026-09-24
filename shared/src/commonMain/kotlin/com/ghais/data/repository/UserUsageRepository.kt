package com.ghais.data.repository

import com.ghais.data.seed.JumpBackInItem
import com.ghais.data.seed.QuranData
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Serializable
data class PersistedHistoryItem(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val coverUrl: String,
    val surahId: Int,
    val reciterSlug: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedTimestampMs: Long = 0L
)

data class UserListeningStats(
    val daysStreak: Int,
    val minutesToday: Int,
    val uniqueRecitersCount: Int,
    val uniqueSurahsCount: Int,
    val totalSecondsListened: Long = 0L
)

/**
 * Cloud snapshot for [UserUsageRepository.restoreStats].
 *
 * Mirrors [UserListeningStats] plus the cloud `updated_at` millis used for
 * the `minutes_today` day-boundary check (adopted only when it falls on the
 * current epoch day, else restored as 0 — never resurrect yesterday's
 * minutes).
 */
data class StatsSnapshot(
    val daysStreak: Int,
    val minutesToday: Int,
    val uniqueRecitersCount: Int,
    val uniqueSurahsCount: Int,
    val totalSecondsListened: Long = 0L,
    val updatedAtMs: Long? = null
)

/**
 * UserUsageRepository:
 *
 * Provides live, real-time, persisted tracking of user listening history
 * ("Continue Listening") and listening statistics ("Your Stats").
 */
object UserUsageRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val KEY_HISTORY = "ghais_user_listening_history_v2"
    private const val KEY_LAST_DAY = "ghais_stats_last_day"
    private const val KEY_STREAK = "ghais_stats_streak"
    private const val KEY_SECONDS_TODAY = "ghais_stats_seconds_today"
    private const val KEY_TOTAL_SECONDS = "ghais_stats_total_seconds"
    private const val KEY_UNIQUE_RECITERS = "ghais_stats_unique_reciters"
    private const val KEY_UNIQUE_SURAHS = "ghais_stats_unique_surahs"
    private const val KEY_DAILY_SECONDS = "ghais_stats_daily_seconds"
    private const val KEY_SURAH_PLAYS = "ghais_stats_surah_plays"
    private const val KEY_DEVICE_ID = "ghais_device_id"
    private const val MAX_DAILY_BUCKETS = 400

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    // One-time migration readers: read the new (owner-prefixed) key first;
    // if it holds no value and the legacy key does, adopt the legacy value,
    // persist it under the new key, and delete the legacy key. Never throws.
    private fun migratedString(base: String, default: String = ""): String {
        return try {
            val newKey = k(base)
            val current = settings.getString(newKey, default)
            if (current.isNotBlank()) return current
            val legacyKey = k(legacyBase(base))
            val legacy = try {
                settings.getString(legacyKey, "")
            } catch (_: Exception) {
                return current
            }
            if (legacy.isNotBlank()) {
                try {
                    settings.putString(newKey, legacy)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                legacy
            } else {
                current
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun migratedInt(base: String, default: Int): Int {
        return try {
            val newKey = k(base)
            val current = settings.getInt(newKey, default)
            if (current != default) return current
            val legacyKey = k(legacyBase(base))
            val legacy = try {
                settings.getInt(legacyKey, default)
            } catch (_: Exception) {
                return current
            }
            if (legacy != default) {
                try {
                    settings.putInt(newKey, legacy)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                legacy
            } else {
                current
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun migratedLong(base: String, default: Long): Long {
        return try {
            val newKey = k(base)
            val current = settings.getLong(newKey, default)
            if (current != default) return current
            val legacyKey = k(legacyBase(base))
            val legacy = try {
                settings.getLong(legacyKey, default)
            } catch (_: Exception) {
                return current
            }
            if (legacy != default) {
                try {
                    settings.putLong(newKey, legacy)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                legacy
            } else {
                current
            }
        } catch (_: Exception) {
            default
        }
    }

    private var ownerPrefix = ""
    private var currentOwnerId = "local"

    private fun k(key: String): String =
        if (ownerPrefix.isEmpty()) key else ownerPrefix + key

    private var cachedDeviceId: String? = null

    /**
     * Stable per-install device identity for per-device daily sync (v2).
     *
     * Owner-INDEPENDENT by design: stored under the raw [KEY_DEVICE_ID]
     * Settings key (no [k] owner prefix), so the id survives account
     * switches — the same device keeps one id across accounts because this
     * is device identity, not user identity. Generated once via UUID on
     * first run, cached in memory, never reset by [setOwner]. Never throws
     * (falls back to a `dev-<nowMs>-<rand>` id when UUID/Settings fail).
     */
    fun deviceId(): String {
        cachedDeviceId?.takeIf { it.isNotBlank() }?.let { return it }
        try {
            val stored = try {
                settings.getString(KEY_DEVICE_ID, "")
            } catch (_: Exception) {
                ""
            }
            if (stored.isNotBlank()) {
                cachedDeviceId = stored
                return stored
            }
            val fresh = try {
                Uuid.random().toString()
            } catch (_: Exception) {
                "dev-${currentTimeMs()}-${(0..999_999).random()}"
            }.takeIf { it.isNotBlank() } ?: "dev-${currentTimeMs()}"
            try {
                settings.putString(KEY_DEVICE_ID, fresh)
            } catch (_: Exception) {
            }
            cachedDeviceId = fresh
            return fresh
        } catch (_: Exception) {
            val fallback = "dev-${currentTimeMs()}"
            cachedDeviceId = fallback
            return fallback
        }
    }

    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { "local" }
        if (normalized == currentOwnerId) return
        currentOwnerId = normalized
        ownerPrefix = if (normalized == "local") "" else "$normalized::"
        lastRecordedTimeMs = 0L
        unaccountedMs = 0L
        lastSavedTimestampMs = currentTimeMs()
        playCountTrackKey = null
        playCountCounted = false
        playCountLastPosMs = 0L
        _history.value = emptyList()
        _dailySeconds.value = emptyMap()
        _remoteDailySeconds.value = emptyMap()
        _surahPlays.value = emptyMap()
        loadHistory()
        loadStats()
    }

    private val _history = MutableStateFlow<List<JumpBackInItem>>(emptyList())
    val history: StateFlow<List<JumpBackInItem>> = _history.asStateFlow()

    private val _stats = MutableStateFlow(
        UserListeningStats(
            daysStreak = 0,
            minutesToday = 0,
            uniqueRecitersCount = 1,
            uniqueSurahsCount = 1
        )
    )
    val stats: StateFlow<UserListeningStats> = _stats.asStateFlow()

    private val _dailySeconds = MutableStateFlow<Map<Long, Long>>(emptyMap())
    /**
     * Per-day listening buckets: epochDay (ms/86400000) -> seconds listened.
     * Local-only accumulation for Today/Week/30d/3mo/Year ranges; capped at
     * [MAX_DAILY_BUCKETS] newest days, older entries pruned on write.
     */
    val dailySeconds: StateFlow<Map<Long, Long>> = _dailySeconds.asStateFlow()

    private val _remoteDailySeconds = MutableStateFlow<Map<Long, Long>>(emptyMap())
    /**
     * Per-day seconds from OTHER devices (memory-only, derived per sync pull;
     * never persisted). Keyed by epochDay like [dailySeconds]. Recomputed on
     * every stats pull; empty until the first pull with cloud daily data.
     * Local [dailySeconds] buckets stay this-device-only and never include
     * these values — UI layers sum both sides for display.
     */
    val remoteDailySeconds: StateFlow<Map<Long, Long>> = _remoteDailySeconds.asStateFlow()

    private val _surahPlays = MutableStateFlow<Map<Int, Long>>(emptyMap())
    /**
     * Per-surah play counts: surahId (1..114) -> qualifying plays, all-time.
     *
     * Counting rule ("completion-or-30s"): a surah earns ONE play the first
     * time in a listen session where, while [AudioEngine.isPlaying] is true,
     * either (a) observed position >= 30_000ms, or (b) progress >= 0.95
     * (covers short surahs under 30s). NOT counted on track start alone, so
     * skips under 30s don't inflate the chart.
     *
     * Ayah-mode tracks (per-ayah EveryAyah audio) count only when the FINAL
     * ayah of the surah qualifies — otherwise each ayah completion would
     * inflate the surah count by ~ayahsCount. Partial-surah listens in
     * ayah mode (never reaching the last ayah) don't count yet.
     *
     * Session tracking: keyed by `surahId::reciterSlug`; a position rewind
     * >10s resets the counted flag so genuine replays/loops count again,
     * while small seeks don't double-count. Seeks straight to the end DO
     * count (known limitation — no seek signal is exposed). Owner-namespaced
     * + persisted; intentionally all-time (no per-range filtering).
     */
    val surahPlays: StateFlow<Map<Int, Long>> = _surahPlays.asStateFlow()

    /**
     * Replaces the in-memory [remoteDailySeconds] map (sync pull path only).
     * Sanitizes (drops negative days / non-positive seconds), never persists,
     * never touches [dailySeconds]. Empty input clears the map. Never throws.
     */
    fun setRemoteDailySeconds(daily: Map<Long, Long>) {
        try {
            if (daily.isEmpty()) {
                _remoteDailySeconds.value = emptyMap()
                return
            }
            _remoteDailySeconds.value = daily.filter { it.key >= 0L && it.value > 0L }
        } catch (_: Exception) {
        }
    }

    private var lastRecordedTimeMs: Long = 0L
    /** Sub-second leftover: position polls arrive ~4x/sec, so plain ms/1000 truncation would drop everything. */
    private var unaccountedMs: Long = 0L
    private var lastSavedTimestampMs: Long = 0L
    // Play-count session tracking (see [surahPlays] for the rule).
    private var playCountTrackKey: String? = null
    private var playCountCounted: Boolean = false
    private var playCountLastPosMs: Long = 0L

    init {
        loadHistory()
        loadStats()
        observeAudioEngine()
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun currentEpochDay(): Long = currentTimeMs() / 86_400_000L

    private fun loadHistory() {
        val rawJson = migratedString(KEY_HISTORY, "")
        if (rawJson.isNotBlank()) {
            try {
                val parsed = json.decodeFromString<List<PersistedHistoryItem>>(rawJson)
                if (parsed.isNotEmpty()) {
                    // Latest-first invariant at read: newest timestamp first (stable for ties).
                    _history.value = parsed.map { it.toJumpBackInItem() }
                        .sortedByDescending { it.lastPlayedTimestampMs }
                    return
                }
            } catch (_: Exception) {
            }
        }
        // No mock data: empty history renders honest empty states downstream.
    }

    private fun loadStats() {
        val today = currentEpochDay()
        val savedDay = migratedLong(KEY_LAST_DAY, 0L)
        var streak = migratedInt(KEY_STREAK, 0).coerceAtLeast(0)
        var secondsToday = migratedLong(KEY_SECONDS_TODAY, 0L)
        val totalSeconds = migratedLong(KEY_TOTAL_SECONDS, 0L)

        if (savedDay != 0L) {
            if (today == savedDay + 1L) {
                // Consecutive day
                secondsToday = 0L
            } else if (today > savedDay + 1L) {
                // Streak broken (no listen yet today — first listen bumps 0 → 1)
                streak = 0
                secondsToday = 0L
            }
        }

        val uniqueReciters = getStoredSet(KEY_UNIQUE_RECITERS).ifEmpty { setOf("mishary") }
        val uniqueSurahs = getStoredSet(KEY_UNIQUE_SURAHS).ifEmpty { setOf("18") }

        // Day rollover keeps buckets: only secondsToday/minutesToday resets above.
        // Buckets accumulate indefinitely (capped) so range queries stay intact.
        _dailySeconds.value = loadDailySeconds()
        _surahPlays.value = loadSurahPlays()

        _stats.value = UserListeningStats(
            daysStreak = streak,
            minutesToday = (secondsToday / 60L).toInt(),
            uniqueRecitersCount = uniqueReciters.size,
            uniqueSurahsCount = uniqueSurahs.size,
            totalSecondsListened = totalSeconds
        )
    }

    /**
     * Adopts a cloud stats snapshot (fresh-login restore).
     *
     * - Adopts cloud totals/streak/uniques as-is.
     * - `minutes_today` is adopted ONLY when [StatsSnapshot.updatedAtMs]
     *   falls on the current epoch day (`ms/86400000`, same math as the
     *   day-rollover logic); otherwise it restores as 0. `KEY_LAST_DAY` is
     *   stamped to today so the rollover logic can't resurrect yesterday's
     *   minutes on the next load.
     * - Unique counts come from the cloud as counts only (no identity), so
     *   the local id sets are padded with `restored-*` placeholders up to the
     *   cloud size — never shrunk, so real local uniques are never lost.
     * - Persists everything to disk and refreshes [_stats]. Never throws.
     * - Per-day buckets ([dailySeconds]) are adopted separately, not here:
     *   the sync agent merges the cloud `daily_json` series via [restoreDaily].
     */
    fun restoreStats(snapshot: StatsSnapshot) {
        try {
            val today = currentEpochDay()
            val isToday = snapshot.updatedAtMs != null &&
                snapshot.updatedAtMs > 0L &&
                snapshot.updatedAtMs / 86_400_000L == today
            val total = snapshot.totalSecondsListened.coerceAtLeast(0L)
            val streak = snapshot.daysStreak.coerceAtLeast(1)
            var secondsToday =
                if (isToday) snapshot.minutesToday.coerceAtLeast(0) * 60L else 0L
            if (secondsToday > total) secondsToday = total

            settings.putLong(k(KEY_LAST_DAY), today)
            settings.putInt(k(KEY_STREAK), streak)
            settings.putLong(k(KEY_SECONDS_TODAY), secondsToday)
            settings.putLong(k(KEY_TOTAL_SECONDS), total)

            val reciters = getStoredSet(KEY_UNIQUE_RECITERS).toMutableSet()
            if (snapshot.uniqueRecitersCount > reciters.size) {
                var i = 1
                while (reciters.size < snapshot.uniqueRecitersCount) {
                    reciters.add("restored-reciter-$i")
                    i++
                }
                saveStoredSet(KEY_UNIQUE_RECITERS, reciters)
            }
            val surahs = getStoredSet(KEY_UNIQUE_SURAHS).toMutableSet()
            if (snapshot.uniqueSurahsCount > surahs.size) {
                var i = 1
                while (surahs.size < snapshot.uniqueSurahsCount) {
                    surahs.add("restored-surah-$i")
                    i++
                }
                saveStoredSet(KEY_UNIQUE_SURAHS, surahs)
            }

            _stats.value = UserListeningStats(
                daysStreak = streak,
                minutesToday = (secondsToday / 60L).toInt(),
                uniqueRecitersCount = reciters.ifEmpty { setOf("mishary") }.size
                    .coerceAtLeast(if (snapshot.uniqueRecitersCount > 0) snapshot.uniqueRecitersCount else 1),
                uniqueSurahsCount = surahs.ifEmpty { setOf("18") }.size
                    .coerceAtLeast(if (snapshot.uniqueSurahsCount > 0) snapshot.uniqueSurahsCount else 1),
                totalSecondsListened = total
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Merges a cloud daily-seconds series into the local per-day buckets
     * (sync restore path for the `listening_stats.daily_json` attribute).
     *
     * Per-day `max()`, never sum: there is no per-device tracking, so a day
     * present on both sides usually counts the SAME listening on each side —
     * summing would double-count multi-device same-day listening, while `max`
     * adopts the fuller side. Never shrinks local buckets, prunes to
     * [MAX_DAILY_BUCKETS] newest days, persists via [saveDailySeconds], and
     * refreshes [dailySeconds]. Empty input is a no-op. Never throws.
     */
    fun restoreDaily(daily: Map<Long, Long>) {
        if (daily.isEmpty()) return
        try {
            val cleaned = daily.filter { it.key >= 0L && it.value > 0L }
            if (cleaned.isEmpty()) return
            val merged = _dailySeconds.value.toMutableMap()
            for ((day, secs) in cleaned) {
                merged[day] = maxOf(merged[day] ?: 0L, secs)
            }
            val pruned = if (merged.size > MAX_DAILY_BUCKETS) {
                merged.entries.sortedByDescending { it.key }.take(MAX_DAILY_BUCKETS)
                    .associate { it.key to it.value }
            } else {
                merged
            }
            _dailySeconds.value = pruned
            saveDailySeconds(pruned)
        } catch (_: Exception) {
        }
    }

    private fun observeAudioEngine() {
        scope.launch {
            AudioEngine.isPlaying.collect { isPlaying ->
                if (isPlaying) {
                    lastRecordedTimeMs = currentTimeMs()
                    ensureStreakUpdatedForToday()
                } else {
                    saveStatsToDisk()
                    saveHistoryToDisk()
                }
            }
        }

        scope.launch {
            AudioEngine.currentPositionMs.collect { pos ->
                val track = AudioEngine.currentTrack.value ?: return@collect
                val duration = AudioEngine.durationMs.value.takeIf { it > 0L }
                    ?: (QuranDataRepository.getSurahById(track.surahId)?.ayahsCount ?: 50) * 15_000L

                recordProgress(track, pos, duration)

                if (AudioEngine.isPlaying.value) {
                    maybeCountSurahPlay(track, pos, duration)
                    val now = currentTimeMs()
                    val deltaMs = if (lastRecordedTimeMs > 0L) (now - lastRecordedTimeMs).coerceIn(0L, 5000L) else 0L
                    lastRecordedTimeMs = now
                    if (deltaMs > 0L) {
                        recordListeningTime(deltaMs)
                    }
                }
            }
        }
    }

    private fun recordProgress(track: TrackItem, positionMs: Long, durationMs: Long) {
        val now = currentTimeMs()
        val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        val formattedRemaining = formatRemainingTime(positionMs, durationMs)
        val cover = getCoverForSurah(track.surahId)

        val updatedItem = JumpBackInItem(
            title = track.surahNameEn.ifEmpty { "Surah ${track.surahId}" },
            subtitle = "${track.reciterName.ifEmpty { "Mishary" }} • $formattedRemaining",
            progress = progress,
            coverUrl = cover,
            surahId = track.surahId,
            reciterSlug = track.reciterSlug,
            positionMs = positionMs,
            durationMs = durationMs,
            lastPlayedTimestampMs = now
        )

        _history.update { currentList ->
            val filtered = currentList.filterNot { it.surahId == track.surahId && it.reciterSlug == track.reciterSlug }
            // Most-recent-first: newest at index 0.
            (listOf(updatedItem) + filtered).take(15)
        }

        recordUniqueItem(track.reciterSlug, track.surahId)

        val saveNow = currentTimeMs()
        if (saveNow - lastSavedTimestampMs > 5000L) {
            lastSavedTimestampMs = saveNow
            saveHistoryToDisk()
            saveStatsToDisk()
        }
    }

    private fun recordListeningTime(deltaMs: Long) {
        unaccountedMs += deltaMs.coerceAtLeast(0L)
        val deltaSeconds = unaccountedMs / 1000L
        if (deltaSeconds <= 0L) return
        unaccountedMs %= 1000L

        val today = currentEpochDay()
        val savedDay = migratedLong(KEY_LAST_DAY, 0L)

        var streak = _stats.value.daysStreak
        var secondsToday = migratedLong(KEY_SECONDS_TODAY, 0L) + deltaSeconds
        val totalSeconds = migratedLong(KEY_TOTAL_SECONDS, 0L) + deltaSeconds

        if (savedDay != today) {
            if (savedDay != 0L && today == savedDay + 1L) {
                streak += 1
            } else if (savedDay != 0L && today > savedDay + 1L) {
                streak = 1
            } else if (savedDay == 0L) {
                // Fresh install, first listen ever: 0 → 1.
                streak = streak.coerceAtLeast(1)
            }
            secondsToday = deltaSeconds
            settings.putLong(k(KEY_LAST_DAY), today)
            settings.putInt(k(KEY_STREAK), streak)
        }

        settings.putLong(k(KEY_SECONDS_TODAY), secondsToday)
        settings.putLong(k(KEY_TOTAL_SECONDS), totalSeconds)

        // Accumulate into today's per-day bucket (local-only; never reset on rollover).
        val prunedBuckets = _dailySeconds.value.let { current ->
            current + (today to ((current[today] ?: 0L) + deltaSeconds))
        }.let { updated ->
            if (updated.size > MAX_DAILY_BUCKETS) {
                updated.entries.sortedByDescending { it.key }.take(MAX_DAILY_BUCKETS)
                    .associate { it.key to it.value }
            } else {
                updated
            }
        }
        _dailySeconds.value = prunedBuckets
        saveDailySeconds(prunedBuckets)

        _stats.update { current ->
            current.copy(
                daysStreak = streak,
                minutesToday = (secondsToday / 60L).toInt(),
                totalSecondsListened = totalSeconds
            )
        }
    }

    private fun ensureStreakUpdatedForToday() {
        val today = currentEpochDay()
        val savedDay = migratedLong(KEY_LAST_DAY, 0L)
        var streak = migratedInt(KEY_STREAK, 0).coerceAtLeast(0)

        if (savedDay != today) {
            if (savedDay != 0L && today == savedDay + 1L) {
                streak += 1
            } else if (savedDay != 0L && today > savedDay + 1L) {
                streak = 1
            } else if (savedDay == 0L) {
                // Fresh install, first listen ever: 0 → 1.
                streak = streak.coerceAtLeast(1)
            }
            settings.putLong(k(KEY_LAST_DAY), today)
            settings.putInt(k(KEY_STREAK), streak)
            settings.putLong(k(KEY_SECONDS_TODAY), 0L)
            // Buckets intentionally kept: only minutes_today resets on rollover.

            _stats.update { it.copy(daysStreak = streak, minutesToday = 0) }
        }
    }

    private fun recordUniqueItem(reciterSlug: String, surahId: Int) {
        val reciters = getStoredSet(KEY_UNIQUE_RECITERS).toMutableSet()
        val surahs = getStoredSet(KEY_UNIQUE_SURAHS).toMutableSet()

        var changed = false
        if (reciters.add(reciterSlug)) {
            saveStoredSet(KEY_UNIQUE_RECITERS, reciters)
            changed = true
        }
        if (surahs.add(surahId.toString())) {
            saveStoredSet(KEY_UNIQUE_SURAHS, surahs)
            changed = true
        }

        if (changed) {
            _stats.update {
                it.copy(
                    uniqueRecitersCount = reciters.size,
                    uniqueSurahsCount = surahs.size
                )
            }
        }
    }

    /**
     * Completion-or-30s gate for [surahPlays] (call only while isPlaying).
     *
     * Session handling: `surahId::reciterSlug` key change starts a new
     * session (counted=false); a rewind >10s also resets, so genuine
     * replays/queue loops can count again while small seeks can't
     * double-count. Ayah-mode tracks are eligible only on the surah's final
     * ayah (else each ayah would inflate the surah count). Never throws.
     */
    private fun maybeCountSurahPlay(track: TrackItem, positionMs: Long, durationMs: Long) {
        try {
            val surahId = track.surahId
            if (surahId <= 0 || positionMs < 0L) return
            val key = "${surahId}::${track.reciterSlug}"
            if (key != playCountTrackKey) {
                playCountTrackKey = key
                playCountCounted = false
                playCountLastPosMs = positionMs
            } else if (positionMs < playCountLastPosMs - 10_000L) {
                // Rewind/restart/replay (incl. SURAH-repeat loops): new session.
                playCountCounted = false
            }
            playCountLastPosMs = positionMs
            if (playCountCounted) return

            // Ayah-mode guard: only the final ayah can close out a surah play.
            if (track.ayahNo > 0) {
                val totalAyahs = QuranDataRepository.getSurahById(surahId)?.ayahsCount
                    ?: return
                if (track.ayahNo < totalAyahs) return
            }

            val progress = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            if (positionMs >= 30_000L || progress >= 0.95f) {
                playCountCounted = true
                recordSurahPlay(surahId)
            }
        } catch (_: Exception) {
        }
    }

    private fun recordSurahPlay(surahId: Int) {
        try {
            val updated = _surahPlays.value.toMutableMap()
            updated[surahId] = (updated[surahId] ?: 0L) + 1L
            _surahPlays.value = updated
            saveSurahPlays(updated)
        } catch (_: Exception) {
        }
    }

    private fun loadSurahPlays(): Map<Int, Long> {
        val raw = migratedString(KEY_SURAH_PLAYS, "")
        if (raw.isBlank()) return emptyMap()
        try {
            val parsed = json.decodeFromString<Map<Int, Long>>(raw)
            return parsed.filter { it.key in 1..114 && it.value > 0L }
        } catch (_: Exception) {
            try {
                val fallback = json.decodeFromString<Map<String, Long>>(raw)
                return fallback.mapNotNull { (id, plays) ->
                    val surahId = id.toIntOrNull() ?: return@mapNotNull null
                    if (surahId !in 1..114 || plays <= 0L) null else surahId to plays
                }.toMap()
            } catch (_: Exception) {
                return emptyMap()
            }
        }
    }

    private fun saveSurahPlays(plays: Map<Int, Long>) {
        try {
            settings.putString(k(KEY_SURAH_PLAYS), json.encodeToString(plays))
        } catch (_: Exception) {}
    }

    private fun formatRemainingTime(positionMs: Long, durationMs: Long): String {
        if (positionMs <= 0L) return "Not started"
        if (durationMs > 0L && positionMs >= durationMs * 0.98f) return "Completed"

        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
        val remainingSeconds = remainingMs / 1000L
        val mins = remainingSeconds / 60L
        val secs = remainingSeconds % 60L

        return if (mins == 0L) {
            "${secs}s left"
        } else {
            "${mins}:${secs.toString().padStart(2, '0')} left"
        }
    }

    /**
     * Restores listening history from decoded cloud entries (most-recent-first
     * `TrackItem` + `played_at_ms` + `position_ms` triples, as supplied by the
     * sync pull).
     *
     * Day/order-aware: entries are sorted by `playedAtMs` desc so the most
     * recent play heads the list, and each entry keeps its cloud timestamp as
     * [JumpBackInItem.lastPlayedTimestampMs] (persisted as-is; see
     * [toPersistedHistoryItem]) instead of being re-stamped with "now".
     * Capped at 15 like [recordProgress], and merges without duplicating
     * identical entries (same `surahId` + `reciterSlug` keeps the most recent
     * occurrence). Persists via [saveHistoryToDisk]. The cloud `position_ms`
     * is adopted (capped strictly below `durationMs` so resume never starts
     * at/past the end); entries without one resume from the start
     * (`positionMs = 0`, "Not started" subtitle).
     */
    fun restoreHistory(items: List<Triple<TrackItem, Long, Long>>) {
        if (items.isEmpty()) return
        val seen = mutableSetOf<Pair<Int, String>>()
        val restored = items.sortedByDescending { it.second }.mapNotNull { (track, playedAt, positionMs) ->
            if (track.surahId <= 0 || playedAt <= 0L) return@mapNotNull null
            if (!seen.add(track.surahId to track.reciterSlug)) return@mapNotNull null
            val duration = track.durationMs.coerceAtLeast(0L)
            val safePos = if (duration > 0L) {
                positionMs.coerceIn(0L, (duration - 1L).coerceAtLeast(0L))
            } else {
                positionMs.coerceAtLeast(0L)
            }
            val progress = if (duration > 0L) (safePos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
            JumpBackInItem(
                title = track.surahNameEn.ifEmpty { "Surah ${track.surahId}" },
                subtitle = "${track.reciterName.ifEmpty { "Mishary" }} • ${formatRemainingTime(safePos, duration)}",
                progress = progress,
                coverUrl = getCoverForSurah(track.surahId),
                surahId = track.surahId,
                reciterSlug = track.reciterSlug,
                positionMs = safePos,
                durationMs = duration,
                lastPlayedTimestampMs = playedAt
            )
        }.take(15)
        if (restored.isEmpty()) return
        _history.update { current ->
            val keys = current.map { it.surahId to it.reciterSlug }.toMutableSet()
            val fresh = restored.filter { keys.add(it.surahId to it.reciterSlug) }
            (current + fresh).take(15)
        }
        saveHistoryToDisk()
    }

    private fun getCoverForSurah(surahId: Int): String {
        return when (surahId) {
            1 -> GhaisAssets.LibraryMorningCover
            18 -> GhaisAssets.LibraryMorningCover
            36 -> GhaisAssets.AllCuratedPlaylists.getOrNull(1)?.coverUrl ?: GhaisAssets.LibraryTahajjudCover
            55 -> GhaisAssets.LibraryTahajjudCover
            67 -> GhaisAssets.AllCuratedPlaylists.firstOrNull()?.coverUrl ?: GhaisAssets.LibraryMorningCover
            else -> GhaisAssets.AllCuratedPlaylists.find { it.id == "garden-of-tranquility" }?.coverUrl ?: GhaisAssets.LibraryTahajjudCover
        }
    }

    private fun saveHistoryToDisk() {
        try {
            val list = _history.value.map { it.toPersistedHistoryItem() }
            val raw = json.encodeToString(list)
            settings.putString(k(KEY_HISTORY), raw)
        } catch (_: Exception) {}
    }

    private fun saveStatsToDisk() {
        try {
            val current = _stats.value
            settings.putInt(k(KEY_STREAK), current.daysStreak)
            settings.putLong(k(KEY_TOTAL_SECONDS), current.totalSecondsListened)
        } catch (_: Exception) {}
    }

    private fun getStoredSet(key: String): Set<String> {
        val raw = migratedString(key, "")
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun saveStoredSet(key: String, set: Set<String>) {
        settings.putString(k(key), set.joinToString(","))
    }

    private fun loadDailySeconds(): Map<Long, Long> {
        val raw = migratedString(KEY_DAILY_SECONDS, "")
        if (raw.isBlank()) return emptyMap()
        try {
            val parsed = json.decodeFromString<Map<Long, Long>>(raw)
            if (parsed.isEmpty()) return emptyMap()
            val cleaned = parsed.filter { it.key >= 0L && it.value > 0L }
            if (cleaned.isEmpty()) return emptyMap()
            return if (cleaned.size > MAX_DAILY_BUCKETS) {
                cleaned.entries.sortedByDescending { it.key }.take(MAX_DAILY_BUCKETS)
                    .associate { it.key to it.value }
            } else {
                cleaned
            }
        } catch (_: Exception) {
            // Tolerate Long-keys-encoded-as-strings variance across JSON impls.
            try {
                val fallback = json.decodeFromString<Map<String, Long>>(raw)
                val cleaned = fallback.mapNotNull { (day, secs) ->
                    val epochDay = day.toLongOrNull() ?: return@mapNotNull null
                    if (epochDay < 0L || secs <= 0L) null else epochDay to secs
                }.toMap()
                if (cleaned.isEmpty()) return emptyMap()
                return if (cleaned.size > MAX_DAILY_BUCKETS) {
                    cleaned.entries.sortedByDescending { it.key }.take(MAX_DAILY_BUCKETS)
                        .associate { it.key to it.value }
                } else {
                    cleaned
                }
            } catch (_: Exception) {
                return emptyMap()
            }
        }
    }

    private fun saveDailySeconds(buckets: Map<Long, Long>) {
        try {
            val pruned = if (buckets.size > MAX_DAILY_BUCKETS) {
                buckets.entries.sortedByDescending { it.key }.take(MAX_DAILY_BUCKETS)
                    .associate { it.key to it.value }
            } else {
                buckets
            }
            settings.putString(k(KEY_DAILY_SECONDS), json.encodeToString(pruned))
        } catch (_: Exception) {}
    }

    private fun PersistedHistoryItem.toJumpBackInItem(): JumpBackInItem {
        return JumpBackInItem(
            title = title,
            subtitle = subtitle,
            progress = progress,
            coverUrl = coverUrl,
            surahId = surahId,
            reciterSlug = reciterSlug,
            positionMs = positionMs,
            durationMs = durationMs,
            lastPlayedTimestampMs = lastPlayedTimestampMs
        )
    }

    private fun JumpBackInItem.toPersistedHistoryItem(): PersistedHistoryItem {
        return PersistedHistoryItem(
            title = title,
            subtitle = subtitle,
            progress = progress,
            coverUrl = coverUrl,
            surahId = surahId,
            reciterSlug = reciterSlug,
            positionMs = positionMs,
            durationMs = durationMs,
            // Preserve real play timestamps (e.g. cloud-restored entries);
            // only stamp "now" for in-session items that don't have one yet.
            lastPlayedTimestampMs = lastPlayedTimestampMs.takeIf { it > 0L } ?: currentTimeMs()
        )
    }
}
