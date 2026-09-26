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
 *   - likes: docId `"fav-" + sha256("<userId>|<slug>|<surahId>|<ayahNo>")[0..24)`
 *     (28 chars; full-tuple hash — no truncation, distinct per user)
 *   - playback_state: docId = userId (single document per user)
 *   - listening_stats: docId = userId (single document per user)
 *   - follows: docId `"fol-" + sha256("<userId>|<slug>")[0..24)` (28 chars;
 *     the userId is hashed in, so every user's follow is its own document
 *     under `documentSecurity: true`, and long slugs are never truncated)
 *   - schedules: docId = `schedule.id`
 *   - history: docId `"h-<slug>-<surahId>-<playedAtMs>"` (max 36 chars)
  *   - routine_backups: docId `"rtn-<routineId>"`
  *   - user_prefs: docId = userId (single document per user)
  *   - khatma_plans: docId `"khatma-<planId>"` (sanitized, max 36 chars)
 * - **Fault isolation:** each collection is wrapped in its own try/catch in
 *   the android actual, so one collection that is missing from a given
 *   deployment must not break the others.
 * - **Listening HISTORY syncs to [HISTORY]**
 *   (`history{user_id,track_json,played_at_ms,position_ms}`, capped at 15
 *   recent
 *   entries): push uploads `track_json = Json.encodeToString(TrackItem)` per
 *   entry with stable `h-…` doc ids and prunes cloud docs beyond the local
 *   set; pull restores into an empty local history via
 *   `UserUsageRepository.restoreHistory`, ordered by `played_at_ms` desc
 *   (empty-push still skipped to protect the cloud copy).
 *   The aggregate [UserUsageRepository.stats][com.ghais.data.repository.UserUsageRepository]
 *   snapshot is pushed to [STATS] alongside it.
 *
 * ## Local-store → collection mapping
 * | Local source | Collection | Document shape |
 * |---|---|---|
 * | FavoritesStore | [LIKES] (`likes{user_id,surah_id,ayah_no,level,reciter_slug?}`) | one doc per liked track; `ayah_no = track.ayahNo`, `level = "like"`. Push includes `reciter_slug` (string 64, optional) with a slug-less fallback while the attribute is unprovisioned; pull rebuilds a fully playable TrackItem (reciter name + audio URL via QuranDataRepository/Reciter, surah names via getSurahById) when the slug is present and known, else a synthetic `appwrite://likes/<docId>` placeholder |
 * | FollowStore | [FOLLOWS] (`follows{user_id,reciter_slug}`) | one doc per followed reciter; docId `"fol-" + sha256("<userId>|<slug>")[0..24)` |
 * | UserUsageRepository.stats | [STATS] (`listening_stats{user_id,total_seconds,days_streak,minutes_today,unique_surahs,unique_reciters,updated_at}`) | single doc per user (docId = userId); aggregate snapshot only (history syncs to [HISTORY] separately). Pull adopts via `UserUsageRepository.restoreStats` only when local stats are at fresh defaults (never overwrites non-zero local); `minutes_today` is valid only when cloud `updated_at` is today (else restores as 0); zero-push still skipped to protect the cloud copy |
 * | AudioEngine.currentTrack + position | [PLAYBACK] (`playback_state{user_id,current_ref,position_ms,queue_json,updated_at}`) | single doc per user (docId = userId); `current_ref = "<slug>/<surahId>"`, `position_ms` = playback position, `queue_json` = single-track array. Pull restores via `AudioEngine.prepareTrack` (paused preload, no autoplay, no FGS boot) when the player is empty; push is skipped when local is empty but cloud holds data (fresh-login guard). Playback changes (track/pause) join the debounced push trigger; position ticks stay excluded |
 * | SchedulesStore | [SCHEDULES] (`schedules{user_id,schedule_id,schedule_json,enabled}`) | one doc per schedule; docId = `schedule.id`, `schedule_json = Json.encodeToString(schedule)` |
 * | CustomRoutinesStore (ALL routines, private + public) | [ROUTINE_BACKUPS] (`routine_backups{user_id,routine_id,routine_json,updated_at}`) | one doc per routine; docId `"rtn-<routineId>"`, `routine_json = Json.encodeToString(routine)`. Separate from the public `playlists` publish flow (private routines never enter the catalog). Pull-if-empty restores via `CustomRoutinesStore.restoreAllJson` (empty-local only, malformed entries skipped per-item); empty-push still skipped to protect the cloud copy |
 * | OnboardingStore.goal + dailyGoalMinutes | [USER_PREFS] (`user_prefs{user_id,goal,daily_minutes}`) | single doc per user (docId = userId). Goal + daily minutes only — onboarding seen/done/step flags are never synced |
 * | KhatmaStore.plans | [KHATMA] (`khatma_plans{user_id,title,total_days,current_surah,current_ayah,percent}`) | one doc per plan; docId `"khatma-<planId>"` (sanitized, max 36 chars), `percent = progressPercentage`. The collection carries no plan id / streak / date attributes, so pull derives the plan id back from the doc id, restores streak 0 with `startDateMs = lastProgressMs = now`, and derives `totalAyahsRead` from `percent * 6236`. Pull-if-empty restores via `KhatmaStore.restoreAll`, empty-push still skipped |
 * | CustomRoutinesStore (public only) | `playlists` + `playlist_items` | push-only publish to the public catalog; one `playlists` doc per public routine (docId `"rtn-<routineId>"`), items replaced wholesale. Private routines are never uploaded here — they are backed up to [ROUTINE_BACKUPS] instead |
 * | FollowStore counts | [RECITER_STATS] (`reciter_stats{slug,followers_count,likes_count,updated_at}`) | public read-only (doc id = slug); client never writes. `SyncEngine.init` wires the reader into `FollowStore.countFetcher`; counts refresh best-effort after follows pull/push |
 * | UserUsageRepository.history | [HISTORY] (`history{user_id,track_json,played_at_ms}`) | one doc per entry (docId `"h-<slug>-<surahId>-<playedAtMs>"`, max 36 chars); `track_json = Json.encodeToString(TrackItem)` rebuilt from the history entry via reciter/surah lookups. Push capped at 100 recent + prune of cloud docs beyond the local set; pull-if-empty ordered by `played_at_ms` desc restores via `UserUsageRepository.restoreHistory`, empty-push still skipped |
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

    /**
     * Best-effort pull of the public editorial catalog (`playlists` with
     * `is_public = true`, limit 20 + `playlist_items` per playlist, limit 50
     * ordered by `position`) into `EditorialRepository`.
     *
     * Read-only: never writes, never deletes. Keeps the last-good cache on
     * ANY failure (offline / 404 / signed out — the `playlists` collection
     * requires an authenticated user). Safe to call from UI refresh paths;
     * `syncNow` also runs it inside its `playlists` collection block.
     * Never throws.
     */
    suspend fun refreshEditorial()
}
