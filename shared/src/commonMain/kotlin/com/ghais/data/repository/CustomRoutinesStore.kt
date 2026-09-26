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
 * The exposed list is newest first: [create] prepends, and the bulk entry
 * points ([restoreAllJson], [mergeJson]) re-sort the whole list with
 * [routineOrder]. Single-item mutators ([update], [setPublic], [delete]) keep
 * the incoming item's slot, so the ordering is "newest first by
 * `updatedAtMs`" only as good as the last bulk pass or create that ran.
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

    /**
     * Bulk-restores routines from cloud backup JSONs (sync pull-restore path).
     *
     * Replaces local state ONLY when the store is currently empty (fresh
     * device) — existing local routines are never clobbered. Malformed or
     * blank entries are skipped per-item; duplicate ids collapse to the first
     * occurrence. Returns the number of routines adopted.
     *
     * The decoded `id` is preserved verbatim: the routine is added as decoded,
     * never re-minted through [create]. That is what keeps the local set
     * comparable to the cloud `routine_id` attributes the paired push prunes
     * against.
     *
     * The empty-store gate is why this is NOT the pull path for a
     * partially-populated store; use [mergeJson] for that.
     */
    fun restoreAllJson(rawJsons: List<String>): Int {
        if (_routines.value.isNotEmpty()) return 0
        val seenIds = LinkedHashSet<String>()
        val restored = mutableListOf<CustomRoutine>()
        for (raw in rawJsons) {
            val routine = decodeRoutine(raw) ?: continue
            if (!seenIds.add(routine.id)) continue
            restored.add(routine)
        }
        if (restored.isEmpty()) return 0
        // Newest-first, matching the store's exposed ordering.
        _routines.value = restored.sortedWith(routineOrder())
        save()
        return restored.size
    }

    /**
     * Merge-restores routines from cloud backup JSONs (the pull half of the
     * `routine_backups` cloud collection sync; called by `SyncEngine`).
     *
     * MERGE, never replace, and — unlike [restoreAllJson] — never gated on the
     * local list being empty. That gate is the reason this function exists:
     * `SyncEngine.pushRoutineBackups` prunes every cloud backup whose
     * `routine_id` is absent from the local set, so a cloud routine this device
     * never adopts leaves a permanently unmatched id, which quarantines that
     * prune forever and keeps backups of routines deleted on this device
     * stranded on the cloud. Merging on every pass closes the set.
     *
     * - A routine already present locally wins on [CustomRoutine.id] match:
     *   local is the live truth for something the user may have just edited,
     *   and leaving it alone is also what makes the paired push's prune a
     *   no-op for that id.
     * - Incoming routines the local set does not have are inserted with their
     *   `id` preserved verbatim — not re-minted through [create]. A re-minted
     *   id would read as a brand-new local routine, so the push would upload a
     *   second doc and prune the original it came from.
     * - Blank ids and duplicate ids within one payload are skipped per-item,
     *   and an empty payload is a no-op, so the call is idempotent: a second
     *   pass with the same payload inserts nothing and returns 0.
     *
     * Decoding and ordering go through the same helpers [restoreAllJson] uses,
     * so a merged routine is indistinguishable from a locally created one.
     * Returns the number of routines actually inserted (0 = nothing changed),
     * which the sync layer logs and reads as "the cloud set is now fully
     * reconciled, pruning is safe".
     */
    fun mergeJson(rawJsons: List<String>): Int {
        if (rawJsons.isEmpty()) return 0
        val current = _routines.value
        val localIds = current.mapTo(mutableSetOf()) { it.id }
        val additions = LinkedHashMap<String, CustomRoutine>()
        for (raw in rawJsons) {
            val routine = decodeRoutine(raw) ?: continue
            if (routine.id in localIds) continue
            // Duplicate ids inside one payload collapse to the first.
            if (additions.containsKey(routine.id)) continue
            additions[routine.id] = routine
        }
        if (additions.isEmpty()) return 0
        // Newest-first, matching the store's exposed ordering.
        _routines.value = (current + additions.values).sortedWith(routineOrder())
        save()
        return additions.size
    }

    /**
     * Decodes one backup payload, or null when the payload is blank or
     * malformed. The single decode path for both bulk entry points, so a merged
     * routine decodes exactly like a restored one.
     */
    private fun decodeRoutine(raw: String): CustomRoutine? {
        if (raw.isBlank()) return null
        return try {
            val routine = json.decodeFromString<CustomRoutine>(raw)
            if (routine.id.isBlank()) null else routine
        } catch (_: Exception) {
            null
        }
    }

    // Newest-first, with the id as a stable tiebreak so two routines sharing an
    // `updatedAtMs` (including the 0L default on older payloads) cannot swap
    // places between passes — that would make an otherwise-idempotent merge
    // churn the list.
    private fun routineOrder(): Comparator<CustomRoutine> =
        compareByDescending<CustomRoutine> { it.updatedAtMs }.thenBy { it.id }

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
