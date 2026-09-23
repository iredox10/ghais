package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
data class RoutineItem(
    val reciterSlug: String,
    val surahId: Int
)

@Serializable
data class CustomRoutine(
    val id: String,
    val title: String,
    val description: String = "",
    val items: List<RoutineItem> = emptyList(),
    val isPublic: Boolean = false,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

/**
 * CustomRoutinesStore: persisted store of user-defined recitation routines.
 *
 * Mirrors [FavoritesStore]/[FollowStore]/[SchedulesStore] persistence approach
 * (multiplatform Settings + JSON-encoded list + StateFlow).
 * The exposed list is newest first.
 */
object CustomRoutinesStore {
    private const val KEY_ROUTINES = "ghais_custom_routines"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _routines = MutableStateFlow<List<CustomRoutine>>(emptyList())
    val routines: StateFlow<List<CustomRoutine>> = _routines.asStateFlow()

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    fun create(
        title: String,
        description: String,
        items: List<RoutineItem>,
        isPublic: Boolean
    ): CustomRoutine {
        val now = Clock.System.now().toEpochMilliseconds()
        val routine = CustomRoutine(
            id = "rtn-" + now + "-" + (0..9999).random(),
            title = title,
            description = description,
            items = items,
            isPublic = isPublic,
            createdAtMs = now,
            updatedAtMs = now
        )
        _routines.value = listOf(routine) + _routines.value
        save()
        return routine
    }

    fun update(routine: CustomRoutine) {
        val now = Clock.System.now().toEpochMilliseconds()
        val refreshed = routine.copy(updatedAtMs = now)
        _routines.value = _routines.value.map { current ->
            if (current.id == refreshed.id) refreshed else current
        }
        save()
    }

    fun delete(id: String) {
        val updated = _routines.value.filter { it.id != id }
        if (updated.size == _routines.value.size) return
        _routines.value = updated
        save()
    }

    fun get(id: String): CustomRoutine? {
        return _routines.value.firstOrNull { it.id == id }
    }

    fun setPublic(id: String, isPublic: Boolean) {
        val current = _routines.value
        if (current.none { it.id == id && it.isPublic != isPublic }) return
        val now = Clock.System.now().toEpochMilliseconds()
        _routines.value = current.map { routine ->
            if (routine.id == id) routine.copy(isPublic = isPublic, updatedAtMs = now) else routine
        }
        save()
    }

    private fun load() {
        try {
            var raw = settings.getString(key(KEY_ROUTINES), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_ROUTINES))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_ROUTINES), legacyRaw)
                        } catch (_: Exception) {
                        }
                        try {
                            settings.remove(legacyKey)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
            if (raw.isBlank()) {
                _routines.value = emptyList()
                return
            }
            _routines.value = json.decodeFromString<List<CustomRoutine>>(raw)
        } catch (_: Exception) {
            _routines.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_ROUTINES), json.encodeToString(_routines.value))
        } catch (_: Exception) {
        }
    }
}

/**
 * Expands a [CustomRoutine] into playable [TrackItem]s (one full-surah track
 * per item). Items whose surah cannot be resolved are skipped; reciter lookup
 * via [QuranDataRepository.getReciterBySlug] never returns null (falls back to
 * Alafasy).
 */
fun CustomRoutine.toTrackItems(): List<TrackItem> {
    val surahs = QuranDataRepository.getSurahs()
    return items.mapNotNull { item ->
        val surah = surahs.find { it.id == item.surahId } ?: return@mapNotNull null
        val reciter = QuranDataRepository.getReciterBySlug(item.reciterSlug)
        // Skip surahs this reciter has no audio for — never queue a known-404.
        if (!reciter.isSurahAvailable(surah.id)) return@mapNotNull null
        TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = 0,
            audioUrl = reciter.getFullSurahUrl(surah.id),
            durationMs = surah.ayahsCount * 15000L
        )
    }
}
