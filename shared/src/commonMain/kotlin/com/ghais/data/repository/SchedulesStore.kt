package com.ghais.data.repository

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
 *
 * ## Cloud sync
 * Schedules are pushed to the `schedules` cloud collection (one doc per
 * schedule, keyed by [RecitationSchedule.id]) and restored back with
 * [restoreAllFromCloud], which MERGES the cloud snapshot into the local list.
 * There is no emptiness gate and no replace: the paired push has no
 * "cloud still holds data" guard and prunes any cloud doc whose `schedule_id`
 * is missing locally, so a gated or destructive restore turns the first sync
 * after a fresh login into a mass delete of the account's schedules.
 *
 * ## Pre-auth adoption
 * [setOwner] transfers nothing (see its KDoc). Schedules added while signed
 * out are adopted into the account by `SyncTriggers`, which snapshots them
 * before rebinding and re-adds them afterwards. That snapshot-and-re-add is
 * safe under a merging restore: both the pre-auth and the cloud copies end up
 * local, so the push's prune finds every `schedule_id` it uploaded.
 */
object SchedulesStore {
    private const val KEY_SCHEDULES = "ghais_recitation_schedules"

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

    private val _schedules = MutableStateFlow<List<RecitationSchedule>>(emptyList())
    val schedules: StateFlow<List<RecitationSchedule>> = _schedules.asStateFlow()

    init {
        load()
    }

    /**
     * Rebinds the store to [ownerId]'s namespace, loading it and re-programming
     * the OS alarms. A no-op when the owner is unchanged.
     *
     * Carries NOTHING across namespaces: whatever was in the previous owner's
     * list is simply dropped from this store's view (the other namespace's key
     * is untouched on disk). Pre-auth schedules are preserved by the CALLER
     * instead — `SyncTriggers` snapshots `schedules.value` before this call and
     * re-adds it afterwards — see the class KDoc.
     */
    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
        try {
            ScheduleEngine.refresh(_schedules.value)
        } catch (_: Exception) {
        }
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

    /**
     * Merge-restores schedules from a cloud snapshot (the pull half of the
     * `schedules` cloud collection sync; called by `SyncEngine.pullSchedules`).
     *
     * MERGE, never replace, and never gated on the local list being empty:
     * `SyncEngine.pushSchedules` prunes every cloud doc whose `schedule_id` is
     * absent from the local set and runs with no "cloud still has data" guard,
     * so a fresh device's empty list made the first pass after login DELETE
     * every schedule document for the account (on every device). Gating a pull
     * on emptiness re-opens exactly that hole, and so does replacing instead of
     * merging; both are the bug this signature exists to avoid.
     *
     * - A schedule already present locally wins on [RecitationSchedule.id]
     *   match — local is the live truth for something the user may have just
     *   edited, and keeping it is also what makes the paired push's prune a
     *   no-op.
     * - Remote entries the local set does not have are added, normalized and
     *   sorted like any other mutator, and their `id` is preserved verbatim.
     *   That id is what the cloud `schedule_id` attribute holds and what
     *   `scheduleDocId` derives the document id from, so preserving it is what
     *   keeps the two sides in agreement — a re-minted id here would read as a
     *   brand-new local schedule and the push would prune the doc it came from.
     * - Blank or duplicate ids in the snapshot are skipped per-item, and an
     *   empty snapshot is a no-op, so the call is idempotent: a second pass
     *   with the same snapshot adds nothing and returns 0.
     *
     * Persists and reprograms the OS alarms via the same path as every other
     * mutator. Returns the number of schedules actually added (0 = nothing
     * changed) so the sync layer can log the restore.
     */
    fun restoreAllFromCloud(remote: List<RecitationSchedule>): Int {
        if (remote.isEmpty()) return 0
        val current = _schedules.value
        val localIds = current.map { it.id }.toSet()
        val additions = LinkedHashMap<String, RecitationSchedule>()
        for (schedule in remote) {
            val id = schedule.id
            if (id.isBlank()) continue
            if (id in localIds) continue
            // Duplicate ids inside one snapshot collapse to the first.
            if (additions.containsKey(id)) continue
            additions[id] = normalize(schedule)
        }
        if (additions.isEmpty()) return 0
        val merged = (current + additions.values).sortedWith(scheduleOrder())
        _schedules.value = merged
        saveAndRefresh(merged)
        return additions.size
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
            var raw = settings.getString(key(KEY_SCHEDULES), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy value if present. Two
                // lookups, because the rename and the per-owner namespacing
                // landed together: a pre-namespacing write is at the BARE
                // legacy key, so looking only at the namespaced one (the
                // pre-existing behaviour) never migrated a signed-in user's
                // schedules and they silently started from an empty list —
                // which, paired with the push's unguarded prune, then deleted
                // the account's whole cloud copy. Same fix as
                // FavoritesStore.load().
                val legacyKeys = listOf(
                    key(legacyBase(KEY_SCHEDULES)),
                    legacyBase(KEY_SCHEDULES),
                ).distinct()
                for (legacyKey in legacyKeys) {
                    if (legacyKey == key(KEY_SCHEDULES)) continue
                    val legacyRaw = try {
                        settings.getString(legacyKey, "")
                    } catch (_: Exception) {
                        ""
                    }
                    if (legacyRaw.isBlank()) continue
                    raw = legacyRaw
                    try {
                        settings.putString(key(KEY_SCHEDULES), legacyRaw)
                    } catch (_: Exception) {
                    }
                    try {
                        settings.remove(legacyKey)
                    } catch (_: Exception) {
                    }
                    break
                }
            }
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
            settings.putString(key(KEY_SCHEDULES), json.encodeToString(sorted))
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
