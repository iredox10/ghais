package com.quranify.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * A scheduled daily recitation playback.
 *
 * @param id unique schedule identifier (see [SchedulesStore.newId]).
 * @param hour hour of day, 0..23.
 * @param minute minute of hour, 0..59.
 * @param reciterSlug legacy Reciter slug
 *   (resolvable via [QuranDataRepository.getReciterBySlug]).
 * @param fromSurah first surah of the range, 1..114.
 * @param toSurah last surah of the range, 1..114 and >= [fromSurah]
 *   (normalized on add/update).
 * @param durationMin null = play the range to its end; else stop after N minutes.
 * @param enabled whether the schedule fires.
 */
@Serializable
data class RecitationSchedule(
    val id: String,
    val hour: Int,
    val minute: Int,
    val reciterSlug: String,
    val fromSurah: Int,
    val toSurah: Int,
    val durationMin: Int? = null,
    val enabled: Boolean = true
)

/**
 * SchedulesStore: persisted store of recitation schedules.
 *
 * Mirrors [FavoritesStore]/[FollowStore] persistence approach
 * (multiplatform Settings + JSON-encoded list + StateFlow).
 * The exposed list is always sorted by hour/minute.
 * Every mutation persists and reprograms OS alarms via [ScheduleEngine.refresh].
 */
object SchedulesStore {
    private const val KEY_SCHEDULES = "quranify_recitation_schedules"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _schedules = MutableStateFlow<List<RecitationSchedule>>(emptyList())
    val schedules: StateFlow<List<RecitationSchedule>> = _schedules.asStateFlow()

    init {
        load()
    }

    /**
     * Generates a new unique schedule id.
     *
     * Clock-based (proven common code in this repo via kotlin.time.Clock;
     * no java.util): millisecond timestamp plus a random suffix to avoid
     * collisions when several schedules are created within the same millisecond.
     */
    fun newId(): String =
        "sch-" + Clock.System.now().toEpochMilliseconds() + "-" + (0..9999).random()

    /**
     * Adds a schedule (assigns nothing to the id), normalizing
     * hour/minute/surah range, then persists, refreshes the engine,
     * and returns the stored schedule.
     */
    fun add(schedule: RecitationSchedule): RecitationSchedule {
        val normalized = normalize(schedule)
        val sorted = (_schedules.value.filter { it.id != normalized.id } + normalized)
            .sortedWith(scheduleOrder())
        _schedules.value = sorted
        saveAndRefresh(sorted)
        return normalized
    }

    /** Replaces the schedule with the same id (same normalization as [add]). */
    fun update(schedule: RecitationSchedule) {
        val normalized = normalize(schedule)
        val sorted = (_schedules.value.filter { it.id != normalized.id } + normalized)
            .sortedWith(scheduleOrder())
        _schedules.value = sorted
        saveAndRefresh(sorted)
    }

    fun remove(id: String) {
        val updated = _schedules.value.filter { it.id != id }
        if (updated.size == _schedules.value.size) return
        _schedules.value = updated
        saveAndRefresh(updated)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val current = _schedules.value
        if (current.none { it.id == id && it.enabled != enabled }) return
        val updated = current.map { schedule ->
            if (schedule.id == id) schedule.copy(enabled = enabled) else schedule
        }.sortedWith(scheduleOrder())
        _schedules.value = updated
        saveAndRefresh(updated)
    }

    fun get(id: String): RecitationSchedule? {
        return _schedules.value.firstOrNull { it.id == id }
    }

    private fun normalize(schedule: RecitationSchedule): RecitationSchedule {
        val hour = schedule.hour.coerceIn(0, 23)
        val minute = schedule.minute.coerceIn(0, 59)
        var from = schedule.fromSurah.coerceIn(1, 114)
        var to = schedule.toSurah.coerceIn(1, 114)
        if (from > to) {
            val tmp = from
            from = to
            to = tmp
        }
        return schedule.copy(hour = hour, minute = minute, fromSurah = from, toSurah = to)
    }

    private fun scheduleOrder(): Comparator<RecitationSchedule> =
        compareBy<RecitationSchedule> { it.hour }.thenBy { it.minute }.thenBy { it.id }

    private fun load() {
        try {
            val raw = settings.getString(KEY_SCHEDULES, "")
            if (raw.isBlank()) {
                _schedules.value = emptyList()
                return
            }
            _schedules.value = json.decodeFromString<List<RecitationSchedule>>(raw)
                .map(::normalize)
                .sortedWith(scheduleOrder())
        } catch (_: Exception) {
            _schedules.value = emptyList()
        }
    }

    private fun saveAndRefresh(sorted: List<RecitationSchedule>) {
        try {
            settings.putString(KEY_SCHEDULES, json.encodeToString(sorted))
        } catch (_: Exception) {
        }
        try {
            ScheduleEngine.refresh(sorted)
        } catch (_: Exception) {
        }
    }
}

/**
 * Platform alarm programmer.
 *
 * Reprograms all OS alarms from the given schedules; called after every
 * [SchedulesStore] mutation. Actual implementations live in platform source sets.
 */
expect object ScheduleEngine {
    /** Reprogram all OS alarms from the given schedules (call after every mutation). */
    fun refresh(schedules: List<RecitationSchedule>)
}
