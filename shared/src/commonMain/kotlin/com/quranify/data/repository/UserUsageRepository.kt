package com.quranify.data.repository

import com.quranify.data.seed.JumpBackInItem
import com.quranify.data.seed.QuranData
import com.quranify.data.seed.StitchAssets
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
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
 * UserUsageRepository:
 *
 * Provides live, real-time, persisted tracking of user listening history
 * ("Continue Listening") and listening statistics ("Your Stats").
 */
object UserUsageRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val KEY_HISTORY = "quranify_user_listening_history_v2"
    private const val KEY_LAST_DAY = "quranify_stats_last_day"
    private const val KEY_STREAK = "quranify_stats_streak"
    private const val KEY_SECONDS_TODAY = "quranify_stats_seconds_today"
    private const val KEY_TOTAL_SECONDS = "quranify_stats_total_seconds"
    private const val KEY_UNIQUE_RECITERS = "quranify_stats_unique_reciters"
    private const val KEY_UNIQUE_SURAHS = "quranify_stats_unique_surahs"

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
    private var lastSavedTimestampMs: Long = 0L

    init {
        loadHistory()
        loadStats()
        observeAudioEngine()
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun currentEpochDay(): Long = currentTimeMs() / 86_400_000L

    private fun loadHistory() {
        val rawJson = settings.getString(KEY_HISTORY, "")
        if (rawJson.isNotBlank()) {
            try {
                val parsed = json.decodeFromString<List<PersistedHistoryItem>>(rawJson)
                if (parsed.isNotEmpty()) {
                    _history.value = parsed.map { it.toJumpBackInItem() }
                    return
                }
            } catch (_: Exception) {}
        }

        // Initial default starter items for a clean fresh experience
        _history.value = listOf(
            JumpBackInItem(
                title = "Al-Kahf",
                subtitle = "Mishary Rashid Alafasy • Ready to play",
                progress = 0.0f,
                coverUrl = StitchAssets.LibraryMorningCover,
                surahId = 18,
                reciterSlug = "mishary",
                positionMs = 0L,
                durationMs = 33 * 60 * 1000L
            ),
            JumpBackInItem(
                title = "Ar-Rahman",
                subtitle = "Mishary Rashid Alafasy • Ready to play",
                progress = 0.0f,
                coverUrl = StitchAssets.LibraryTahajjudCover,
                surahId = 55,
                reciterSlug = "mishary",
                positionMs = 0L,
                durationMs = 19 * 60 * 1000L
            ),
            JumpBackInItem(
                title = "Al-Mulk",
                subtitle = "Mishary Rashid Alafasy • Ready to play",
                progress = 0.0f,
                coverUrl = StitchAssets.AllCuratedPlaylists.firstOrNull()?.coverUrl ?: StitchAssets.LibraryMorningCover,
                surahId = 67,
                reciterSlug = "mishary",
                positionMs = 0L,
                durationMs = 7 * 60 * 1000L
            ),
            JumpBackInItem(
                title = "Ya-Sin",
                subtitle = "Mishary Rashid Alafasy • Ready to play",
                progress = 0.0f,
                coverUrl = StitchAssets.AllCuratedPlaylists.getOrNull(1)?.coverUrl ?: StitchAssets.LibraryTahajjudCover,
                surahId = 36,
                reciterSlug = "mishary",
                positionMs = 0L,
                durationMs = 15 * 60 * 1000L
            )
        )
    }

    private fun loadStats() {
        val today = currentEpochDay()
        val savedDay = settings.getLong(KEY_LAST_DAY, 0L)
        var streak = settings.getInt(KEY_STREAK, 1).coerceAtLeast(1)
        var secondsToday = settings.getLong(KEY_SECONDS_TODAY, 0L)
        val totalSeconds = settings.getLong(KEY_TOTAL_SECONDS, 0L)

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
            durationMs = durationMs
        )

        _history.update { currentList ->
            val filtered = currentList.filterNot { it.surahId == track.surahId && it.reciterSlug == track.reciterSlug }
            (listOf(updatedItem) + filtered).take(15)
        }

        recordUniqueItem(track.reciterSlug, track.surahId)

        val now = currentTimeMs()
        if (now - lastSavedTimestampMs > 5000L) {
            lastSavedTimestampMs = now
            saveHistoryToDisk()
            saveStatsToDisk()
        }
    }

    private fun recordListeningTime(deltaMs: Long) {
        val deltaSeconds = deltaMs / 1000L
        if (deltaSeconds <= 0L) return

        val today = currentEpochDay()
        val savedDay = settings.getLong(KEY_LAST_DAY, 0L)

        var streak = _stats.value.daysStreak
        var secondsToday = settings.getLong(KEY_SECONDS_TODAY, 0L) + deltaSeconds
        val totalSeconds = settings.getLong(KEY_TOTAL_SECONDS, 0L) + deltaSeconds

        if (savedDay != today) {
            if (savedDay != 0L && today == savedDay + 1L) {
                streak += 1
            } else if (savedDay != 0L && today > savedDay + 1L) {
                streak = 1
            }
            secondsToday = deltaSeconds
            settings.putLong(KEY_LAST_DAY, today)
            settings.putInt(KEY_STREAK, streak)
        }

        settings.putLong(KEY_SECONDS_TODAY, secondsToday)
        settings.putLong(KEY_TOTAL_SECONDS, totalSeconds)

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
        val savedDay = settings.getLong(KEY_LAST_DAY, 0L)
        var streak = settings.getInt(KEY_STREAK, 1).coerceAtLeast(1)

        if (savedDay != today) {
            if (savedDay != 0L && today == savedDay + 1L) {
                streak += 1
            } else if (savedDay != 0L && today > savedDay + 1L) {
                streak = 1
            }
            settings.putLong(KEY_LAST_DAY, today)
            settings.putInt(KEY_STREAK, streak)
            settings.putLong(KEY_SECONDS_TODAY, 0L)

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

    private fun getCoverForSurah(surahId: Int): String {
        return when (surahId) {
            1 -> StitchAssets.LibraryMorningCover
            18 -> StitchAssets.LibraryMorningCover
            36 -> StitchAssets.AllCuratedPlaylists.getOrNull(1)?.coverUrl ?: StitchAssets.LibraryTahajjudCover
            55 -> StitchAssets.LibraryTahajjudCover
            67 -> StitchAssets.AllCuratedPlaylists.firstOrNull()?.coverUrl ?: StitchAssets.LibraryMorningCover
            else -> StitchAssets.AllCuratedPlaylists.find { it.id == "garden-of-tranquility" }?.coverUrl ?: StitchAssets.LibraryTahajjudCover
        }
    }

    private fun saveHistoryToDisk() {
        try {
            val list = _history.value.map { it.toPersistedHistoryItem() }
            val raw = json.encodeToString(list)
            settings.putString(KEY_HISTORY, raw)
        } catch (_: Exception) {}
    }

    private fun saveStatsToDisk() {
        try {
            val current = _stats.value
            settings.putInt(KEY_STREAK, current.daysStreak)
            settings.putLong(KEY_TOTAL_SECONDS, current.totalSecondsListened)
        } catch (_: Exception) {}
    }

    private fun getStoredSet(key: String): Set<String> {
        val raw = settings.getString(key, "")
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun saveStoredSet(key: String, set: Set<String>) {
        settings.putString(key, set.joinToString(","))
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
            lastPlayedTimestampMs = currentTimeMs()
        )
    }
}
