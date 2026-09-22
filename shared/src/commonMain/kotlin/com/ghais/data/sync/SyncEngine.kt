package com.ghais.data.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Offline-first, last-write-wins cross-device sync against Appwrite Cloud
 * (database [DB_ID]).
 *
 * ## Semantics
 * - **Pull (restore):** only fills EMPTY local stores (first login on a new
 *   device). Pull never overwrites non-empty local data, so local edits made
 *   offline always win over older cloud state.
 * - **Push:** debounced upload of the full local state per collection, using
 *   idempotent upserts with stable document IDs (no delete-then-insert):
 *   - likes: docId `"fav-<slug>-<surahId>-<ayahNo>"`
 *   - playback_state: docId = userId (single document per user)
 *   - listening_stats: docId = userId (single document per user)
 *   - follows: docId `"fol-<slug>"`
 *   - schedules: docId = `schedule.id`
 * - **Fault isolation:** each collection is wrapped in its own try/catch in
 *   the android actual, so one missing collection (the NEW [FOLLOWS], [STATS],
 *   [SCHEDULES] collections may not exist yet — the user creates them manually)
 *   must not break the other collections.
 * - **Listening HISTORY stays local-only in v1** and is never uploaded: the
 *   per-day/per-ayah history log has no cloud collection by design. Only the
 *   aggregate [UserUsageRepository.stats][com.ghais.data.repository.UserUsageRepository]
 *   snapshot is pushed to [STATS].
 *
 * ## Local-store → collection mapping
 * | Local source | Collection | Document shape |
 * |---|---|---|
 * | FavoritesStore | [LIKES] (`likes{user_id,surah_id,ayah_no,level,reciter_slug?}`) | one doc per liked track; `ayah_no = track.ayahNo`, `level = "like"`. Push includes `reciter_slug` (string 64, optional) with a slug-less fallback while the attribute is unprovisioned; pull rebuilds a fully playable TrackItem (reciter name + audio URL via QuranDataRepository/Reciter, surah names via getSurahById) when the slug is present and known, else a synthetic `appwrite://likes/<docId>` placeholder |
 * | FollowStore | [FOLLOWS] (`follows{user_id,reciter_slug}`) | one doc per followed reciter; docId `"fol-<slug>"` |
 * | UserUsageRepository.stats | [STATS] (`listening_stats{user_id,total_seconds,days_streak,minutes_today,unique_surahs,unique_reciters,updated_at}`) | single doc per user (docId = userId); aggregate snapshot only, history stays local. Pull adopts only when local stats are at fresh defaults (never overwrites non-zero local); `minutes_today` is valid only when cloud `updated_at` is today (else restores as 0). No UserUsageRepository restore API yet, so pull validates + protects the cloud copy by skipping the zero-push |
 * | AudioEngine.currentTrack + position | [PLAYBACK] (`playback_state{user_id,current_ref,position_ms,queue_json,updated_at}`) | single doc per user (docId = userId); `current_ref = "<slug>/<surahId>"`, `position_ms` = playback position, `queue_json` = single-track array. Pull rebuilds the TrackItem (names via getSurahById, URLs via Reciter helpers) when the player is empty; restore is metadata-only until AudioEngine gains a paused-load path (no autoplay) |
 * | SchedulesStore | [SCHEDULES] (`schedules{user_id,schedule_id,schedule_json,enabled}`) | one doc per schedule; docId = `schedule.id`, `schedule_json = Json.encodeToString(schedule)` |
 *
 * Platform work (Appwrite SDK calls, debounce, store wiring) lives in the
 * `androidMain`/`iosMain` actuals; this expect declaration keeps the common
 * UI contract stable.
 */
expect object SyncEngine {

    /**
     * Current sync lifecycle state.
     *
     * `DISABLED` while signed out; `SYNCING` during a pass; `ERROR` when the
     * last pass failed (see [lastError]); `IDLE` otherwise.
     */
    val status: StateFlow<SyncStatus>

    /** Epoch-millis of the last successful pass, or null if never synced. */
    val lastSyncedAt: StateFlow<Long?>

    /** Human-readable message from the last failure, or null if none. */
    val lastError: StateFlow<String?>

    /**
     * Runs one pull-then-push pass now.
     *
     * Pull restores only empty local stores; push uploads full local state per
     * collection with per-collection try/catch. No-op when sync is disabled.
     */
    suspend fun syncNow()
}
