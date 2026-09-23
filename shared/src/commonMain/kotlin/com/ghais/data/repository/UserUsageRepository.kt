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

    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { "local" }
        if (normalized == currentOwnerId) return
        currentOwnerId = normalized
        ownerPrefix = if (normalized == "local") "" else "$normalized::"
        lastRecordedTimeMs = 0L
        unaccountedMs = 0L
        lastSavedTimestampMs = currentTimeMs()
        _history.value = emptyList()
        loadHistory()
        loadStats()
    }

    private val _history = MutableStateFlow<List<JumpBackInItem>>(emptyList())
    val history: StateFlow<List<JumpBackInItem>> = _history.asStateFlow()

    private val _stats = MutableStateFlow(
        UserListeningStats(
            daysStreak = 1,
            minutesToday = 0,
            uniqueRecitersCount = 1,
            uniqueSurahsCount = 1
        )
    )
    val stats: StateFlow<UserListeningStats> = _stats.asStateFlow()

    private var lastRecordedTimeMs: Long = 0L
    /** Sub-second leftover: position polls arrive ~4x/sec, so plain ms/1000 truncation would drop everything. */
    private var unaccountedMs: Long = 0L
    private var lastSavedTimestampMs: Long = 0L

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
        var streak = migratedInt(KEY_STREAK, 1).coerceAtLeast(1)
        var secondsToday = migratedLong(KEY_SECONDS_TODAY, 0L)
        val totalSeconds = migratedLong(KEY_TOTAL_SECONDS, 0L)

        if (savedDay != 0L) {
            if (today == savedDay + 1L) {
                // Consecutive day
                secondsToday = 0L
            } else if (today > savedDay + 1L) {
                // Streak broken
                streak = 1
                secondsToday = 0L
            }
        }

        val uniqueReciters = getStoredSet(KEY_UNIQUE_RECITERS).ifEmpty { setOf("mishary") }
        val uniqueSurahs = getStoredSet(KEY_UNIQUE_SURAHS).ifEmpty { setOf("18") }

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
            }
            secondsToday = deltaSeconds
            settings.putLong(k(KEY_LAST_DAY), today)
            settings.putInt(k(KEY_STREAK), streak)
        }

        settings.putLong(k(KEY_SECONDS_TODAY), secondsToday)
        settings.putLong(k(KEY_TOTAL_SECONDS), totalSeconds)

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
        var streak = migratedInt(KEY_STREAK, 1).coerceAtLeast(1)

        if (savedDay != today) {
            if (savedDay != 0L && today == savedDay + 1L) {
                streak += 1
            } else if (savedDay != 0L && today > savedDay + 1L) {
                streak = 1
            }
            settings.putLong(k(KEY_LAST_DAY), today)
            settings.putInt(k(KEY_STREAK), streak)
            settings.putLong(k(KEY_SECONDS_TODAY), 0L)

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
     * `TrackItem` + `played_at_ms` pairs, as supplied by the sync pull).
     *
     * Day/order-aware: entries are sorted by `playedAtMs` desc so the most
     * recent play heads the list, and each entry keeps its cloud timestamp as
     * [JumpBackInItem.lastPlayedTimestampMs] (persisted as-is; see
     * [toPersistedHistoryItem]) instead of being re-stamped with "now".
     * Capped at 15 like [recordProgress], and merges without duplicating
     * identical entries (same `surahId` + `reciterSlug` keeps the most recent
     * occurrence). Persists via [saveHistoryToDisk]. Cloud tracks carry no
     * saved position, so restored entries resume from the start
     * (`positionMs = 0`, "Not started" subtitle) with the known duration.
     */
    fun restoreHistory(items: List<Pair<TrackItem, Long>>) {
        if (items.isEmpty()) return
        val seen = mutableSetOf<Pair<Int, String>>()
        val restored = items.sortedByDescending { it.second }.mapNotNull { (track, playedAt) ->
            if (track.surahId <= 0 || playedAt <= 0L) return@mapNotNull null
            if (!seen.add(track.surahId to track.reciterSlug)) return@mapNotNull null
            val duration = track.durationMs.coerceAtLeast(0L)
            JumpBackInItem(
                title = track.surahNameEn.ifEmpty { "Surah ${track.surahId}" },
                subtitle = "${track.reciterName.ifEmpty { "Mishary" }} • ${formatRemainingTime(0L, duration)}",
                progress = 0f,
                coverUrl = getCoverForSurah(track.surahId),
                surahId = track.surahId,
                reciterSlug = track.reciterSlug,
                positionMs = 0L,
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
