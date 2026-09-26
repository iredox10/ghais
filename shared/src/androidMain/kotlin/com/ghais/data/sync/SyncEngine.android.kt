package com.ghais.data.sync

import android.util.Log
import com.ghais.data.auth.AppwriteConfig
import com.ghais.data.auth.AuthRepository
import com.ghais.data.auth.PersistentCookieJar
import com.ghais.data.repository.Broadcast
import com.ghais.data.repository.BroadcastRepository
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.EditorialPlaylist
import com.ghais.data.repository.EditorialPlaylistItem
import com.ghais.data.repository.EditorialRepository
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.KhatmaStore
import com.ghais.data.repository.MasteryStatus
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.SearchHistoryStore
import com.ghais.domain.model.Reciter
import com.ghais.data.repository.UserUsageRepository
import com.ghais.domain.model.KhatmaPlan
import com.ghais.data.repository.StatsSnapshot
import com.ghais.ui.screens.reciters.remotePhotoFetcher
import com.russhwolf.settings.Settings
import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Android live implementation of the `SyncEngine` expect object declared in
 * commonMain (sibling-owned: `SyncStatus` + `expect object SyncEngine` with
 * `status` / `lastSyncedAt` / `lastError` flows and `suspend fun syncNow()`).
 *
 * Auth: builds its own [Client] from [AppwriteConfig] endpoint/project and a
 * fresh [PersistentCookieJar] (same `ghais_appwrite_cookies` SharedPreferences
 * file the auth actual uses, so the `a_session_*` session cookie is sent and
 * calls are authenticated). No API keys, no JWT plumbing.
 *
 * Needs an Android context for the cookie jar: the owner must call
 * `SyncEngine.init(this)` once in `MainActivity.onCreate` (alongside the
 * existing `AuthRepository.init(this)` line).
 *
 * Flow: DISABLED when unconfigured/signed-out; else SYNCING, then per
 * collection pull + push, each in its own try/catch (first failure wins for
 * [lastError]). `lastSyncedAt` is set on full success only. All network runs
 * on Dispatchers.IO. Failures go to Log.e("GhaisSync").
 *
 * ## Pull is a MERGE, not an adopt-only-on-empty
 *
 * Gating a pull on "local is empty" is only correct while the ONLY way local
 * can be non-empty is "the user already synced here". That stopped being true:
 * [com.ghais.data.repository.UserUsageRepository] and the stores it lives
 * beside now adopt pre-auth (signed-out) listening, likes, follows and plans
 * into the freshly-bound account, and a track started in the 2s between login
 * and the first pass also lands locally. So "one stale local row" is the NORMAL
 * state of a new device, and an empty-gated pull silently skipped the whole
 * cloud history for it — while the paired push, which converges to the exact
 * local set, then DELETED that history from the cloud. Every pull here is
 * therefore a union that keeps local data and adopts the rows local lacks:
 * - HISTORY / STATS: per-key union, local wins the slot (see
 *   [pullHistory] / [pullStats]); aggregates are per-field `max`, so a cloud
 *   value can only ever add to the local snapshot, never shrink it.
 * - LIKES / FOLLOWS / KHATMA / SCHEDULES / HIFZ_MASTERY / SEARCH_HISTORY: set
 *   union, which also makes the paired push prune a no-op instead of a
 *   delete-everything-the-cloud-has pass.
 * Only the genuinely single-valued, live-state collections keep an
 * empty-gate, because there "local is newer" is the correct answer:
 * [pullPlaybackIfEmpty] (the position being played right now), [pullUserPrefsIfEmpty]
 * (a goal just chosen on this device) and [pullRoutineBackupsIfEmpty]
 * (whose store has no id-preserving merge — see the note there, and the prune
 * quarantine that keeps its delete half from ever firing blind).
 *
 * Every per-user list read is PAGINATED ([listForUser]): Appwrite returns a
 * bounded default page, so a single unpaginated read both under-restored a
 * heavy account and — because the prune loops converge the cloud to the exact
 * local set — deleted everything past the page it never saw.
 *
 * ## Push robustness
 *
 * `SyncTriggers`' only listening-related trigger is `debounce(8000)` over
 * `AudioEngine.currentTrack`/`isPlaying` and `UserUsageRepository.stats`, but
 * `stats.totalSecondsListened` ticks about once a second DURING playback, so
 * the debounce window never elapses while audio plays: forty minutes of
 * listening followed by a force-quit uploaded nothing. That file is not ours
 * to edit, so the guarantee is made here — see the listening push heartbeat
 * ([startListeningPushPump]), which pushes STATS/HISTORY/PLAYBACK on its own
 * cadence whenever local listening state has actually moved, and skips the
 * network entirely when it has not.
 */
actual object SyncEngine {

    private const val TAG = "GhaisSync"

    private const val PLAYLISTS = "playlists"
    private const val PLAYLIST_ITEMS = "playlist_items"
    private const val RECITERS = "reciters"
    private const val TELEMETRY = "telemetry"
    /** Throttle window for the device-telemetry upload (at most once per 24h per device). */
    private const val TELEMETRY_THROTTLE_MS = 24L * 60 * 60 * 1000L
    private const val TELEMETRY_PREF_PREFIX = "ghais_telemetry_last_upload_"
    private const val ROUTINE_DOC_PREFIX = "rtn-"

    // Declared here rather than in the sibling-owned `SyncModels.kt`, which this
    // file does not edit — same as TELEMETRY / PLAYLISTS above. Ids only: the
    // attribute names they are read and written by live in
    // `appwrite/collections.json`, which no code hardcodes a copy of.
    private const val HIFZ_MASTERY = "hifz_mastery"
    private const val SEARCH_HISTORY = "search_history"

    /**
     * `audioUrl` scheme the likes pull uses for a cloud like whose reciter slug
     * is blank or unknown, so the row is still identity-stable in
     * [FavoritesStore]. Mirrors the same constant on the store side.
     */
    private const val LIKES_PLACEHOLDER_PREFIX = "appwrite://likes/"

    // Must match the local cap enforced by `UserUsageRepository.recordProgress`
    // and `restoreHistory`. A higher value only inflates the pull query and
    // the push `take`, since the local list can never exceed 15.
    private const val HISTORY_CAP = 15

    // Must match `SearchHistoryStore`'s own MAX_SIZE: a higher value only
    // inflates the pull query, since the local list can never exceed it.
    private const val SEARCH_HISTORY_CAP = 15

    /** Column size of `search_history.query` (string 128, required). */
    private const val SEARCH_HISTORY_QUERY_MAX_CHARS = 128

    // Database/collection ids come from the sibling-owned SyncModels.kt
    // (same package: DB_ID, LIKES, PLAYBACK, FOLLOWS, STATS, SCHEDULES, HISTORY).
    // Doc-ID schemes and document shapes follow the SyncEngine.kt KDoc table.

    private val json = Json { ignoreUnknownKeys = true }

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    actual val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    actual val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    actual val lastError: StateFlow<String?> = _lastError.asStateFlow()

    @Volatile
    private var appContext: android.content.Context? = null

    // Owner-independent throttle store (multiplatform-settings, same prefs
    // mechanism UserUsageRepository uses; raw keys, no owner prefix — this is
    // device state, not user state).
    private val telemetryPrefs: Settings by lazy { Settings() }

    // ------------------------------------------------- listening push pump
    //
    // See the class KDoc: `SyncTriggers` cannot fire its debounced pass while
    // audio is playing, so the "my listening reached the cloud" guarantee is
    // made here, in a self-contained loop that needs no trigger wiring.
    //
    // The scope is process-lifetime and never cancelled: the loop is idle
    // (a `delay` and two flow reads) whenever there is no session, no network
    // or nothing to push, and it exits only with the process. SupervisorJob so
    // a thrown tick can never take the process down with it.

    /** Idle cadence of the heartbeat; also its retry cadence after a failure. */
    private const val LISTENING_PUSH_INTERVAL_MS = 20_000L

    /** Cadence used to re-push state that moved while a pass was in flight. */
    private const val LISTENING_PUSH_RETRY_MS = 5_000L

    /**
     * Cap on consecutive quick retries. Bounded on purpose: a listener who
     * keeps playing keeps changing the state (that is the normal case, and the
     * retry only fires when it moved DURING a pass, i.e. mid-network), and a
     * device with a wrong clock could otherwise re-push on every tick forever.
     */
    private const val LISTENING_MAX_QUICK_RETRIES = 3

    private val pumpScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var pumpStarted: Boolean = false

    /**
     * Extra (non-expect) member: caches the app context for the cookie jar.
     * Actual objects may declare members beyond the expect contract.
     * MUST be called once from MainActivity.onCreate:
     * `com.ghais.data.sync.SyncEngine.init(this)`.
     */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        // Wire the public reciter_stats reader into FollowStore so the UI can
        // display live follower counts (null = unknown = hidden).
        if (FollowStore.countFetcher == null) {
            FollowStore.countFetcher = { slug -> followerCount(slug) }
        }
        // Wire the public reciters photo reader into ReciterPhotos so avatars
        // can upgrade to cloud portraits (null = local-only fallback).
        if (remotePhotoFetcher == null) {
            remotePhotoFetcher = { slug -> reciterImage(slug) }
        }
        // Wire the public broadcasts pull into BroadcastRepository so the
        // inbox refreshes without a login (PUBLIC_READ collection).
        if (BroadcastRepository.cloudFetcher == null) {
            BroadcastRepository.cloudFetcher = { fetchBroadcasts() }
        }
        // Wire the public reciter catalog pull into ReciterCloudCache so the
        // catalog can refresh on a timer, independently of a user sync
        // (PUBLIC_READ collection). This is what makes a reciter uploaded in
        // the admin panel appear for users without any local action.
        if (com.ghais.data.repository.ReciterCloudCache.catalogFetcher == null) {
            com.ghais.data.repository.ReciterCloudCache.catalogFetcher = {
                val ctx = appContext
                if (ctx == null) emptyList() else fetchReciterCatalog(databases(ctx))
            }
        }
        // Listening push heartbeat. Started here (not in a `start()` call
        // SyncTriggers owns) because `init` is the one entry point the app
        // already guarantees to call exactly once, before sign-in, and the
        // loop gates itself on a live session every tick — so it costs nothing
        // while signed out and needs no trigger file to be wired.
        startListeningPushPump()
    }

    actual suspend fun syncNow() {
        if (!AppwriteConfig.isConfigured()) {
            _status.value = SyncStatus.DISABLED
            return
        }
        val userId = AuthRepository.session.value?.userId
        if (userId == null) {
            _status.value = SyncStatus.DISABLED
            return
        }
        val ctx = appContext
        if (ctx == null) {
            val msg = "SyncEngine.init(context) has not been called yet."
            Log.e(TAG, msg)
            _lastError.value = msg
            _status.value = SyncStatus.ERROR
            return
        }
        if (_status.value == SyncStatus.SYNCING) return
        _status.value = SyncStatus.SYNCING
        var firstError: String? = null
        // Listening state at the moment the pass started, so the tail can tell
        // whether it moved WHILE we were pushing it (see below).
        val listeningAtStart = listeningStateFingerprint()
        withContext(Dispatchers.IO) {
            val db = databases(ctx)
            suspend fun runCollection(name: String, block: suspend () -> Unit) {
                try {
                    block()
                } catch (e: Exception) {
                    if (isNotFound(e)) {
                        // Collection not provisioned in the Console yet: skip
                        // without poisoning the whole sync (see file KDoc / gaps).
                        Log.w(TAG, "Collection '$name' not found; skipping.")
                    } else {
                        Log.e(TAG, "$name sync failed", e)
                        if (firstError == null) {
                            firstError =
                                "$name: ${e.message?.takeIf { it.isNotBlank() } ?: e::class.simpleName}"
                        }
                    }
                }
            }
            runCollection(LIKES) {
                val cloudHasData = pullLikes(db, userId)
                pushLikes(db, userId, cloudHasData)
            }
            runCollection(FOLLOWS) {
                pullFollows(db, userId)
                pushFollows(db, userId)
            }
            // Best-effort follower-count refresh from public reciter_stats
            // (covers login pull + debounced push after follow/unfollow; one
            // batched list per TTL serves every slug — see reciterStatsSnapshot).
            // The client never writes reciter_stats, so counts stay unknown
            // (UI hides them) until the collection is populated out-of-band;
            // a local follow does NOT increment anything.
            try {
                FollowStore.refreshCounts(FollowStore.followedSlugs.value)
            } catch (e: Exception) {
                Log.w(TAG, "follower count refresh failed", e)
            }
            runCollection(PLAYBACK) {
                val cloudHasData = pullPlaybackIfEmpty(db, userId)
                pushPlayback(db, userId, cloudHasData)
            }
            runCollection(STATS) {
                val cloudHasData = pullStats(db, userId)
                pushStats(db, userId, cloudHasData)
            }
            runCollection(HISTORY) {
                val cloudHasData = pullHistory(db, userId)
                pushHistory(db, userId, cloudHasData)
            }
            runCollection(SCHEDULES) {
                val cloudHasData = pullSchedules(db, userId)
                pushSchedules(db, userId, cloudHasData)
            }
            runCollection(KHATMA) {
                val cloudHasData = pullKhatma(db, userId)
                pushKhatma(db, userId, cloudHasData)
            }
            runCollection(HIFZ_MASTERY) {
                // Bind before touching the store. `HifzMasteryStore`'s snapshot
                // API is namespace-scoped and, unbound, reports and writes the
                // DEVICE-GLOBAL `local` profile — which the next sign-in would
                // then adopt (see its `setOwner` KDoc). `SyncTriggers`' session
                // collector does not bind this store yet, so this pass does.
                // It is idempotent (`setOwner` returns early on the same id), so
                // it is a no-op the moment that collector does own the binding,
                // and `syncNow` only ever runs signed-in, so the owner passed is
                // always a real account — never "local".
                HifzMasteryStore.setOwner(userId)
                val cloudHasData = pullHifzMastery(db, userId)
                pushHifzMastery(db, userId, cloudHasData)
            }
            runCollection(SEARCH_HISTORY) {
                // Same reason as the block above: bind before read/write, or the
                // snapshot lands in the device-global `local` namespace.
                SearchHistoryStore.setOwner(userId)
                val cloudHasData = pullSearchHistory(db, userId)
                pushSearchHistory(db, userId, cloudHasData)
            }
            runCollection(PLAYLISTS) {
                refreshEditorial()
                pushRoutines(db, userId)
            }
            runCollection(ROUTINE_BACKUPS) {
                val pull = pullRoutineBackupsIfEmpty(db, userId)
                pushRoutineBackups(db, userId, pull)
            }
            runCollection(USER_PREFS) {
                pullUserPrefsIfEmpty(db, userId)
                pushUserPrefs(db, userId)
            }
            // Reciter catalog (Appwrite source of truth): refresh the
            // in-memory cloud overlay; bundled seeds stay the fallback.
            try {
                refreshReciterCatalog(db)
            } catch (e: Exception) {
                Log.w(TAG, "reciter catalog refresh failed", e)
            }
            // Public broadcasts inbox: newest-first pull into the in-memory
            // BroadcastRepository cache (PUBLIC_READ, so this also succeeds
            // for the fetcher path outside syncNow).
            try {
                BroadcastRepository.refresh()
            } catch (e: Exception) {
                Log.w(TAG, "broadcasts refresh failed", e)
            }
            // Device telemetry: only after a fully successful pass, throttled,
            // and ALWAYS silent (never touches firstError — must not break sync).
            try {
                if (firstError == null) pushTelemetryIfDue(db, userId, ctx)
            } catch (e: Exception) {
                Log.w(TAG, "telemetry upload skipped", e)
            }
        }
        if (firstError == null) {
            _lastError.value = null
            _lastSyncedAt.value = System.currentTimeMillis()
            _status.value = SyncStatus.IDLE
        } else {
            _lastError.value = firstError
            _status.value = SyncStatus.ERROR
        }
        // A pass takes seconds, and seconds of playback accrue during it, so
        // the state we just uploaded is already stale by the time we finish.
        // Tell the heartbeat to come back on its quick retry instead of
        // waiting out the full interval; it re-checks the fingerprint itself
        // and no-ops when nothing actually moved.
        if (listeningAtStart != listeningStateFingerprint()) {
            listeningPushUrgent = true
        }
    }

    // --------------------------------------------------- listening heartbeat
    //
    // Why this exists (see the class KDoc for the trigger-side story):
    // `SyncTriggers` debounces `stats` + `currentTrack`/`isPlaying` by 8s, and
    // `stats.totalSecondsListened` changes about once a second while audio
    // plays, so the debounce NEVER elapses during a listen. A user who listened
    // for forty minutes and force-quit pushed nothing, and the next thing that
    // uploads is a manual pull. The trigger file belongs to another owner, so
    // the push guarantee is made from this side instead.
    //
    // Design constraints this satisfies:
    // - Only the three listening collections are pushed (STATS, HISTORY,
    //   PLAYBACK). The other eight stay owned by the debounced full pass, so
    //   the heartbeat can never race the thing it would duplicate.
    // - Cheap when idle: a fingerprint of exactly the state those three pushes
    //   would upload gates every tick, so an unchanged state costs zero
    //   requests — not even the list read. A paused, finished listen costs one
    //   fingerprint per interval and nothing else.
    // - Bounded: one pass at a time (the loop is sequential and a full
    //   `syncNow` in flight makes the tick skip), at most
    //   [LISTENING_MAX_QUICK_RETRIES] quick retries back to back, and
    //   [LISTENING_PUSH_INTERVAL_MS] between steady-state passes.
    // - Pull-then-push per collection, identical to the full pass, so the
    //   "empty local must not clobber the cloud snapshot" guards still apply
    //   (a push-only heartbeat would overwrite a new device's stats with
    //   zeros, because `pushStats`' cloud guard is fed by the pull).
    // - Inert while signed out, offline, unconfigured or un-initialised: the
    //   tick is a flow read plus a `delay`.

    /** Set by [syncNow] when listening state moved during its pass. */
    @Volatile
    private var listeningPushUrgent: Boolean = false

    /** Starts the heartbeat once; safe to call from every `init`. */
    private fun startListeningPushPump() {
        if (pumpStarted) return
        pumpStarted = true
        pumpScope.launch { listeningPushPump() }
    }

    private suspend fun listeningPushPump() {
        var lastPushed: String? = null
        var wasPlaying = false
        var quickRetries = 0
        while (true) {
            var nextDelayMs = LISTENING_PUSH_INTERVAL_MS
            val urgent = listeningPushUrgent
            listeningPushUrgent = false
            try {
                val userId = AuthRepository.session.value?.userId
                val ctx = appContext
                // Null checks are inline (not folded into a `canPush` boolean)
                // so `userId`/`ctx` smart-cast into the pass below.
                if (userId != null &&
                    ctx != null &&
                    AppwriteConfig.isConfigured() &&
                    NetworkMonitor.isOnline.value &&
                    // A full pass is already walking every collection; skip
                    // rather than duplicate its work in parallel.
                    _status.value != SyncStatus.SYNCING
                ) {
                    val playing = AudioEngine.isPlaying.value
                    val fingerprint = listeningStateFingerprint()
                    val moved = fingerprint != lastPushed
                    // `wasPlaying` covers the pause/stop edge: the position
                    // freezes there, so that is the last tick that can still
                    // carry the final resume point to the cloud.
                    if (moved && (playing || wasPlaying)) {
                        if (pushListeningOnce(ctx, userId)) {
                            // Re-read AFTER the pass: if the state moved while
                            // we were pushing it, what is on the cloud is
                            // already stale, so come back quickly — bounded, so
                            // a device whose clock runs fast cannot turn this
                            // into an unbounded retry loop.
                            val after = listeningStateFingerprint()
                            lastPushed = after
                            quickRetries = if (after != fingerprint &&
                                quickRetries < LISTENING_MAX_QUICK_RETRIES
                            ) {
                                quickRetries + 1
                            } else {
                                0
                            }
                            if (quickRetries > 0) nextDelayMs = LISTENING_PUSH_RETRY_MS
                        } else {
                            // Leave `lastPushed` alone so the next tick retries.
                            Log.w(TAG, "listening heartbeat pass failed; retrying next tick.")
                        }
                    }
                    wasPlaying = playing
                } else {
                    // Signed out / offline / busy: forget the fingerprint so a
                    // returning session re-pushes instead of assuming the cloud
                    // already holds this state.
                    lastPushed = null
                    wasPlaying = false
                }
            } catch (e: Exception) {
                Log.w(TAG, "listening heartbeat tick failed", e)
            }
            if (urgent && !listeningPushUrgent) nextDelayMs = LISTENING_PUSH_RETRY_MS
            delay(nextDelayMs)
        }
    }

    /**
     * One pull+push pass over the three listening collections, sharing
     * [syncNow]'s per-collection fault isolation (a failure here must never
     * escalate: it only logs, and the next tick retries). Returns false when
     * the pass did not complete cleanly, so the caller can retry.
     */
    private suspend fun pushListeningOnce(
        ctx: android.content.Context,
        userId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        var clean = true
        val db = databases(ctx)
        suspend fun runCollection(name: String, block: suspend () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                clean = false
                if (isNotFound(e)) {
                    Log.w(TAG, "Collection '$name' not found; skipping.")
                } else {
                    Log.e(TAG, "$name heartbeat push failed", e)
                }
            }
        }
        runCollection(PLAYBACK) {
            pushPlayback(db, userId, pullPlaybackIfEmpty(db, userId))
        }
        runCollection(STATS) {
            pushStats(db, userId, pullStats(db, userId))
        }
        runCollection(HISTORY) {
            pushHistory(db, userId, pullHistory(db, userId))
        }
        clean
    }

    /**
     * Compact fingerprint of exactly the local state the three listening
     * pushes would upload: the player identity + position, the stats
     * aggregates, the per-day and per-surah series, and every history row's
     * identity/position/timestamp.
     *
     * Deliberately a plain `String` built from primitives and flow values (no
     * hashing of the maps beyond `hashCode`, no allocation per entry beyond
     * the builder): it runs on every tick of a loop that must stay free while
     * audio plays. `Map.hashCode()` is content-based, so a changed bucket
     * changes the fingerprint.
     */
    private fun listeningStateFingerprint(): String = buildString(160) {
        val track = AudioEngine.currentTrack.value
        append(track?.reciterSlug ?: "-").append('/').append(track?.surahId ?: 0)
        append('@').append(AudioEngine.currentPositionMs.value)
        val s = UserUsageRepository.stats.value
        append('|').append(s.totalSecondsListened).append(',').append(s.daysStreak)
        append(',').append(s.minutesToday).append(',').append(s.uniqueRecitersCount)
        append(',').append(s.uniqueSurahsCount)
        append("|d").append(UserUsageRepository.dailySeconds.value.hashCode())
        append("|p").append(UserUsageRepository.surahPlays.value.hashCode())
        for (item in UserUsageRepository.history.value) {
            append(';').append(item.reciterSlug).append('#').append(item.surahId)
            append('@').append(item.positionMs)
            append('/').append(item.lastPlayedTimestampMs)
        }
    }

    // ------------------------------------------------------------------ setup

    private fun databases(ctx: android.content.Context): Databases {
        // Fresh jar per sync so session cookies written by AuthRepository are
        // re-read from disk (two in-memory jars would otherwise drift).
        val client = Client()
            .setEndpoint(AppwriteConfig.ENDPOINT)
            .setProject(AppwriteConfig.PROJECT_ID)
            .apply {
                http = okhttp3.OkHttpClient.Builder()
                    .cookieJar(PersistentCookieJar(ctx))
                    .build()
            }
        return Databases(client)
    }

    private fun isNotFound(e: Exception): Boolean =
        e is AppwriteException && e.code == 404

    // ------------------------------------------------------------------ likes
    //
    // Cloud shape: `likes{user_id,surah_id,ayah_no,level,reciter_slug,track_json}`.
    // `track_json` is the whole [TrackItem] as `Json.encodeToString` — the same
    // encoding `history` uses (see [pushHistory]) and for the same reason: the
    // scalar columns cannot express a favourite. `textUthmani` (the Arabic a
    // favourites row displays) and `durationMs` (rendered as "unknown duration"
    // when 0) exist ONLY inside it, so before it was written a restore rebuilt
    // both from nothing. The columns stay authoritative for identity — the doc
    // id and the prune are keyed on (reciter, surah, ayah) — and `track_json`
    // supplies the payload for a doc whose (surah, ayah) matches.

    /**
     * Column size of `likes.track_json` (string 16384, optional). A [TrackItem]
     * is normally a few hundred characters, but a full-surah favourite can carry
     * a whole surah of `textUthmani`, and an over-long attribute is rejected as
     * a WHOLE document — so [likeTrackJson] drops the Arabic text rather than
     * let one pathological row cost every other favourite its write.
     */
    private const val LIKES_TRACK_JSON_MAX_CHARS = 16384

    /**
     * `likes` attributes the cloud may not have provisioned yet, in the order
     * they are shed when the server rejects a write as unknown-attribute (see
     * [pushLikes]). Both ARE in `appwrite/collections.json` today; the list is
     * kept for a deployment that predates them, because losing a reciter slug or
     * a track payload is recoverable while a rejected document loses the whole
     * favourites push with it.
     */
    private val LIKES_OPTIONAL_ATTRS = listOf("reciter_slug", "track_json")

    /**
     * Unions the cloud `likes` docs into [FavoritesStore] — it does NOT gate on
     * local emptiness any more (see the class KDoc: pre-auth likes are adopted
     * into a freshly-bound account, and one like tapped in the 2s before the
     * first pass is enough to hide the whole cloud set, which the paired
     * [pushLikes] would then delete).
     *
     * [FavoritesStore.clear] is still called, but only while the list is empty,
     * which is exactly when the store needs it: until `clear` has run, the
     * store's `add` diverts new entries into its pending adoption buffer, so a
     * restore into an empty store would be invisible — and a like tapped on an
     * empty list would never show. Calling it on a NON-empty list would be
     * actively harmful (it empties the list, so every local row would have to
     * be re-added, rewriting the whole list to disk on every pass), and it buys
     * nothing there: with a non-empty list `add` never diverts, so a plain
     * union below is equivalent.
     *
     * Cloud rows whose `likeKey` is already held by a playable local entry are
     * skipped: [FavoritesStore.add] would be a no-op for them anyway (it
     * upserts by the store's own identity — audio URL widened to the cloud
     * doc-id tuple, so a playable row also upgrades its leftover
     * `appwrite://likes/<docId>` placeholder), and skipping keeps the common
     * "cloud == local" pass from rewriting every like to disk.
     *
     * Returns true when the cloud holds at least one like document (restored or
     * not — the caller must then skip pushing an empty local list, which would
     * delete the account's whole cloud favourites set). False when the
     * collection is missing or empty, so push converges.
     */
    private suspend fun pullLikes(db: Databases, userId: String): Boolean {
        val docs = listForUser(db, LIKES, userId) ?: return false
        if (FavoritesStore.favoriteTracks.value.isEmpty()) {
            FavoritesStore.clear()
        }
        if (docs.isEmpty()) return false
        val known = mutableSetOf<String>()
        for (track in FavoritesStore.favoriteTracks.value) {
            if (!track.audioUrl.startsWith(LIKES_PLACEHOLDER_PREFIX)) {
                known += likeKey(track.surahId, track.ayahNo)
            }
        }
        var adopted = 0
        for (doc in docs) {
            val data = doc.data
            val surahId = (data["surah_id"] as? Number)?.toInt() ?: continue
            val ayahNo = (data["ayah_no"] as? Number)?.toInt() ?: 0
            // `reciter_slug` is optional server-side: older cloud docs (and a
            // collection where the sibling hasn't added the attribute yet) lack
            // it. When present and known, rebuild a FULLY playable TrackItem
            // (reciter display name + real audio URL via QuranDataRepository /
            // Reciter helpers, surah names via getSurahById). Otherwise keep the
            // synthetic placeholder so FavoritesStore's audioUrl-identity dedup
            // keeps working; such items play after the gap closes.
            val rawSlug = (data["reciter_slug"] as? String)?.trim().orEmpty()
            val key = likeKey(surahId, ayahNo)
            if (key in known) continue
            known += key
            // `track_json` first: it is the favourite as it was saved, so it
            // restores `textUthmani` and `durationMs` that the columns cannot
            // carry. A legacy document (or one written before the attribute was
            // provisioned) has none, and falls through to the same column
            // rebuild — and then to the same `appwrite://likes/<docId>`
            // placeholder — as before this attribute existed.
            val playable = decodeLikeTrack(data["track_json"], surahId, ayahNo)
                ?: buildPlayableLike(rawSlug, surahId, ayahNo)
            if (playable != null) {
                FavoritesStore.add(playable)
            } else {
                FavoritesStore.add(
                    TrackItem(
                        reciterSlug = rawSlug,
                        reciterName = "",
                        surahId = surahId,
                        surahNameEn = "",
                        surahNameAr = "",
                        ayahNo = ayahNo,
                        audioUrl = "$LIKES_PLACEHOLDER_PREFIX${doc.id}",
                    )
                )
            }
            adopted++
        }
        if (adopted > 0) {
            Log.i(
                TAG,
                "likes pull: unioned $adopted new cloud like(s) into " +
                    "${FavoritesStore.favoriteTracks.value.size} local (cloud had ${docs.size})."
            )
        }
        return true
    }

    /**
     * Rebuilds a fully playable liked [TrackItem] from cloud fields, or null
     * when the slug is blank/unknown or the surah is unknown (caller falls back
     * to the synthetic `appwrite://likes/<docId>` placeholder).
     *
     * Ayah likes (`ayahNo > 0`) resolve via [Reciter.getAyahAudioUrl] (per-ayah
     * EveryAyah MP3); full-surah likes via [Reciter.getFullSurahUrl]. The stored
     * slug is the canonical [Reciter.slug] so doc IDs stay stable.
     */
    private fun buildPlayableLike(slug: String, surahId: Int, ayahNo: Int): TrackItem? {
        if (slug.isBlank()) return null
        val reciter = findReciterOrNull(slug) ?: return null
        val surah = runCatching { QuranDataRepository.getSurahById(surahId) }.getOrNull()
            ?: return null
        val audioUrl = runCatching {
            if (ayahNo > 0) reciter.getAyahAudioUrl(surahId, ayahNo)
            else reciter.getFullSurahUrl(surahId)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = ayahNo,
            audioUrl = audioUrl,
        )
    }

    /**
     * `likes.track_json`: the local [TrackItem] verbatim as
     * `Json.encodeToString`, the encoding [pushHistory] already uses for
     * `history.track_json`. A placeholder row (one this device could not
     * resolve) serialises like any other; the pull recognises its URL and
     * rebuilds it, so a placeholder is never restored as a placeholder.
     *
     * Trims to the payload minus `textUthmani` when the full one does not fit
     * [LIKES_TRACK_JSON_MAX_CHARS], and to `""` if even that does not — a blank
     * payload is simply treated as absent by [decodeLikeTrack], which is the
     * right degradation: the reciter, surah and audio URL (what actually makes
     * the row playable) survive, and the Arabic text is the only thing lost.
     */
    private fun likeTrackJson(track: TrackItem): String {
        val full = runCatching { json.encodeToString(track) }.getOrNull() ?: return ""
        if (full.length <= LIKES_TRACK_JSON_MAX_CHARS) return full
        val trimmed = runCatching { json.encodeToString(track.copy(textUthmani = "")) }
            .getOrNull()
            ?: return ""
        return if (trimmed.length <= LIKES_TRACK_JSON_MAX_CHARS) trimmed else ""
    }

    /**
     * The [TrackItem] a cloud like's `track_json` carries, or null when the
     * document has no usable one — the caller then falls back to
     * [buildPlayableLike]'s column rebuild, and then to the `appwrite://likes/`
     * placeholder.
     *
     * A payload is refused (→ null, never thrown, so one bad document cannot
     * abort the pass) unless:
     * - it decodes at all;
     * - it AGREES with the document's own `surah_id` / `ayah_no`. Those columns
     *   are what the doc id and [pushLikes]' prune are keyed on, so a payload
     *   that disagrees with them would restore a favourite under an identity the
     *   cloud does not have, and the paired push would then delete its own doc;
     * - it names a reciter and carries a real audio URL — not blank, and not an
     *   `appwrite://likes/` placeholder — so a row whose reciter the catalog can
     *   no longer resolve still takes the identity-stable placeholder path.
     */
    private fun decodeLikeTrack(raw: Any?, surahId: Int, ayahNo: Int): TrackItem? {
        val payload = (raw as? String)?.trim().orEmpty()
        if (payload.isEmpty()) return null
        val track = try {
            json.decodeFromString<TrackItem>(payload)
        } catch (_: Exception) {
            return null
        }
        if (track.surahId != surahId || track.ayahNo != ayahNo) return null
        if (track.reciterSlug.isBlank() || track.audioUrl.isBlank()) return null
        if (track.audioUrl.startsWith(LIKES_PLACEHOLDER_PREFIX)) return null
        return track
    }

    /**
     * Null-returning twin of [QuranDataRepository.getReciterBySlug] (which falls
     * back to Alafasy and never returns null): mirrors its smart-normalization
     * predicate so "unknown" slugs are detectable and can use the synthetic
     * fallback instead of misattributing to the fallback reciter.
     */
    private fun findReciterOrNull(slug: String): Reciter? {
        val cleanSlug = slug.trim().lowercase()
        if (cleanSlug.isEmpty()) return null
        return runCatching { QuranDataRepository.getReciters() }.getOrNull()?.find { reciter ->
            val rSlug = reciter.slug.lowercase()
            rSlug == cleanSlug ||
                (cleanSlug == "mishary" && rSlug == "alafasy") ||
                (cleanSlug == "al-sudais" && rSlug == "sudais") ||
                (cleanSlug == "al-muaiqly" && rSlug == "muaiqly") ||
                (cleanSlug == "al-dossari" && rSlug == "dossari") ||
                (cleanSlug == "abdul-basit" && rSlug.startsWith("abdulbaset")) ||
                (cleanSlug == "abdulbasit" && rSlug.startsWith("abdulbaset")) ||
                (cleanSlug == "shuraim" && rSlug == "shuraym") ||
                (cleanSlug == "islam-sobhi" && rSlug.contains("islam")) ||
                rSlug.replace("_", "-") == cleanSlug ||
                rSlug.replace("-", "_") == cleanSlug ||
                reciter.nameEn.lowercase().contains(cleanSlug)
        }
    }

    /**
     * Converges the cloud `likes` set to the local favourites.
     *
     * Empty-local guard (same shape as [pushHistory] / [pushStats]): with local
     * favourites empty and the cloud holding any, the prune below would delete
     * the account's ENTIRE cloud favourites set and the upsert loop would
     * recreate none. That is the normal state of a fresh device whose restore
     * could not be applied, so the push is skipped and the pull's return value
     * decides.
     *
     * ## Prune invariant (the "conservative, never aggressive" rule)
     *
     * The doc id is [likeDocId] = `sha256("<userId>|<slug>|<surahId>|<ayahNo>")`,
     * so a doc is identified by the (reciter, surah, ayah) TRIPLE. The prune
     * used to key on `surahId:ayahNo` alone, which made one local row stand in
     * for every reciter's doc for that ayah: favouriting the same ayah from two
     * reciters produced two cloud docs that one local key covered, so
     * un-favouriting either deleted both and the next restore brought the other
     * back. The key now carries the slug — canonicalised through
     * [findReciterOrNull] on BOTH sides, so a device that stores the alias
     * `"mishary"` and a device that stores the canonical `"alafasy"` agree
     * instead of deleting each other's doc.
     *
     * The catch is that the local store is keyed by AUDIO URL, not by the doc
     * triple (`FavoritesStore.sameFavorite`), and two catalog rows for the same
     * person can resolve to the same file (the MP3Quran full-surah and
     * EveryAyah twins, and any two rows sharing a server URL), in which case
     * ONE local row is the only local representation of TWO cloud docs. A
     * strict triple match would therefore delete a like the user never
     * removed and that this device cannot even show separately. So a doc is
     * deleted only on POSITIVE PROOF that this device holds no like for it:
     * 1. a local row whose canonical (slug, surah, ayah) equals the doc's — keep;
     * 2. a local row playing the exact URL the doc's own (reciter, surah, ayah)
     *    would play (rebuilt with the same [buildPlayableLike] the pull uses) —
     *    keep: that row is this doc's like, whatever slug it happens to carry;
     * 3. a doc whose `reciter_slug` is absent or no longer in the catalog, so
     *    its identity is unknowable: keep whenever ANY local row likes that
     *    ayah, delete only when the user likes no reciter's version of it;
     * 4. a local row for the same ayah whose slug or audio URL is blank (a
     *    placeholder the pull could not resolve) — the row cannot be attributed,
     *    so it protects every doc for that ayah: keep.
     * Anything the device cannot place is kept; only a doc that is
     * positively unmatched is deleted. Cost of a wrong keep: an inert
     * document, re-checked (and eventually deleted, or restored as a
     * placeholder) on a later pass. Cost of a wrong delete: a lost like.
     */
    private suspend fun pushLikes(db: Databases, userId: String, cloudHasData: Boolean) {
        val local = FavoritesStore.favoriteTracks.value
        if (local.isEmpty()) {
            if (cloudHasData) {
                Log.i(TAG, "pushLikes skipped (local empty, cloud has data).")
            }
            return
        }
        val existing = listForUser(db, LIKES, userId) ?: return
        // Local index for the four keep-conditions above. Built once per pass
        // and only over the (small) local list; the per-doc work below is a set
        // lookup, plus a URL rebuild for the docs whose slug does not match.
        val localDocKeys = HashSet<String>(local.size * 2)
        val localAyahKeys = HashSet<String>(local.size * 2)
        val localAudioUrls = HashSet<String>(local.size * 2)
        val unattributableAyahKeys = HashSet<String>()
        for (track in local) {
            val ayahKey = likeKey(track.surahId, track.ayahNo)
            localDocKeys += likeDocKey(canonicalReciterSlug(track.reciterSlug), track.surahId, track.ayahNo)
            localAyahKeys += ayahKey
            if (track.audioUrl.isBlank() || track.reciterSlug.isBlank()) {
                unattributableAyahKeys += ayahKey
            } else {
                localAudioUrls += track.audioUrl
            }
        }
        // Per-pass set of attributes the cloud has rejected as unknown so far.
        // Grows only, at most one entry per rejection (see the upsert loop).
        var shed = emptySet<String>()
        for (doc in existing) {
            val data = doc.data
            val surahId = (data["surah_id"] as? Number)?.toInt() ?: continue
            val ayahNo = (data["ayah_no"] as? Number)?.toInt() ?: 0
            val ayahKey = likeKey(surahId, ayahNo)
            if (ayahKey in unattributableAyahKeys) continue
            val rawSlug = (data["reciter_slug"] as? String)?.trim().orEmpty()
            val docSlug = canonicalReciterSlug(rawSlug)
            val knownReciter = if (rawSlug.isEmpty()) null else findReciterOrNull(rawSlug)
            var keep = false
            if (knownReciter == null) {
                // Rule 3: identity unknowable — only a doc for an ayah the user
                // likes not at all can be called stale.
                keep = ayahKey in localAyahKeys
            } else {
                val playable = buildPlayableLike(docSlug, surahId, ayahNo)
                if (playable == null) {
                    // Rule 2 unavailable (surah gone from the catalog): nothing
                    // can be proven, so keep.
                    keep = true
                } else {
                    // Rules 1 and 2.
                    keep = likeDocKey(docSlug, surahId, ayahNo) in localDocKeys ||
                        playable.audioUrl in localAudioUrls
                }
            }
            if (keep) continue
            try {
                db.deleteDocument(DB_ID, LIKES, doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "delete stale like ${doc.id} failed", e)
            }
        }
        for (track in local) {
            val full: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "surah_id" to track.surahId,
                "ayah_no" to track.ayahNo,
                "level" to "like",
                "reciter_slug" to track.reciterSlug,
                "track_json" to likeTrackJson(track),
            )
            val docId = likeDocId(userId, track.reciterSlug, track.surahId, track.ayahNo)
            // `reciter_slug` (string 64) and `track_json` (string 16384) are both
            // optional server-side and both in `collections.json` today; when a
            // deployment has not provisioned one yet the server rejects the WHOLE
            // document as unknown-attribute, so shed it and keep syncing every
            // other field. The loop (not a single retry) matters: a deployment
            // can be missing BOTH, and one retry would give up on the second and
            // fail the whole collection block. It terminates because `shed` only
            // grows and is bounded by [LIKES_OPTIONAL_ATTRS]; anything that is
            // not an unknown-attribute rejection is rethrown untouched.
            var attempt = full.filterKeys { it !in shed }
            while (true) {
                try {
                    upsert(db, LIKES, docId, attempt)
                    break
                } catch (e: Exception) {
                    val attr = LIKES_OPTIONAL_ATTRS.firstOrNull {
                        it in attempt && isUnknownAttribute(e, it)
                    } ?: throw e
                    Log.w(TAG, "likes.$attr not provisioned yet; pushing without it.")
                    shed += attr
                    attempt = full.filterKeys { it !in shed }
                }
            }
        }
    }

    /**
     * True when [e] looks like an "unknown attribute" rejection for [attr]
     * (thrown by create/update when the cloud collection lacks the attribute).
     * Checked defensively across Appwrite SDK message wordings; callers only
     * pass attribute names they sent.
     */
    private fun isUnknownAttribute(e: Exception, attr: String): Boolean {
        val msg = (e.message ?: "").lowercase()
        if (!msg.contains(attr.lowercase())) return false
        return msg.contains("unknown") || msg.contains("invalid") || msg.contains("not found")
    }

    private fun likeKey(surahId: Int, ayahNo: Int): String = "$surahId:$ayahNo"

    /**
     * The (reciter, surah, ayah) identity [likeDocId] hashes, WITHOUT the user
     * id — i.e. the tuple a local row can be matched to a cloud doc on. Both
     * sides are passed through [canonicalReciterSlug] first (see [pushLikes]).
     */
    private fun likeDocKey(reciterSlug: String, surahId: Int, ayahNo: Int): String =
        "$reciterSlug|${likeKey(surahId, ayahNo)}"

    /**
     * The reciter's canonical [Reciter.slug] for [slug], falling back to the raw
     * lowercased spelling when the catalog no longer knows it.
     *
     * This is what makes the [pushLikes] prune key agree across devices: the
     * cloud `reciter_slug` is written from whichever alias the pushing device
     * had (`mishary` vs `alafasy`, `al-muiqly` vs `muaiqly`, see
     * [findReciterOrNull]), so a raw comparison would let each device prune the
     * other's live doc. The fallback keeps unknown slugs comparable with each
     * other instead of collapsing them all into one bucket.
     */
    private fun canonicalReciterSlug(slug: String): String =
        findReciterOrNull(slug)?.slug?.lowercase() ?: slug.trim().lowercase()

    // ---------------------------------------------------------------- follows

    /**
     * Unions the cloud `follows` slugs into [FollowStore] — no local-emptiness
     * gate any more (class KDoc: a reciter followed while signed out is adopted
     * into the new account, and one local follow was enough to hide the whole
     * cloud set, which the paired [pushFollows] then deleted).
     *
     * A follow is a bare slug with no payload to reconcile, so the union is
     * exact: the push's prune is keyed off the same slug set, which makes it a
     * no-op after this pull instead of a delete-everything-the-cloud-has pass.
     * [FollowStore.setAll] is only called when the cloud actually adds a slug,
     * so a converged pass writes nothing.
     */
    private suspend fun pullFollows(db: Databases, userId: String) {
        val docs = listForUser(db, FOLLOWS, userId) ?: return
        if (docs.isEmpty()) return
        // Bulk merge, no per-slug count refresh (syncNow refreshes once).
        val local = FollowStore.followedSlugs.value
        val merged = local.toMutableSet()
        for (doc in docs) {
            val slug = (doc.data["reciter_slug"] as? String)?.trim().orEmpty()
            if (slug.isNotEmpty()) merged += slug
        }
        if (merged.size == local.size) return
        val added = merged.size - local.size
        FollowStore.setAll(merged)
        Log.i(TAG, "follows pull: unioned $added new cloud slug(s) into ${merged.size} local.")
    }

    private suspend fun pushFollows(db: Databases, userId: String) {
        val local = FollowStore.followedSlugs.value
        val existing = listForUser(db, FOLLOWS, userId) ?: return
        for (doc in existing) {
            val slug = doc.data["reciter_slug"] as? String ?: continue
            if (slug !in local) {
                try {
                    db.deleteDocument(DB_ID, FOLLOWS, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "delete stale follow ${doc.id} failed", e)
                }
            }
        }
        for (slug in local) {
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "reciter_slug" to slug,
            )
            upsert(db, FOLLOWS, followDocId(userId, slug), data)
        }
    }

    // ------------------------------------------------------- reciter stats
    //
    // Public read-only counters (`reciter_stats{slug,followers_count,
    // likes_count,updated_at}`) with a `slug` unique index. Read as ONE batched
    // `listDocuments` (never an N× per-slug fan-out, never a per-slug
    // `getDocument` that 404s and then retries) resolved into a slug → counts
    // map cached for [RECITER_STATS_TTL_MS], so `FollowStore.refreshCounts`
    // costs a single request per TTL window no matter how many reciters the
    // user follows. Absent slug = unknown = hidden in the UI. The client never
    // writes these docs, so a fresh account sees no counts at all.

    /** How long one batched `reciter_stats` list is reused before re-reading. */
    private const val RECITER_STATS_TTL_MS = 5L * 60 * 1000L

    /**
     * Per-request cap on the batched `reciter_stats` list. Counters are
     * cosmetic (one doc per catalog reciter) and the client never writes them,
     * so a full page is the whole collection in practice; hitting the cap is
     * logged rather than silently truncating.
     */
    private const val RECITER_STATS_MAX_DOCS = 200

    /** Batched counters by slug → field → value; empty until the first read. */
    @Volatile
    private var reciterStatsCache: Map<String, Map<String, Any?>> = emptyMap()

    /** Epoch-millis of the last batched read; 0 = never read yet. */
    @Volatile
    private var reciterStatsReadAtMs: Long = 0L

    /**
     * Reads `followers_count` for [reciterSlug] from the batched
     * `reciter_stats` snapshot. Null when the slug has no doc (or the
     * collection is missing/unreadable), i.e. "unknown".
     */
    suspend fun followerCount(reciterSlug: String): Long? =
        reciterStat(reciterSlug, "followers_count")

    /** Reads `likes_count` for [reciterSlug] from the batched `reciter_stats` snapshot. */
    suspend fun likesCount(reciterSlug: String): Long? =
        reciterStat(reciterSlug, "likes_count")

    private suspend fun reciterStat(reciterSlug: String, field: String): Long? =
        withContext(Dispatchers.IO) {
            try {
                val ctx = appContext ?: return@withContext null
                if (!AppwriteConfig.isConfigured()) return@withContext null
                val slug = reciterSlug.trim()
                if (slug.isEmpty()) return@withContext null
                val db = databases(ctx)
                (reciterStatsSnapshot(db)[slug]?.get(field) as? Number)?.toLong()
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Batched, TTL-cached `reciter_stats` read resolved into
     * `slug → (field → value)`. One `listDocuments` serves every caller for
     * [RECITER_STATS_TTL_MS].
     *
     * 404 is a legitimate "absent" answer — the collection is provisioned by
     * hand and may not exist yet — so it short-circuits to an empty, CACHED
     * snapshot instead of falling through to a second request. That empty
     * result is the normal state for an unprovisioned collection, and caching
     * it is what keeps a sync pass at one `reciter_stats` request instead of
     * two per followed reciter. Never throws; on an unexpected failure the
     * previous snapshot is kept.
     */
    private suspend fun reciterStatsSnapshot(
        db: Databases,
    ): Map<String, Map<String, Any?>> {
        val now = System.currentTimeMillis()
        val cached = reciterStatsCache
        if (reciterStatsReadAtMs > 0L && now - reciterStatsReadAtMs < RECITER_STATS_TTL_MS) {
            return cached
        }
        val fresh = try {
            val docs = db.listDocuments(
                DB_ID,
                RECITER_STATS,
                listOf(Query.limit(RECITER_STATS_MAX_DOCS)),
            ).documents
            if (docs.size >= RECITER_STATS_MAX_DOCS) {
                Log.w(TAG, "reciter_stats list hit the $RECITER_STATS_MAX_DOCS cap; counts may be partial.")
            }
            val out = HashMap<String, Map<String, Any?>>(docs.size * 2)
            for (doc in docs) {
                // `slug` is the unique-indexed key; the doc id is not part of
                // the contract, so key off the attribute and fall back to the
                // doc id only when the attribute is missing.
                val slug = (doc.data["slug"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: doc.id
                out[slug] = doc.data
            }
            out
        } catch (e: Exception) {
            if (isNotFound(e)) {
                Log.w(TAG, "Collection '$RECITER_STATS' not found; follower counts stay unknown.")
                emptyMap()
            } else {
                Log.w(TAG, "reciter_stats list failed", e)
                return cached
            }
        }
        reciterStatsCache = fresh
        reciterStatsReadAtMs = System.currentTimeMillis()
        return fresh
    }

    // ------------------------------------------------------- reciter catalog
    //
    // Appwrite `reciters` is the source of truth (keyed by
    // `catalog_key = "<catalog>:<slug>"` so MP3Quran and EveryAyah variants
    // never collide; `image_url` editable per reciter from admin panel).
    // Bulk refresh populates [ReciterCloudCache]; bundled seeds stay the
    // offline fallback. Never throws.

    /**
     * Pulls all `reciters` docs (paginated) into [ReciterCloudCache].
     * Skips disabled docs and malformed rows. Never throws.
     */
    suspend fun refreshReciterCatalog(db: Databases) {
        try {
            val out = fetchReciterCatalog(db)
            if (out.isNotEmpty()) {
                com.ghais.data.repository.ReciterCloudCache.setCloudReciters(out)
            }
        } catch (e: Exception) {
            Log.w(TAG, "reciter catalog refresh failed", e)
        }
    }

    /**
     * Public-read reciter catalog pull (Appwrite `reciters`, see
     * `collections.json`). Split out of [refreshReciterCatalog] so the
     * periodic, session-free refresh in `SyncTriggers` can reuse it without
     * running a full user sync. Returns an empty list on any failure, which
     * callers treat as "keep the current cache".
     */
    suspend fun fetchReciterCatalog(db: Databases): List<Reciter> {
        return try {
            val out = ArrayList<Reciter>(300)
            var offset = 0
            while (true) {
                val page = try {
                    db.listDocuments(
                        DB_ID,
                        RECITERS,
                        listOf(Query.limit(100), Query.offset(offset)),
                    ).documents
                } catch (e: Exception) {
                    if (!isNotFound(e)) Log.w(TAG, "reciters list failed", e)
                    break
                }
                if (page.isEmpty()) break
                for (doc in page) {
                    parseReciterDoc(doc.data)?.let { out.add(it) }
                }
                if (page.size < 100) break
                offset += page.size
            }
            out
        } catch (e: Exception) {
            Log.w(TAG, "reciter catalog fetch failed", e)
            emptyList()
        }
    }

    private fun parseReciterDoc(data: Map<String, Any?>): Reciter? {
        return try {
            val slug = (data["slug"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val enabled = data["enabled"] as? Boolean ?: true
            if (!enabled) return null
            // Catalog fallback chain: explicit `catalog` first, then the
            // `catalog_key = "<catalog>:<slug>"` prefix (legacy docs predate
            // the catalog attribute), else MP3QURAN via catalogOf default.
            val catalogRaw = (data["catalog"] as? String)?.takeIf { it.isNotBlank() }
                ?: (data["catalog_key"] as? String)
                    ?.substringBefore(':')
                    ?.takeIf { it.isNotBlank() }
            // `available_surahs` is a comma-separated string per collections.json,
            // but tolerate list/number shapes so one odd doc never drops a row.
            val availableSurahs = when (val v = data["available_surahs"]) {
                is String -> v
                is List<*> -> v.joinToString(",") { it.toString() }
                is Number -> v.toInt().toString()
                else -> ""
            }
            Reciter(
                slug = slug,
                nameEn = (data["name_en"] as? String)?.takeIf { it.isNotBlank() } ?: slug,
                nameAr = (data["name_ar"] as? String) ?: "",
                riwayah = (data["riwayah"] as? String)?.takeIf { it.isNotBlank() } ?: "Hafs",
                style = (data["style"] as? String)?.takeIf { it.isNotBlank() } ?: "murattal",
                tempo = (data["tempo"] as? String)?.takeIf { it.isNotBlank() } ?: "medium",
                imageUrl = (data["image_url"] as? String)?.takeIf { it.isNotBlank() },
                audioFolder = (data["audio_folder"] as? String) ?: "",
                country = (data["country"] as? String) ?: "",
                serverUrl = (data["server_url"] as? String) ?: "",
                availableSurahList = availableSurahs,
                surahFileMap = (data["full_surah_file_map"] as? String) ?: "",
                catalog = com.ghais.data.repository.ReciterCloudCache.catalogOf(catalogRaw),
                description = (data["description"] as? String) ?: "",
                isTeacher = (data["is_teacher"] as? Boolean) ?: false,
                imageFileId = (data["image_file_id"] as? String)?.takeIf { it.isNotBlank() },
                enabled = true,
            )
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------- reciter photos
    //
    // Cloud-hosted reciter portraits (`reciters` collection, per-reciter
    // `image_url` editable from the admin panel, public read-only). The
    // sibling provisions the collection in parallel, so 404/timeout/offline
    // all degrade to null (local fallback) and never throw.

    /**
     * Reads `image_url` for [reciterSlug] from the `reciters` collection
     * (`slug`-equal query, limit 1, `image_url` select). Returns the
     * non-blank URL or null on ANY failure — never throws.
     */
    suspend fun reciterImage(reciterSlug: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val ctx = appContext ?: return@withContext null
                if (!AppwriteConfig.isConfigured()) return@withContext null
                val slug = reciterSlug.trim()
                if (slug.isEmpty()) return@withContext null
                val db = databases(ctx)
                val data: Map<String, Any?>? = try {
                    db.listDocuments(
                        DB_ID,
                        RECITERS,
                        listOf(
                            Query.equal("slug", slug),
                            Query.limit(1),
                            Query.select(listOf("image_url")),
                        ),
                    ).documents.firstOrNull()?.data
                } catch (e: Exception) {
                    if (!isNotFound(e)) {
                        Log.w(TAG, "reciters image_url select query '$slug' failed; trying plain query", e)
                    }
                    try {
                        db.listDocuments(
                            DB_ID,
                            RECITERS,
                            listOf(Query.equal("slug", slug), Query.limit(1)),
                        ).documents.firstOrNull()?.data
                    } catch (e2: Exception) {
                        if (!isNotFound(e2)) {
                            Log.w(TAG, "reciters query '$slug' failed", e2)
                        }
                        null
                    }
                }
                (data?.get("image_url") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }

    // --------------------------------------------------------------- broadcasts
    //
    // Public announcements (`broadcasts{title,body,audience,urgency}`,
    // PUBLIC_READ in DB `ghais`). Single newest-first pull (order
    // `$createdAt` desc, limit 50) into BroadcastRepository; the client never
    // writes. Needs no session — only AppwriteConfig + init context — so the
    // inbox pull also works for signed-out users. Never throws.

    /**
     * Pulls all `broadcasts` docs newest-first (cap 50) as [Broadcast] rows.
     * Skips docs with blank title/body. Returns empty on ANY failure.
     */
    suspend fun fetchBroadcasts(): List<Broadcast> = withContext(Dispatchers.IO) {
        try {
            val ctx = appContext ?: return@withContext emptyList()
            if (!AppwriteConfig.isConfigured()) return@withContext emptyList()
            val db = databases(ctx)
            val docs = try {
                db.listDocuments(
                    DB_ID,
                    BROADCASTS,
                    listOf(Query.orderDesc("\$createdAt"), Query.limit(50)),
                ).documents
            } catch (e: Exception) {
                if (!isNotFound(e)) Log.w(TAG, "broadcasts list failed", e)
                return@withContext emptyList()
            }
            docs.mapNotNull { doc ->
                try {
                    val data = doc.data
                    val title = (data["title"] as? String)?.trim()
                        ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    val body = (data["body"] as? String)?.trim()
                        ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    Broadcast(
                        id = doc.id,
                        title = title,
                        body = body,
                        audience = (data["audience"] as? String)?.trim()
                            ?.takeIf { it.isNotEmpty() } ?: "all",
                        urgency = (data["urgency"] as? String)?.trim()
                            ?.takeIf { it.isNotEmpty() } ?: "Normal",
                        createdAt = doc.createdAt,
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --------------------------------------------------------------- playback
    //
    // Pull-if-empty + push-always (push skips when local is still empty but
    // cloud has data, so a fresh login never clobbers its restore source with
    // an empty snapshot).

    /**
     * Pulls the cloud `playback_state` doc (docId = [userId]) when the local
     * player is empty, and silently preloads it PAUSED via
     * [AudioEngine.prepareTrack] (load + seek, NO autoplay — the user presses
     * play to start, and [AudioEngine.resume] continues from the preloaded
     * position instead of restarting from 0).
     *
     * Returns true when the cloud holds a usable snapshot (caller must then
     * skip pushing, otherwise the still-empty local state would overwrite
     * it). Returns false when there is nothing to protect (local already
     * playing, or no/empty cloud doc) so push proceeds.
     *
     * Parsing: `current_ref` is `"<slug>/<surahId>"`; `queue_json` is the
     * single-track JSON array the pusher writes
     * (`[{reciter_slug,surah_id,ayah_no,audio_url}]`) and is preferred because
     * it also carries `ayah_no`. Names/duration resolve via
     * [QuranDataRepository.getSurahById] + [Reciter.getAyahAudioUrl] /
     * [Reciter.getFullSurahUrl] (duration stays unknown/0 until streaming).
     */
    private suspend fun pullPlaybackIfEmpty(db: Databases, userId: String): Boolean {
        if (AudioEngine.currentTrack.value != null) return false
        val doc = try {
            db.getDocument(DB_ID, PLAYBACK, userId)
        } catch (_: Exception) {
            null
        } ?: return false
        val data = doc.data
        val ref = (data["current_ref"] as? String)?.trim().orEmpty()
        if (ref.isEmpty()) return false // idle snapshot the pusher wrote; nothing to restore
        val positionMs = (data["position_ms"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
        val updatedAt = (data["updated_at"] as? Number)?.toLong()
        val queued = parseQueueFirst((data["queue_json"] as? String).orEmpty())
        val refParts = parseCurrentRef(ref)
        val slug = queued?.slug ?: refParts?.first ?: return true
        val surahId = queued?.surahId ?: refParts?.second ?: return true
        val ayahNo = queued?.ayahNo ?: 0
        val reciter = findReciterOrNull(slug)
        if (reciter == null) {
            Log.w(TAG, "playback pull: unknown reciter slug '$slug'; keeping cloud snapshot.")
            return true
        }
        val surah = runCatching { QuranDataRepository.getSurahById(surahId) }.getOrNull()
        if (surah == null) {
            Log.w(TAG, "playback pull: unknown surah $surahId; keeping cloud snapshot.")
            return true
        }
        var audioUrl = queued?.audioUrl?.takeIf { it.isNotBlank() }.orEmpty()
        if (audioUrl.isBlank()) {
            audioUrl = runCatching {
                if (ayahNo > 0) reciter.getAyahAudioUrl(surahId, ayahNo)
                else reciter.getFullSurahUrl(surahId)
            }.getOrNull().orEmpty()
        }
        if (audioUrl.isBlank()) return true
        val track = TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = ayahNo,
            audioUrl = audioUrl,
        )
        // Silent preload: load + seek, stays paused, NO autoplay.
        Log.i(
            TAG,
            "playback pull: restoring '${reciter.slug}/$surahId' ayah=$ayahNo pos=${positionMs}ms" +
                (if (updatedAt != null) " updatedAt=$updatedAt" else "") +
                " — preloading paused (no autoplay)."
        )
        AudioEngine.prepareTrack(track, positionMs)
        return true
    }

    private suspend fun pushPlayback(db: Databases, userId: String, cloudHasData: Boolean) {
        val track = AudioEngine.currentTrack.value
        if (track == null && cloudHasData) {
            // Fresh login with metadata-only restore: the local player is still
            // empty, so pushing would overwrite the cloud snapshot with ""/"[]".
            Log.i(TAG, "pushPlayback skipped (local empty, cloud has data).")
            return
        }
        val positionMs = AudioEngine.currentPositionMs.value
        val ref = if (track != null) "${track.reciterSlug}/${track.surahId}" else ""
        val queueJson = if (track != null) {
            """[{"reciter_slug":${jsonString(track.reciterSlug)},"surah_id":${track.surahId},"ayah_no":${track.ayahNo},"audio_url":${jsonString(track.audioUrl)}}]"""
        } else {
            "[]"
        }
        val data: Map<String, Any?> = mapOf(
            "user_id" to userId,
            "current_ref" to ref,
            "position_ms" to positionMs,
            "queue_json" to queueJson,
            "updated_at" to System.currentTimeMillis(),
        )
        // Single doc per user, id = userId (matches user_state_unique index).
        upsert(db, PLAYBACK, userId, data)
    }

    /** First entry of the pusher-written single-track `queue_json` array, or null. */
    private fun parseQueueFirst(queueJson: String): QueuedTrack? {
        if (queueJson.isBlank()) return null
        return try {
            val obj = json.parseToJsonElement(queueJson).jsonArray.firstOrNull()?.jsonObject
                ?: return null
            val slug = obj["reciter_slug"]?.jsonPrimitive?.content?.trim().orEmpty()
            val surahId = obj["surah_id"]?.jsonPrimitive?.intOrNull ?: return null
            if (slug.isEmpty()) return null
            QueuedTrack(
                slug = slug,
                surahId = surahId,
                ayahNo = obj["ayah_no"]?.jsonPrimitive?.intOrNull ?: 0,
                audioUrl = obj["audio_url"]?.jsonPrimitive?.content.orEmpty(),
            )
        } catch (_: Exception) {
            null
        }
    }

    private data class QueuedTrack(
        val slug: String,
        val surahId: Int,
        val ayahNo: Int,
        val audioUrl: String,
    )

    /** Splits `"<slug>/<surahId>"` on the last `/` (slugs never contain `/`). */
    private fun parseCurrentRef(ref: String): Pair<String, Int>? {
        val idx = ref.lastIndexOf('/')
        if (idx <= 0 || idx >= ref.length - 1) return null
        val slug = ref.substring(0, idx).trim()
        val surahId = ref.substring(idx + 1).trim().toIntOrNull() ?: return null
        if (slug.isEmpty() || surahId <= 0) return null
        return slug to surahId
    }

    // ------------------------------------------------------------------ stats
    //
    // Daily series: `listening_stats.daily_json` (string 8192, optional) carries
    // the per-day buckets as compact JSON `{"<epochDay>":seconds,...}`, capped
    // at [DAILY_JSON_CAP] newest days so offline listening uploads on reconnect
    // and range charts restore on a fresh device. Merge is per-day max(), never
    // sum: there is no per-device tracking, so a day present on both sides
    // usually counts the SAME listening on each side — summing would
    // double-count multi-device same-day listening, while max adopts the fuller
    // side without ever shrinking local buckets.
    //
    // v2 push: `daily_json` is written per-device as
    // `{"<epochDay>":{"<devId>":seconds,...}}` ([UserUsageRepository.deviceId]),
    // merged-then-written (cloud doc is read first; every device entry is kept
    // and only this device's own entry is overwritten) so concurrent devices
    // stop clobbering each other.
    //
    // `surah_plays` (all-time per-surah play counts) rides along as a
    // `{"<surahId>":plays}` object string; it has no per-device split, so the
    // pull maxes it per surah the same way the daily buckets are maxed.
    //
    // Pull is a MERGE (per-field `max` for the aggregates, per-day `max` for the
    // series) rather than an adopt-only-when-local-is-empty — see [pullStats].

    /** Newest days carried in `daily_json` (count ceiling; the 8192-char cap binds first in v2). */
    private const val DAILY_JSON_CAP = 150

    /**
     * Hard ceiling for the `daily_json` string attribute. The v2 encoder drops
     * oldest days first until the payload measures under this.
     */
    private const val DAILY_JSON_MAX_CHARS = 8192

    /**
     * Stats attributes that may not be provisioned on the deployment being
     * talked to, in the order they are shed when the server rejects a write as
     * unknown-attribute (see [upsertStatsPayload]).
     *
     * BOTH are in `appwrite/collections.json` for `listening_stats` today
     * (`daily_json` string 8192, `surah_plays` string 2048, both optional), so
     * on a freshly provisioned database neither is ever shed. The list is kept
     * anyway, and this is the reason to keep it: `collections.json` describes
     * what a NEW database looks like, and a database that was provisioned before
     * those two attributes were added still rejects any document carrying them.
     * The failure it prevents is silent and total — the server rejects the WHOLE
     * stats document, so an unshed `daily_json` would cost the user their
     * lifetime total, streak and daily series, not just the daily series.
     */
    private val STATS_OPTIONAL_ATTRS = listOf("surah_plays", "daily_json")

    /**
     * Pulls the cloud `listening_stats` doc (docId = [userId]) and MERGES it
     * into local stats — it no longer requires local stats to be at fresh-install
     * defaults (see the class KDoc: a second of listening before the first pass
     * was enough to hide the account's whole lifetime total, streak and daily
     * series forever).
     *
     * Merge semantics, all never-shrinking:
     * - aggregates: per-field `max(local, cloud)`. The cloud row is the account
     *   total across devices, so `max` is the only rule that can add what
     *   another device earned without ever discarding what this one has (a
     *   stale cloud row can only be behind, never ahead). `restoreStats` takes
     *   the larger of each aggregate internally too, so this is belt and
     *   braces — the reason it is done here is `minutes_today`, which
     *   `restoreStats` REPLACES (it is a today-scoped value, not a running
     *   total), and which must therefore be passed in already maxed.
     * - `minutes_today` date semantics: adopted only when cloud `updated_at`
     *   falls on the current epoch day, exactly as before; otherwise the local
     *   value is kept. The merged snapshot is then stamped with `now` rather
     *   than the cloud's `updated_at`, because `restoreStats` zeroes
     *   `secondsToday` whenever the stamp it is given is not from today — a
     *   stale cloud stamp would wipe a live local today-minutes value.
     * - `daily_json` (when present) is parsed in BOTH shapes — v2 per-device
     *   `{"day":{"dev":secs}}` and legacy flat `{"day":secs}` (legacy counts as
     *   other-device `"legacy"` seconds). The own-device slice merges per-day
     *   with max() against local daily buckets via
     *   [UserUsageRepository.restoreDaily] (local buckets stay this-device-only);
     *   all other devices' seconds are summed per day (excluding this device's
     *   id) into the memory-only [UserUsageRepository.remoteDailySeconds] — so
     *   offline listening accumulated on another device restores here and
     *   converges on next push.
     * - `surah_plays` (when present) is decoded and handed to
     *   [UserUsageRepository.restoreSurahPlays], which per-surah maxes it
     *   against the local counts.
     *
     * `restoreStats` is only called when some aggregate would actually GROW, so
     * a converged pass costs no settings writes; the daily / surah-play merges
     * are already no-ops inside the store when nothing changed.
     *
     * Returns true when the cloud holds a usable snapshot (caller must then
     * skip pushing when local is still empty, otherwise local zeros would
     * overwrite it). Returns false when the cloud has nothing, so push
     * converges.
     */
    private suspend fun pullStats(db: Databases, userId: String): Boolean {
        val doc = getDocument(db, STATS, userId) ?: return false
        val data = doc.data
        val total = asLong(data["total_seconds"]) ?: 0L
        val streak = asInt(data["days_streak"]) ?: 0
        val uniquesReciters = asInt(data["unique_reciters"]) ?: 0
        val uniquesSurahs = asInt(data["unique_surahs"]) ?: 0
        val updatedAt = asLong(data["updated_at"])
        var minutes = asInt(data["minutes_today"]) ?: 0
        if (updatedAt == null || !sameEpochDay(updatedAt, System.currentTimeMillis())) {
            minutes = 0
        }
        val cloudDailyPerDevice =
            parseDailyJsonPerDevice((data["daily_json"] as? String).orEmpty())
        val ownId = UserUsageRepository.deviceId()
        val (ownDaily, remoteOthers) = splitOwnAndRemote(cloudDailyPerDevice, ownId)
        // Memory-only others-side total, recomputed every pull (never persisted,
        // never merged into local buckets). Always set — even empty — so a
        // pull with no cloud daily data clears a stale map.
        UserUsageRepository.setRemoteDailySeconds(remoteOthers)
        val cloudPlays = decodeSurahPlays((data["surah_plays"] as? String).orEmpty())
        if (total <= 0L && streak <= 0 && minutes <= 0 && uniquesReciters <= 0 &&
            uniquesSurahs <= 0 && ownDaily.isEmpty() && remoteOthers.isEmpty() &&
            cloudPlays.isEmpty()
        ) {
            return false // cloud snapshot itself is empty; let push converge
        }
        // Daily buckets merge (per-day max of the OWN device slice only — see
        // section header) before the aggregate restore, so a fresh login adopts
        // its own cloud series; other devices' seconds stay in
        // remoteDailySeconds only and never enter local buckets. A later push
        // then re-uploads the merged map and the series converges.
        val mergedDaily = mergeDailyMax(UserUsageRepository.dailySeconds.value, ownDaily)
        if (mergedDaily.isNotEmpty()) {
            UserUsageRepository.restoreDaily(mergedDaily)
        }
        // Per-surah play counts: per-surah max() inside the store, same
        // never-shrink rule as the daily buckets.
        if (cloudPlays.isNotEmpty()) {
            UserUsageRepository.restoreSurahPlays(cloudPlays)
        }
        val local = UserUsageRepository.stats.value
        val merged = StatsSnapshot(
            daysStreak = maxOf(local.daysStreak, streak),
            minutesToday = maxOf(local.minutesToday, minutes),
            uniqueRecitersCount = maxOf(local.uniqueRecitersCount, uniquesReciters),
            uniqueSurahsCount = maxOf(local.uniqueSurahsCount, uniquesSurahs),
            totalSecondsListened = maxOf(local.totalSecondsListened, total),
            updatedAtMs = System.currentTimeMillis(),
        )
        val grew = merged.daysStreak > local.daysStreak ||
            merged.minutesToday > local.minutesToday ||
            merged.uniqueRecitersCount > local.uniqueRecitersCount ||
            merged.uniqueSurahsCount > local.uniqueSurahsCount ||
            merged.totalSecondsListened > local.totalSecondsListened
        if (grew) {
            UserUsageRepository.restoreStats(merged)
            Log.i(
                TAG,
                "stats pull: merged cloud into local — total=${merged.totalSecondsListened}s " +
                    "(cloud $total) streak=${merged.daysStreak} minutesToday=${merged.minutesToday} " +
                    "reciters=${merged.uniqueRecitersCount} surahs=${merged.uniqueSurahsCount} " +
                    "days=${mergedDaily.size} surahPlayDays=${cloudPlays.size}."
            )
        } else {
            Log.i(
                TAG,
                "stats pull: cloud is behind local (cloud total=$total vs local " +
                    "${local.totalSecondsListened}); kept local. days=${mergedDaily.size} " +
                    "surahPlayDays=${cloudPlays.size}."
            )
        }
        return true
    }

    /**
     * Pushes stats with a per-device v2 `daily_json`
     * (`{"<epochDay>":{"<devId>":secs}}`, newest [DAILY_JSON_CAP] days, kept
     * under [DAILY_JSON_MAX_CHARS] chars by dropping oldest days first) plus the
     * all-time `surah_plays` object ([encodeSurahPlays]).
     *
     * Merge-then-write: the current cloud doc is read first and its device
     * maps are unioned (every device entry kept, only this device's own entry
     * overwritten with the local buckets) before upsert, which narrows the
     * multi-writer clobber window vs a blind overwrite. That read is also what
     * makes the heartbeat safe: it must never write a `daily_json` built from
     * an empty "cloud" side, or it would drop every OTHER device's days from
     * the collection.
     *
     * `surah_plays` has no per-device split, so it is written as this device
     * sees it; [UserUsageRepository.restoreSurahPlays] per-surah maxes it on the
     * way back in, which makes a two-device account converge on the higher
     * count rather than the sum (the same trade-off, and for the same reason, as
     * the daily buckets).
     *
     * Residual race (documented, accepted): read-modify-write is not atomic —
     * two devices pushing concurrently can interleave (both read, then last
     * writer wins), losing one push worth of the loser's own-device seconds.
     * Bounded damage: each writer only mutates its own devId key, so only the
     * concurrent update is lost (never another device's history), and the next
     * push re-uploads the full local buckets, self-healing on the following
     * sync. The heartbeat does not widen this window: it skips its tick while a
     * full pass is in flight, and the two only overlap if a full pass starts
     * mid-heartbeat-pass, in which case they write the same local values.
     */
    private suspend fun pushStats(db: Databases, userId: String, cloudHasData: Boolean) {
        if (cloudHasData && isLocalStatsEmpty()) {
            // Fresh login whose cloud snapshot couldn't be applied locally yet:
            // don't overwrite it with zeros.
            Log.i(TAG, "pushStats skipped (local empty, cloud has data).")
            return
        }
        val s = UserUsageRepository.stats.value
        val devId = UserUsageRepository.deviceId()
        val dailyJson = encodeDailyJsonV2(
            cloud = readCloudDailyPerDevice(db, userId),
            devId = devId,
            local = UserUsageRepository.dailySeconds.value,
        )
        val full: Map<String, Any?> = mapOf(
            "user_id" to userId,
            "days_streak" to s.daysStreak,
            "minutes_today" to s.minutesToday,
            "unique_reciters" to s.uniqueRecitersCount,
            "unique_surahs" to s.uniqueSurahsCount,
            "total_seconds" to s.totalSecondsListened,
            "updated_at" to System.currentTimeMillis(),
            "daily_json" to dailyJson,
            "surah_plays" to encodeSurahPlays(UserUsageRepository.surahPlays.value),
        )
        // `daily_json` (string 8192, optional) and `surah_plays` (string 2048,
        // optional) are sent when the cloud collection has them; when an
        // attribute isn't provisioned yet the server rejects the WHOLE write as
        // unknown-attribute, so drop them one at a time (per pass) and keep the
        // other fields syncing rather than losing the entire stats push.
        upsertStatsPayload(db, userId, full, STATS_OPTIONAL_ATTRS)
    }

    /**
     * Upserts the single-doc-per-user stats payload, shedding optional
     * attributes the cloud has not provisioned yet.
     *
     * [droppable] is consumed in order, at most one per attempt: the server
     * rejects the whole document for the first unprovisioned attribute it meets,
     * so one retry per attribute is enough (depth is bounded by its size). Any
     * other failure is rethrown into the caller's `runCollection` guard.
     */
    private suspend fun upsertStatsPayload(
        db: Databases,
        userId: String,
        payload: Map<String, Any?>,
        droppable: List<String>,
    ) {
        try {
            upsert(db, STATS, userId, payload)
        } catch (e: Exception) {
            val attr = droppable.firstOrNull { isUnknownAttribute(e, it) } ?: throw e
            Log.w(TAG, "stats.$attr not provisioned yet; pushing without it.")
            upsertStatsPayload(db, userId, payload - attr, droppable - attr)
        }
    }

    /**
     * `listening_stats.surah_plays`: per-surah all-time play counts as a JSON
     * object string, `{"18":7,"36":2}` — keys as strings (JSON object keys
     * always are) and values as plain integers, the same encoder convention as
     * `daily_json`. Mirrors [UserUsageRepository.surahPlays] exactly; the
     * decoder [decodeSurahPlays] is lenient about quoted numbers.
     */
    private fun encodeSurahPlays(plays: Map<Int, Long>): String {
        if (plays.isEmpty()) return "{}"
        return try {
            buildString(plays.size * 12 + 2) {
                append('{')
                var first = true
                for ((surahId, count) in plays) {
                    if (surahId !in 1..114 || count <= 0L) continue
                    if (!first) append(',')
                    first = false
                    append('"').append(surahId).append("\":").append(count)
                }
                append('}')
            }
        } catch (_: Exception) {
            "{}"
        }
    }

    /**
     * Lenient inverse of [encodeSurahPlays]: out-of-range surah ids, blank keys,
     * non-numeric and non-positive counts and malformed entries are all
     * skipped. Missing/blank/malformed input yields an empty map. Never throws.
     */
    private fun decodeSurahPlays(raw: String): Map<Int, Long> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val out = LinkedHashMap<Int, Long>(obj.size)
            for ((key, value) in obj) {
                val surahId = key.trim().toIntOrNull() ?: continue
                if (surahId !in 1..114) continue
                val prim = try {
                    value.jsonPrimitive
                } catch (_: Exception) {
                    null
                } ?: continue
                val count = prim.longOrNull
                    ?: prim.content.toDoubleOrNull()?.toLong()
                    ?: continue
                if (count <= 0L) continue
                out[surahId] = count
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Best-effort read of the cloud `daily_json` for merge-then-write
     * ([pushStats]): parses BOTH the v2 per-device shape and the legacy flat
     * shape via [parseDailyJsonPerDevice] (legacy days survive under the
     * `"legacy"` device key). Returns empty on missing doc/collection or ANY
     * read failure so a failed read degrades to a local-only write instead of
     * blocking the push. Never throws.
     */
    private suspend fun readCloudDailyPerDevice(
        db: Databases,
        userId: String,
    ): Map<Long, Map<String, Long>> {
        return try {
            val raw = getDocument(db, STATS, userId)?.data?.get("daily_json") as? String
            parseDailyJsonPerDevice(raw.orEmpty())
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Per-device daily-series JSON: `{"<epochDay>":{"<devId>":seconds,...}}`,
     * oldest day first on the wire, capped at [DAILY_JSON_CAP] newest days.
     *
     * Union semantics: every cloud device entry is kept; only [devId]'s own
     * entry per day is overwritten with the local buckets (days absent locally
     * keep the cloud's own-device entry untouched). Sanitizes both sides
     * (drops negative days / non-positive seconds / blank device ids).
     *
     * Size guard: v2 entries run ~50+ chars/day/device, so the
     * [DAILY_JSON_MAX_CHARS] attribute limit binds long before the day-count
     * cap (~130 single-device days fit) — oldest days are dropped first until
     * the measured payload is under the limit. Never throws (falls back to
     * `"{}"`).
     */
    private fun encodeDailyJsonV2(
        cloud: Map<Long, Map<String, Long>>,
        devId: String,
        local: Map<Long, Long>,
    ): String {
        try {
            val cleanDev = devId.trim().takeIf { it.isNotEmpty() } ?: return "{}"
            val merged = LinkedHashMap<Long, MutableMap<String, Long>>(
                cloud.size + local.size
            )
            for ((day, devs) in cloud) {
                if (day < 0L) continue
                val clean = devs.filter { it.key.isNotBlank() && it.value > 0L }
                if (clean.isNotEmpty()) merged[day] = clean.toMutableMap()
            }
            for ((day, secs) in local) {
                if (day < 0L || secs <= 0L) continue
                merged.getOrPut(day) { LinkedHashMap() }[cleanDev] = secs
            }
            if (merged.isEmpty()) return "{}"
            var days = merged.keys.sortedDescending().take(DAILY_JSON_CAP)
            var out = buildDailyJsonV2(merged, days)
            while (out.length >= DAILY_JSON_MAX_CHARS && days.size > 1) {
                days = days.dropLast(1)
                out = buildDailyJsonV2(merged, days)
            }
            return out
        } catch (_: Exception) {
            return "{}"
        }
    }

    /** Renders the v2 daily map for [daysDesc] (newest first), oldest day first on the wire. */
    private fun buildDailyJsonV2(
        merged: Map<Long, Map<String, Long>>,
        daysDesc: List<Long>,
    ): String {
        val days = daysDesc.sorted()
        return buildString(days.size * 64 + 2) {
            append('{')
            var firstDay = true
            for (day in days) {
                val devs = merged[day] ?: continue
                if (devs.isEmpty()) continue
                if (!firstDay) append(',')
                firstDay = false
                append('"').append(day).append('"').append(':').append('{')
                devs.entries.forEachIndexed { index, (dev, secs) ->
                    if (index > 0) append(',')
                    append(jsonString(dev)).append(':').append(secs)
                }
                append('}')
            }
            append('}')
        }
    }

    /**
     * Lenient per-device parse of `daily_json`: accepts BOTH shapes —
     * - v2 per-device: `{"<epochDay>":{"<devId>":seconds,...}}`
     * - legacy flat: `{"<epochDay>":seconds}` (treated as `{"legacy":seconds}`)
     * Non-numeric day keys, negative days, blank device ids, non-numeric or
     * non-positive seconds, and malformed entries are skipped. Missing/blank /
     * malformed top-level input yields an empty map. Never throws.
     */
    private fun parseDailyJsonPerDevice(raw: String): Map<Long, Map<String, Long>> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val out = LinkedHashMap<Long, Map<String, Long>>(obj.size)
            for ((key, value) in obj) {
                val day = key.toLongOrNull() ?: continue
                if (day < 0L) continue
                try {
                    val el = value
                    val prim = try {
                        el.jsonPrimitive
                    } catch (_: Exception) {
                        null
                    }
                    if (prim != null) {
                        // Flat value (numeric or quoted-numeric) → legacy.
                        val secs = prim.longOrNull
                            ?: prim.content.toDoubleOrNull()?.toLong()
                            ?: continue
                        if (secs <= 0L) continue
                        out[day] = mapOf("legacy" to secs)
                    } else {
                        // Object value → v2 per-device map.
                        val devObj = try {
                            el.jsonObject
                        } catch (_: Exception) {
                            continue
                        }
                        val devMap = LinkedHashMap<String, Long>(devObj.size)
                        for ((devId, devVal) in devObj) {
                            val cleanId = devId.trim()
                            if (cleanId.isEmpty()) continue
                            val secs = try {
                                val p = devVal.jsonPrimitive
                                p.longOrNull
                                    ?: p.content.toDoubleOrNull()?.toLong()
                            } catch (_: Exception) {
                                null
                            } ?: continue
                            if (secs <= 0L) continue
                            devMap[cleanId] = secs
                        }
                        if (devMap.isEmpty()) continue
                        out[day] = devMap
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Splits a per-device daily map into `(ownDaily, remoteOthers)`:
     * - `ownDaily[day]` = this device's entry only (absent when this device
     *   has no entry that day) — the ONLY slice ever merged into local buckets.
     * - `remoteOthers[day]` = per-day sum of ALL device entries EXCEPT the own
     *   device id (legacy `"legacy"` entries count as others). Never throws.
     */
    private fun splitOwnAndRemote(
        perDevice: Map<Long, Map<String, Long>>,
        ownDevId: String,
    ): Pair<Map<Long, Long>, Map<Long, Long>> {
        try {
            if (perDevice.isEmpty()) return emptyMap<Long, Long>() to emptyMap<Long, Long>()
            val own = LinkedHashMap<Long, Long>()
            val remote = LinkedHashMap<Long, Long>()
            for ((day, devMap) in perDevice) {
                if (day < 0L) continue
                var othersSum = 0L
                for ((devId, secs) in devMap) {
                    if (secs <= 0L) continue
                    if (ownDevId.isNotEmpty() && devId == ownDevId) {
                        own[day] = secs
                    } else {
                        othersSum += secs
                    }
                }
                if (othersSum > 0L) remote[day] = othersSum
            }
            return own to remote
        } catch (_: Exception) {
            return emptyMap<Long, Long>() to emptyMap<Long, Long>()
        }
    }

    /**
     * Lenient parse of the legacy flat daily-series shape
     * (`{"<epochDay>":seconds,...}`): non-numeric keys/values and
     * non-positive entries are skipped. Missing/blank/malformed input yields
     * an empty map. Never throws.
     *
     * NOTE: currently unreferenced — the pull path parses both shapes via
     * [parseDailyJsonPerDevice] (legacy days land under `"legacy"`) and the
     * push path encodes v2 via [encodeDailyJsonV2]. Kept as the documented
     * legacy-shape reader.
     */
    private fun parseDailyJson(raw: String): Map<Long, Long> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val out = LinkedHashMap<Long, Long>(obj.size)
            for ((key, value) in obj) {
                val day = key.toLongOrNull() ?: continue
                if (day < 0L) continue
                val secs = try {
                    value.jsonPrimitive.longOrNull
                        ?: value.jsonPrimitive.content.toDoubleOrNull()?.toLong()
                } catch (_: Exception) {
                    null
                } ?: continue
                if (secs <= 0L) continue
                out[day] = secs
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Per-day `max()` merge of the local and cloud daily series (rationale:
     * see the stats section header). Sanitizes both sides (drops negative
     * days / non-positive seconds). Never throws.
     */
    private fun mergeDailyMax(local: Map<Long, Long>, cloud: Map<Long, Long>): Map<Long, Long> {
        try {
            if (cloud.isEmpty()) return local.filter { it.key >= 0L && it.value > 0L }
            if (local.isEmpty()) return cloud.filter { it.key >= 0L && it.value > 0L }
            val merged = local.filter { it.key >= 0L && it.value > 0L }.toMutableMap()
            for ((day, secs) in cloud) {
                if (day < 0L || secs <= 0L) continue
                merged[day] = maxOf(merged[day] ?: 0L, secs)
            }
            return merged
        } catch (_: Exception) {
            return local
        }
    }

    /**
     * True when local stats are still at fresh-install defaults (no seconds
     * listened today or ever, no streak, no uniques). Anything beyond that
     * counts as real local activity.
     *
     * Only the PUSH's clobber guard still uses this, to avoid overwriting a
     * cloud snapshot with local zeros. The pull no longer gates on it — it
     * merges per field (see [pullStats]) — so this is deliberately NOT the
     * "safe to overwrite" test any more, just the "is there anything worth
     * keeping locally" test.
     */
    private fun isLocalStatsEmpty(): Boolean {
        val s = UserUsageRepository.stats.value
        return s.totalSecondsListened <= 0L &&
            s.minutesToday <= 0 &&
            s.daysStreak <= 1 &&
            s.uniqueRecitersCount <= 1 &&
            s.uniqueSurahsCount <= 1
    }

    // ---------------------------------------------------------------- history
    //
    // Merge-pull + push-capped (push skips when local is still empty but cloud
    // has data, so a fresh login never clobbers its restore source with an empty
    // state — no-data-loss guard, same as playback/stats).
    //
    // Cloud shape: `history{user_id,track_json,played_at_ms,position_ms}` (index on
    // `user_id,played_at_ms`). `track_json = Json.encodeToString(TrackItem)`
    // ([TrackItem] is `@Serializable` and carries no position, so the live
    // `positionMs` travels as top-level `position_ms`; rebuilt from the local
    // [JumpBackInItem] via reciter/surah lookups so the JSON carries real
    // display names + audio URL).
    //
    // Doc ids are stable per ROW, not per listen: `recordProgress` keeps at most
    // one entry per `(surahId, reciterSlug)` and stamps it once per session, so
    // a row is the unit the user sees. The id is
    // `"h-" + sha256("<userId>|<slug>|<surahId>")[0..24)` (26 chars) — the
    // [likeDocId]/[followDocId] shape. Two consequences, both deliberate:
    // - The user's `userId` is hashed IN, which the previous
    //   `"h-<slug>-<surahId>-<playedAtMs>"` scheme did not do. `history` has
    //   `documentSecurity: true`, so a slug-only id would make the second user
    //   to listen to the same surah get 409 on create and then 401/403 on the
    //   `upsert` fallback, failing the whole collection block.
    // - One doc per (user, reciter, surah) means the doc set is bounded by the
    //   same [HISTORY_CAP] as the local list, so a restore really does bring
    //   back [HISTORY_CAP] distinct rows instead of however many duplicates
    //   (two devices, one session change) happened to be in the collection.
    //   It also means an update-in-place: `$createdAt` survives, so the resume
    //   row stops being re-created and re-pruned on every listen session.
    //
    // Migration: ids written by the old scheme are orphaned by the change, and
    // the existing prune is what retires them — a doc whose id is not in
    // `localIds` is deleted in the same pass that writes the new one, so there
    // is no permanent double set and nothing to migrate out of band. Content is
    // never at risk, because the pull reads by `user_id` + `played_at_ms` and
    // never looks at ids. The one real risk is a fleet running BOTH schemes at
    // once: an old build and a new one will each delete the other's docs for the
    // same row on every pass. That is doc-level churn (a resume position that
    // flip-flops), never data loss, and it resolves as soon as every device is
    // on the new build.
    //
    // Prune safety (asked explicitly, so stated explicitly): the prune can no
    // longer delete "another device's live doc" for a row this device holds,
    // because there is only ONE doc per row now and this device's copy of it is
    // in `localIds`. The blast radius is unchanged from the old scheme: a doc
    // whose `(reciter, surah)` is absent from this device's local top-15 is
    // still deleted, which is the pre-existing last-writer-wins-the-collection
    // semantics shared by likes/follows/khatma (and the reason the merge-pull
    // above exists — the union guarantees those 15 rows are the 15 the user
    // actually has, not a stale subset). The `unresolvedSurahs` quarantine still
    // covers rows whose slug we cannot resolve, legacy-id rows included.

    /**
     * Pulls the cloud `history` docs (ordered by `played_at_ms` desc) and
     * UNIONS them into local history via [UserUsageRepository.restoreHistory].
     *
     * No local-emptiness gate any more: on a device that restored one stale row,
     * or that started a track in the 2s between login and the first pass, the
     * gate skipped the cloud history FOREVER — and the paired [pushHistory],
     * which converges the cloud to the exact local set, then deleted it.
     *
     * Merge semantics (local wins, cloud only ever adds):
     * - Identity is the row `(reciterSlug, surahId)`, which is exactly what
     *   `UserUsageRepository` keys on, and exactly what the new doc id hashes.
     *   `restoreHistory` itself keeps the local row for a key it already holds
     *   and appends the rest, so it can never roll local data back.
     * - Cloud rows are matched against local rows through [historyRowKey], which
     *   resolves the slug to its canonical catalog spelling first. Without
     *   that, a local row saved as `"mishary"` and a cloud row written as
     *   `"alafasy"` (the two spellings of one reciter) would both be kept and
     *   burn two of the [HISTORY_CAP] slots on one surah. Pre-filtering here
     *   changes nothing in the store — it would have dropped those rows anyway
     *   on its own key — it just stops them consuming the cap.
     * - What the cloud cannot do is REPLACE a stale local row with a newer one
     *   for the same key: `restoreHistory` has no overwrite path, and adding
     *   one is the store owner's call, not this file's. Net effect: the cloud
     *   is authoritative for rows this device has never played, and this device
     *   is authoritative for rows it has.
     *
     * Returns true when the cloud holds at least one usable row (the caller
     * must then skip pushing an empty local state, which would overwrite it).
     * Returns false when the cloud has nothing usable, so push proceeds.
     */
    private suspend fun pullHistory(db: Databases, userId: String): Boolean {
        val docs = listHistoryForUser(db, userId) ?: return false
        if (docs.isEmpty()) return false
        val items = docs.mapNotNull { doc ->
            val trackJson = (doc.data["track_json"] as? String)?.trim().orEmpty()
            if (trackJson.isEmpty()) return@mapNotNull null
            val playedAt = asLong(doc.data["played_at_ms"]) ?: return@mapNotNull null
            if (playedAt <= 0L) return@mapNotNull null
            val positionMs = asLong(doc.data["position_ms"])?.coerceAtLeast(0L) ?: 0L
            val track = try {
                json.decodeFromString<TrackItem>(trackJson)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            Triple(track, playedAt, positionMs)
        }
        if (items.isEmpty()) return false // cloud snapshot itself is empty; let push converge
        val ordered = items.sortedByDescending { it.second }
        val localBefore = UserUsageRepository.history.value
        val localKeys = localBefore.mapTo(mutableSetOf()) { historyRowKey(it.reciterSlug, it.surahId) }
        // `restoreHistory` sorts by playedAt desc and caps at HISTORY_CAP, so
        // newest-first order is preserved and the newest cloud rows are the
        // ones that survive the cap.
        val fresh = ordered.filter { (track, _, _) ->
            localKeys.add(historyRowKey(track.reciterSlug, track.surahId))
        }
        if (fresh.isEmpty()) {
            Log.i(
                TAG,
                "history pull: nothing to merge — local already holds all ${ordered.size} cloud row(s)."
            )
            return true
        }
        UserUsageRepository.restoreHistory(fresh)
        val mergedSize = UserUsageRepository.history.value.size
        Log.i(
            TAG,
            "history pull: merged ${fresh.size} cloud row(s) into local history " +
                "(${localBefore.size} -> $mergedSize; cloud had " +
                "${ordered.size}, most recent playedAt=${ordered.firstOrNull()?.second})."
        )
        return true
    }

    /**
     * Identity of one history row for merge purposes: the reciter's CANONICAL
     * catalog slug plus the surah id. The canonicalisation is what makes
     * `"mishary"` (a pre-catalog alias, and the shape a local row can still
     * carry) and `"alafasy"` (what the pusher writes, from [Reciter.slug])
     * compare equal, so one surah never occupies two rows.
     */
    private fun historyRowKey(reciterSlug: String, surahId: Int): String {
        val canonical = try {
            findReciterOrNull(reciterSlug)?.slug
        } catch (_: Exception) {
            null
        } ?: reciterSlug.trim().lowercase()
        return "$canonical#$surahId"
    }

    private suspend fun pushHistory(db: Databases, userId: String, cloudHasData: Boolean) {
        val local = UserUsageRepository.history.value.take(HISTORY_CAP)
        if (local.isEmpty()) {
            if (cloudHasData) {
                // Fresh login whose cloud snapshot couldn't be applied locally
                // yet: don't overwrite it with an empty state.
                Log.i(TAG, "pushHistory skipped (local empty, cloud has data).")
            }
            return
        }
        val existing = listHistoryForUser(db, userId) ?: return
        val existingById = HashMap<String, io.appwrite.models.Document<Map<String, Any>>>(existing.size * 2)
        for (doc in existing) existingById[doc.id] = doc
        val now = System.currentTimeMillis()
        val localIds = mutableSetOf<String>()
        // Prune invariant: the loop below may only delete a cloud doc this
        // device can PROVE it no longer has. An entry whose reciter slug /
        // audio URL no longer resolves (stale or renamed slug, missing surah)
        // cannot be turned back into the doc id the writing device used (that
        // id is built from the CANONICAL slug, which is unknowable here), so
        // such a surah is quarantined: we still register the best-effort id we
        // CAN compute, and every doc in that surah is exempt from the prune.
        // Without this, one stale slug on one device silently deletes that
        // surah's history on every other device.
        val unresolvedSurahs = mutableSetOf<Int>()
        local.forEachIndexed { index, item ->
            // Zero-timestamp entries are in-session items not yet reloaded from
            // disk (PersistedHistoryItem stamps on save): stagger them so the
            // local most-recent-first order survives the played_at_ms ranking.
            val playedAt = item.lastPlayedTimestampMs.takeIf { it > 0L }
                ?: (now - index)
            val track = buildHistoryTrack(item)
            if (track == null) {
                unresolvedSurahs += item.surahId
                // Best-effort protection in case the writing device used the
                // same raw slug (already-known reciter, blank audio URL).
                localIds += historyDocId(userId, item.reciterSlug, item.surahId)
                Log.w(
                    TAG,
                    "history push: skipping unresolvable entry " +
                        "'${item.reciterSlug}/${item.surahId}' (surah excluded from prune)."
                )
                return@forEachIndexed
            }
            val docId = historyDocId(userId, track.reciterSlug, track.surahId)
            // Registered BEFORE any skip below: a row this device holds must
            // never be pruned, whether or not we ended up writing it.
            localIds.add(docId)
            val positionMs = item.positionMs.coerceAtLeast(0L)
            val trackJson = json.encodeToString(track)
            val cloudDoc = existingById[docId]
            if (cloudDoc != null) {
                val cloudPlayedAt = asLong(cloudDoc.data["played_at_ms"])
                val cloudPosition = asLong(cloudDoc.data["position_ms"])
                val cloudTrackJson = cloudDoc.data["track_json"] as? String
                if (cloudTrackJson == trackJson &&
                    cloudPlayedAt == playedAt &&
                    cloudPosition == positionMs
                ) {
                    // Byte-identical: the common case for the settled rows of
                    // every pass. Skipping them is what makes the push cheap
                    // enough to run on the heartbeat's cadence.
                    return@forEachIndexed
                }
                if (cloudPlayedAt != null && cloudPlayedAt > playedAt) {
                    // Another device (or a later session on this one) has a
                    // NEWER listen of the same row. Writing our older one would
                    // move the shared row backwards for every device, so keep
                    // theirs; `localIds` already protects it from the prune, and
                    // the next pass re-evaluates once local catches up.
                    Log.i(
                        TAG,
                        "history push: keeping newer cloud row $docId " +
                            "(cloud playedAt=$cloudPlayedAt > local $playedAt)."
                    )
                    return@forEachIndexed
                }
            }
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "track_json" to trackJson,
                "played_at_ms" to playedAt,
                "position_ms" to positionMs,
            )
            upsert(db, HISTORY, docId, data)
        }
        for (doc in existing) {
            if (doc.id in localIds) continue
            if (unresolvedSurahs.isNotEmpty()) {
                val docSurahId = historyDocSurahId(doc)
                if (docSurahId != null && docSurahId in unresolvedSurahs) {
                    Log.i(
                        TAG,
                        "history prune: keeping ${doc.id} (surah $docSurahId has an " +
                            "unresolvable local entry)."
                    )
                    continue
                }
            }
            try {
                db.deleteDocument(DB_ID, HISTORY, doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "delete stale history ${doc.id} failed", e)
            }
        }
    }

    /**
     * Surah id a cloud `history` doc belongs to, decoded from its `track_json`.
     * Null when the payload is missing/malformed — such a doc is not a real
     * entry of any device, so it stays prunable as before.
     */
    private fun historyDocSurahId(
        doc: io.appwrite.models.Document<Map<String, Any>>,
    ): Int? = try {
        val raw = (doc.data["track_json"] as? String)?.trim().orEmpty()
        if (raw.isEmpty()) null else json.decodeFromString<TrackItem>(raw).surahId
    } catch (_: Exception) {
        null
    }

    /**
     * Rebuilds a fully playable [TrackItem] from a local history entry, or
     * null when the slug/surah is unknown (caller skips the entry). History
     * entries are per-surah (no ayah granularity), so the URL is always the
     * full-surah stream and names resolve via [QuranDataRepository].
     */
    private fun buildHistoryTrack(item: JumpBackInItem): TrackItem? {
        val reciter = findReciterOrNull(item.reciterSlug) ?: return null
        val surah = runCatching { QuranDataRepository.getSurahById(item.surahId) }.getOrNull()
            ?: return null
        val audioUrl = runCatching { reciter.getFullSurahUrl(surah.id) }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        return TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = 0,
            audioUrl = audioUrl,
            durationMs = item.durationMs,
        )
    }

    /**
     * 404-safe history list for [userId], ordered by `played_at_ms` desc and
     * capped at [HISTORY_CAP] (matches the cloud `user_id,played_at_ms`
     * index). Null when the collection doesn't exist yet; other failures
     * throw into the per-collection `runCollection` guard.
     */
    private suspend fun listHistoryForUser(
        db: Databases,
        userId: String,
    ): List<io.appwrite.models.Document<Map<String, Any>>>? {
        return try {
            db.listDocuments(
                DB_ID,
                HISTORY,
                listOf(
                    Query.equal("user_id", userId),
                    Query.orderDesc("played_at_ms"),
                    Query.limit(HISTORY_CAP),
                ),
            ).documents
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                Log.w(TAG, "Collection '$HISTORY' not found; skipping.")
                null
            } else {
                throw e
            }
        }
    }

    /**
     * History doc id: `"h-" + sha256("<userId>|<slug>|<surahId>")[0..24)` (26
     * chars) — one document per ROW, i.e. per (user, reciter, surah), which is
     * the unit `recordProgress` maintains and the unit the user sees.
     *
     * Stable across position ticks (position lives in an attribute, not the id)
     * and across listen sessions, so a replay updates the row in place instead
     * of creating a new document and pruning the old one. `userId` is hashed in
     * because `history` is `documentSecurity: true`: a slug-only id would make
     * the second user to play a surah collide with the first (409 on create,
     * 401/403 on the update fallback) and fail the whole collection block.
     *
     * See the history section header for the migration story (old
     * timestamp-keyed docs are retired by the prune in the same pass).
     */
    private fun historyDocId(userId: String, slug: String, surahId: Int): String =
        "h-${shortHash("$userId|${slug.trim().lowercase()}|$surahId")}"

    // --------------------------------------------------------------- schedules
    //
    // Merge-pull + push-with-guard, the same shape as the other set
    // collections. `schedules` carries the WHOLE serialised
    // [RecitationSchedule] in `schedule_json` plus `schedule_id` and
    // `enabled`, so no schema change is involved.
    //
    // This block used to be push-ONLY: [pushSchedules] pruned first, and on a
    // fresh device (empty local) the first pass after login therefore deleted
    // every `schedule_id` the account had — for EVERY device, since the delete
    // is on the shared cloud copy. The pull is what makes the prune sound:
    // [SchedulesStore.restoreAllFromCloud] merges and preserves ids, so after
    // it local ⊇ cloud ids and the prune can only retire docs this device
    // really has dropped.
    //
    // Prune key: a doc is live when ANY of three keys is held locally — its
    // `schedule_id` attribute, the id inside its `schedule_json`, or its own
    // document id (which is `scheduleDocId` = the schedule id, truncated). The
    // push writes the first two from the same object so they agree, and
    // accepting any one of them means an inconsistent or partially written doc
    // is kept rather than deleted — the only safe direction here.

    /**
     * Union-pulls the cloud `schedules` docs into [SchedulesStore].
     *
     * No local-emptiness gate and no replace, for the reason in the section
     * header: either would re-open the mass-delete hole the guarded prune
     * closes. The store's merge keeps a locally present id untouched and adds
     * the rest with its id verbatim, which is exactly what makes the paired
     * [pushSchedules] prune a no-op right after this pull.
     *
     * Decoding is defensive: a document with a missing/blank `schedule_json`, a
     * malformed payload, or a blank id is SKIPPED, never thrown, so one bad row
     * can neither abort the pass nor (via the push) be treated as proof that
     * the rest of the account's schedules are gone.
     *
     * Returns true when the cloud holds at least one schedule document
     * (including documents that did not parse — the caller must then skip
     * pushing an empty local set rather than prune against an unusable
     * snapshot). False when the collection is missing or empty, so push
     * converges.
     */
    private suspend fun pullSchedules(db: Databases, userId: String): Boolean {
        val docs = listForUser(db, SCHEDULES, userId) ?: return false
        if (docs.isEmpty()) return false
        val remote = docs.mapNotNull { doc ->
            val raw = (doc.data["schedule_json"] as? String)?.trim().orEmpty()
            if (raw.isEmpty()) return@mapNotNull null
            val schedule = try {
                json.decodeFromString<RecitationSchedule>(raw)
            } catch (_: Exception) {
                null
            } ?: return@mapNotNull null
            if (schedule.id.isBlank()) return@mapNotNull null
            schedule
        }
        if (remote.isEmpty()) {
            Log.w(
                TAG,
                "schedules pull: cloud holds ${docs.size} document(s) but none carried a " +
                    "decodable schedule — local left untouched, push guarded."
            )
            return true
        }
        val added = SchedulesStore.restoreAllFromCloud(remote)
        Log.i(
            TAG,
            "schedules pull: merged ${remote.size} cloud schedule(s) into " +
                "${SchedulesStore.schedules.value.size} local (+$added new)."
        )
        return true
    }

    private suspend fun pushSchedules(db: Databases, userId: String, cloudHasData: Boolean) {
        val local = SchedulesStore.schedules.value
        if (local.isEmpty()) {
            if (cloudHasData) {
                // Fresh login whose cloud snapshot couldn't be applied locally
                // yet: don't overwrite it with nothing.
                Log.i(TAG, "pushSchedules skipped (local empty, cloud has data).")
            }
            return
        }
        val existing = listForUser(db, SCHEDULES, userId) ?: return
        val localIds = local.mapTo(mutableSetOf()) { it.id }
        for (doc in existing) {
            val attributeId = (doc.data["schedule_id"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val payloadId = schedulePayloadId(doc)
            val live = (attributeId != null && attributeId in localIds) ||
                (payloadId != null && payloadId in localIds) ||
                doc.id in localIds
            if (live) continue
            try {
                db.deleteDocument(DB_ID, SCHEDULES, doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "delete stale schedule ${doc.id} failed", e)
            }
        }
        // RecitationSchedule is @Serializable: full fidelity via JSON string.
        for (schedule in local) {
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "schedule_id" to schedule.id,
                "schedule_json" to json.encodeToString(schedule),
                "enabled" to schedule.enabled,
            )
            upsert(db, SCHEDULES, scheduleDocId(userId, schedule.id), data)
        }
    }

    /**
     * The schedule id carried inside a cloud doc's `schedule_json`, or null when
     * it is missing/malformed. Used as a SECOND, payload-derived prune key so a
     * doc whose `schedule_id` attribute disagrees with its own payload is kept
     * rather than deleted.
     */
    private fun schedulePayloadId(
        doc: io.appwrite.models.Document<Map<String, Any>>,
    ): String? = try {
        val raw = (doc.data["schedule_json"] as? String)?.trim().orEmpty()
        if (raw.isEmpty()) {
            null
        } else {
            json.decodeFromString<RecitationSchedule>(raw).id.trim().takeIf { it.isNotEmpty() }
        }
    } catch (_: Exception) {
        null
    }

    // --------------------------------------------------------------- khatma plans
    //
    // Merge-pull + push (push skips when local is still empty but cloud
    // holds data, so a fresh login never clobbers its restore source with an
    // empty state — no-data-loss guard, same as playback/stats/history).
    //
    // Cloud shape: `khatma_plans{user_id,title,total_days,current_surah,
    // current_ayah,percent,streak,total_ayahs_read,start_date_ms,last_progress_ms}`
    // (index on `user_id`). The collection still carries no plan id, so doc ids
    // stay stable per plan: `"khatma-<sanitizedPlanId>"` (max 36 chars) and the
    // pull derives the plan id back from the doc id.
    //
    // The last four attributes are what make a restored plan the SAME plan
    // rather than a plausible copy of it. [KhatmaStore.planOrder] sorts by
    // `startDateMs` DESCENDING and `activePlan()` is the first element, so a
    // fabricated `startDateMs = now` did not just lose a streak — it promoted
    // the wrong plan to the one the user is looking at. They are all optional
    // server-side and all in `collections.json` today, so the push writes them
    // and the pull reads them; the fabrication below survives ONLY as the
    // legacy-document fallback (see [pullKhatma]).

    /**
     * `khatma_plans` attributes the cloud may not have provisioned yet, in the
     * order they are shed when the server rejects a write as unknown-attribute
     * (see [pushKhatma]). All four ARE in `appwrite/collections.json` today;
     * the list is kept for a deployment that predates them, because a plan
     * restored without its streak is a much smaller loss than a khatma push
     * that never syncs at all.
     */
    private val KHATMA_OPTIONAL_ATTRS = listOf(
        "streak",
        "total_ayahs_read",
        "start_date_ms",
        "last_progress_ms",
    )

    /**
     * Pulls the cloud `khatma_plans` docs and UNIONS them with the local plans.
     *
     * No local-emptiness gate any more (class KDoc): a plan created while
     * signed out is adopted into the freshly-bound account, and that was enough
     * for the paired [pushKhatma] — which prunes by plan id — to delete every
     * plan the account actually had in the cloud.
     *
     * Union, not overwrite: [KhatmaStore.restoreAll] REPLACES the store, so the
     * local plans are put back in the same call. A plan id present on both
     * sides keeps the LOCAL object untouched — the cloud carries the plan's
     * streak and dates but no per-plan `updated_at`, so there is no ordering to
     * reconcile them by and nothing to merge INSIDE a plan, only to add or keep
     * — which is also what makes the push's prune a no-op right after this pull.
     */
    private suspend fun pullKhatma(db: Databases, userId: String): Boolean {
        val docs = listForUser(db, KHATMA, userId) ?: return false
        if (docs.isEmpty()) return false
        val now = System.currentTimeMillis()
        val local = KhatmaStore.plans.value
        val localIds = local.mapTo(mutableSetOf()) { it.id }
        val fromCloud = docs.mapNotNull { doc ->
            val data = doc.data
            val planId = doc.id.removePrefix("khatma-").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val title = (data["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            if (!localIds.add(planId)) return@mapNotNull null // local wins the id
            val targetDays = asInt(data["total_days"])?.coerceAtLeast(1) ?: 30
            val surah = asInt(data["current_surah"])?.coerceIn(1, 114) ?: 1
            val ayah = asInt(data["current_ayah"])?.coerceAtLeast(1) ?: 1
            val percent = asDouble(data["percent"])?.toFloat()?.coerceIn(0f, 1f) ?: 0f
            // The four progress attributes are all OPTIONAL server-side, so each
            // is read on its own and the fabrication survives only for the
            // document that genuinely predates it:
            // - `total_ayahs_read`: a real 0 (a plan nobody has read on yet) is
            //   DISTINGUISHED from an absent attribute — `asInt` returns 0 for
            //   both, so only a null/missing value falls through to the `percent`
            //   estimate, which is all a legacy document can offer.
            // - `start_date_ms` / `last_progress_ms`: 0 is not a meaningful
            //   instant for either (KhatmaStore itself reads `lastProgressMs <= 0`
            //   as "no progress yet"), and a 0 start date would sort the plan to
            //   the BOTTOM of planOrder and hand `activePlan()` to a different
            //   plan — the exact failure these attributes exist to prevent. So 0
            //   is treated as absent and keeps the pre-existing `now` fallback.
            // - `streak`: absent means the old behaviour, 0.
            val totalAyahsRead = asInt(data["total_ayahs_read"])?.coerceAtLeast(0)
                ?: (percent * 6236f).toInt().coerceIn(0, 6236)
            KhatmaPlan(
                id = planId,
                title = title,
                targetDays = targetDays,
                currentSurahId = surah,
                currentAyahNo = ayah,
                streak = asInt(data["streak"])?.coerceAtLeast(0) ?: 0,
                startDateMs = asLong(data["start_date_ms"])?.takeIf { it > 0L } ?: now,
                lastProgressMs = asLong(data["last_progress_ms"])?.takeIf { it > 0L } ?: now,
                totalAyahsRead = totalAyahsRead,
            )
        }
        if (fromCloud.isEmpty()) {
            Log.i(TAG, "khatma pull: nothing to merge — local already holds all ${docs.size} plan(s).")
            return true
        }
        KhatmaStore.restoreAll(local + fromCloud)
        Log.i(
            TAG,
            "khatma pull: merged ${fromCloud.size} cloud plan(s) into ${local.size} local " +
                "(cloud had ${docs.size})."
        )
        return true
    }

    private suspend fun pushKhatma(db: Databases, userId: String, cloudHasData: Boolean) {
        val local = KhatmaStore.plans.value
        if (local.isEmpty()) {
            if (cloudHasData) {
                // Fresh login whose cloud snapshot couldn't be applied locally
                // yet: don't overwrite it with an empty state.
                Log.i(TAG, "pushKhatma skipped (local empty, cloud has data).")
            }
            return
        }
        val existing = listForUser(db, KHATMA, userId) ?: return
        val localIds = local.map { it.id }.toSet()
        for (doc in existing) {
            val planId = doc.id.removePrefix("khatma-")
            if (planId !in localIds) {
                try {
                    db.deleteDocument(DB_ID, KHATMA, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "delete stale khatma plan ${doc.id} failed", e)
                }
            }
        }
        var shed = emptySet<String>()
        for (plan in local) {
            val full: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "title" to plan.title,
                "total_days" to plan.targetDays,
                "current_surah" to plan.currentSurahId,
                "current_ayah" to plan.currentAyahNo,
                "percent" to plan.progressPercentage.toDouble(),
                "streak" to plan.streak,
                "total_ayahs_read" to plan.totalAyahsRead,
                "start_date_ms" to plan.startDateMs,
                "last_progress_ms" to plan.lastProgressMs,
            )
            val docId = khatmaDocId(plan.id)
            // Shed-on-unknown-attribute, same posture as [pushLikes]: the server
            // rejects the WHOLE document for the first unprovisioned attribute it
            // meets, so drop that one and retry the same doc without it rather
            // than let an older deployment lose the whole khatma push. Looped
            // because all four can be missing at once; terminates because `shed`
            // only grows and is bounded by [KHATMA_OPTIONAL_ATTRS].
            var attempt = full.filterKeys { it !in shed }
            while (true) {
                try {
                    upsert(db, KHATMA, docId, attempt)
                    break
                } catch (e: Exception) {
                    val attr = KHATMA_OPTIONAL_ATTRS.firstOrNull {
                        it in attempt && isUnknownAttribute(e, it)
                    } ?: throw e
                    Log.w(TAG, "khatma_plans.$attr not provisioned yet; pushing without it.")
                    shed += attr
                    attempt = full.filterKeys { it !in shed }
                }
            }
        }
    }

    // ------------------------------------------------------------ hifz mastery
    //
    // Union-pull + push-with-guard, the same shape as every other set
    // collection in this file (see the class KDoc for why a pull may never be
    // gated on local emptiness when the paired push prunes).
    //
    // Cloud shape: `hifz_mastery{user_id,surah_id,ayah_no,status}` with a
    // `user_surah_ayah_idx` KEY index on the first three. That is the whole
    // collection, and it is why [HifzSnapshot.statuses] is deliberately not
    // `@Serializable`: the map is encoded as N documents, one per (user, ayah),
    // never as a JSON blob. The `status` column carries the `MasteryStatus`
    // enum NAME (`NEW` / `REVIEW_NEEDED` / `MASTERED`), which is exactly the
    // spelling the store persists locally, so a row is a value, not an index.
    //
    // `MasteryStatus.NEW` is never written: the store treats an unset ayah and
    // a `NEW` ayah as the same thing (`exportSnapshot` omits it,
    // `importSnapshot` skips it), so a `NEW` document would be a row no side of
    // this sync can represent.
    //
    // ## The four scalars do NOT round-trip — a schema gap, not a choice
    //
    // [HifzSnapshot] also carries `dailyGoalCount`, `streakDays`,
    // `todayReviewedCount` and `lastReviewDay`, and the collection has NO column
    // for any of them. So they cannot be pushed, and — the part that matters —
    // they must not be INVENTED on the way back: `importSnapshot` is
    // authoritative (it clears the namespace first), so importing a snapshot
    // built from cloud documents alone would reset a returning user's daily
    // goal to the default and their review streak to 0 on every single pass.
    // [pullHifzMastery] therefore merges the CLOUD statuses into the LOCAL
    // snapshot and re-imports that, which leaves the scalars exactly as the
    // device had them. Closing the gap needs four integer columns on
    // `hifz_mastery` in `appwrite/collections.json` (a file this one does not
    // own); until then a hifz profile is per-ayah cross-device and
    // per-device for its counters.
    //
    // Conflict rule: `hifz_mastery` has one row per ayah and no per-row
    // `updated_at`, so two devices that cycled the same ayah differently cannot
    // be reconciled — the last writer's value wins, which is the same
    // last-writer-wins-per-row semantics [pushLikes]/[pushSchedules] already
    // have, and the reason the pull resolves collisions LOCAL-wins: a status
    // this device just set is the freshest evidence it has.

    /**
     * Union-pulls the cloud `hifz_mastery` docs into [HifzMasteryStore].
     *
     * No local-emptiness gate: pre-auth mastery is adopted into a freshly-bound
     * account, and one ayah memorised while signed out was enough for a gated
     * pull to skip the cloud forever while the paired [pushHifzMastery] pruned
     * it away.
     *
     * Decoding is defensive per document — a missing/blank `status`, an
     * unparseable enum name, an out-of-range `surah_id`/`ayah_no` or a `NEW`
     * status is SKIPPED, never thrown, so one bad row can neither abort the pass
     * nor (via the push) count as proof that the rest of the account's mastery
     * is gone. A cloud snapshot with nothing usable in it returns true anyway,
     * which keeps the push from pruning against a snapshot it could not read.
     *
     * Returns true when the cloud holds at least one document (the caller must
     * then skip pushing an empty local profile). False when the collection is
     * missing or empty, so push converges.
     */
    private suspend fun pullHifzMastery(db: Databases, userId: String): Boolean {
        val docs = listForUser(db, HIFZ_MASTERY, userId) ?: return false
        if (docs.isEmpty()) return false
        val fromCloud = HashMap<Pair<Int, Int>, MasteryStatus>(docs.size * 2)
        var skipped = 0
        for (doc in docs) {
            val data = doc.data
            val surahId = asInt(data["surah_id"])?.takeIf { it in 1..114 }
            val ayahNo = asInt(data["ayah_no"])?.takeIf { it > 0 }
            val name = (data["status"] as? String)?.trim()?.uppercase()
            val status = name?.let { candidate ->
                try {
                    MasteryStatus.valueOf(candidate)
                } catch (_: Exception) {
                    null
                }
            }
            if (surahId == null || ayahNo == null || status == null || status == MasteryStatus.NEW) {
                skipped++
                continue
            }
            fromCloud[surahId to ayahNo] = status
        }
        if (fromCloud.isEmpty()) {
            Log.w(
                TAG,
                "hifz pull: cloud holds ${docs.size} document(s) but none carried a usable " +
                    "status — local left untouched, push guarded."
            )
            return true
        }
        val local = HifzMasteryStore.exportSnapshot()
        // Union, local wins the ayah (Pair is a data class, so `+` is a
        // per-coordinate merge). `importSnapshot` is AUTHORITATIVE — it clears
        // the namespace before writing — so the local map has to go back in
        // explicitly, or an ayah memorised here and not yet pushed would be
        // destroyed by the very pull meant to protect it. The scalars ride along
        // untouched; see the section header for why they must not be invented.
        val merged = local.statuses + fromCloud
        val added = merged.size - local.statuses.size
        if (added == 0) {
            Log.i(
                TAG,
                "hifz pull: nothing to merge — local already holds all ${fromCloud.size} cloud ayah(s)."
            )
            return true
        }
        HifzMasteryStore.importSnapshot(local.copy(statuses = merged))
        Log.i(
            TAG,
            "hifz pull: merged $added cloud ayah(s) into ${local.statuses.size} local " +
                "(cloud had ${docs.size}, $skipped unusable)."
        )
        return true
    }

    private suspend fun pushHifzMastery(db: Databases, userId: String, cloudHasData: Boolean) {
        val snapshot = HifzMasteryStore.exportSnapshot()
        // `exportSnapshot` already omits `NEW`, but the filter is kept explicit:
        // a `NEW` row must never claim a doc id, because that would make the
        // prune below KEEP a document that means "this ayah is unset".
        val statuses = snapshot.statuses.filterValues { it != MasteryStatus.NEW }
        if (statuses.isEmpty()) {
            if (cloudHasData) {
                // Fresh login whose cloud snapshot could not be applied locally
                // yet: don't overwrite it with an empty profile.
                Log.i(TAG, "pushHifzMastery skipped (local empty, cloud has data).")
            }
            return
        }
        val existing = listForUser(db, HIFZ_MASTERY, userId) ?: return
        val existingById = HashMap<String, io.appwrite.models.Document<Map<String, Any>>>(existing.size * 2)
        for (doc in existing) existingById[doc.id] = doc
        val localDocIds = mutableSetOf<String>()
        for ((coord, status) in statuses) {
            val docId = hifzMasteryDocId(userId, coord.first, coord.second)
            // Registered BEFORE any skip below: a row this device holds must
            // never be pruned, whether or not we ended up writing it.
            localDocIds += docId
            val cloudDoc = existingById[docId]
            if (cloudDoc != null && (cloudDoc.data["status"] as? String)?.trim() == status.name) {
                // Already exactly this. The settled case for nearly every row of
                // every pass, and what keeps a heavy profile cheap to push.
                continue
            }
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "surah_id" to coord.first,
                "ayah_no" to coord.second,
                "status" to status.name,
            )
            upsert(db, HIFZ_MASTERY, docId, data)
        }
        // Sound because [pullHifzMastery] ran first in the same block and unioned
        // the cloud in: after it, local ⊇ cloud, so a doc missing from
        // `localDocIds` is one this device really has dropped.
        for (doc in existing) {
            if (doc.id in localDocIds) continue
            try {
                db.deleteDocument(DB_ID, HIFZ_MASTERY, doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "delete stale hifz doc ${doc.id} failed", e)
            }
        }
    }

    // ---------------------------------------------------------- search history
    //
    // Union-pull + push-with-guard, once more the same shape. Cloud shape:
    // `search_history{user_id,query,ranked_at_ms}` with a `user_ranked_idx` KEY
    // index on (user_id, ranked_at_ms) AND a `user_query_unique` UNIQUE index on
    // (user_id, query) — so this is one document per (user, query), ranked by
    // `ranked_at_ms`, never a JSON list blob. The unique index is also why the
    // doc id hashes the lowercased query: `SearchHistoryStore.record` dedupes
    // case-insensitively, so "Allah" and "allah" are one query to the user and
    // must be one document here.
    //
    // Recency has to be synthesised: the store keeps an ordered
    // `List<String>` and no per-query timestamp, so `ranked_at_ms` is written as
    // `now - index` (index 0 = most recent) — the same stagger [pushHistory] uses
    // for entries that carry no timestamp of their own. That value moves on
    // every pass by construction, so "already settled" cannot be a byte
    // comparison of the whole row; [pushSearchHistory] compares the ORDER
    // instead, which is the part that actually matters, and writes nothing when
    // the cloud already carries it.
    //
    // Cap-vs-union trade-off, identical to `history` (see that section header):
    // two devices' histories can total more than [SEARCH_HISTORY_CAP], and the
    // cap wins — this device's own 15 are adopted ahead of the cloud's, and the
    // prune retires the rest. Deliberate, and the reason a pull may never be
    // gated on local emptiness.

    /**
     * Newest-first `search_history` list for [userId], capped at
     * [SEARCH_HISTORY_CAP]. The `user_id,ranked_at_ms` index serves this exact
     * order, and the store caps its own list at the same number, so one page is
     * the whole restore — no pagination walk needed (unlike [listForUser], whose
     // collections are unbounded).
     *
     * Null when the collection doesn't exist yet; any other failure throws into
     * the per-collection `runCollection` guard.
     */
    private suspend fun listSearchHistoryForUser(
        db: Databases,
        userId: String,
    ): List<io.appwrite.models.Document<Map<String, Any>>>? {
        return try {
            db.listDocuments(
                DB_ID,
                SEARCH_HISTORY,
                listOf(
                    Query.equal("user_id", userId),
                    Query.orderDesc("ranked_at_ms"),
                    Query.limit(SEARCH_HISTORY_CAP),
                ),
            ).documents
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                Log.w(TAG, "Collection '$SEARCH_HISTORY' not found; skipping.")
                null
            } else {
                throw e
            }
        }
    }

    /**
     * Union-pulls the cloud `search_history` docs into [SearchHistoryStore].
     *
     * No local-emptiness gate (class KDoc): queries typed while signed out are
     * adopted into the account, and one of them was enough to make a gated pull
     * skip the cloud forever while the paired [pushSearchHistory] deleted it.
     *
     * `importSnapshot` REPLACES the list, so the device's own queries are merged
     * in ahead of the cloud's by [mergeSearchQueries] — the list the store ends up
     * with is a superset of the local one, in the local order.
     *
     * Returns true when the cloud holds at least one document (including
     * documents that carried no usable `query` — the caller must then skip
     * pushing an empty local list rather than prune against an unreadable
     * snapshot). False when the collection is missing or empty.
     */
    private suspend fun pullSearchHistory(db: Databases, userId: String): Boolean {
        val docs = listSearchHistoryForUser(db, userId) ?: return false
        if (docs.isEmpty()) return false
        val fromCloud = docs.mapNotNull { doc ->
            (doc.data["query"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        }
        if (fromCloud.isEmpty()) {
            Log.w(
                TAG,
                "search_history pull: cloud holds ${docs.size} document(s) but none carried a " +
                    "query — local left untouched, push guarded."
            )
            return true
        }
        val local = SearchHistoryStore.exportSnapshot()
        val merged = mergeSearchQueries(local, fromCloud)
        if (merged == local) {
            Log.i(
                TAG,
                "search_history pull: nothing to merge — local already holds all " +
                    "${fromCloud.size} cloud query/queries."
            )
            return true
        }
        SearchHistoryStore.importSnapshot(merged)
        Log.i(
            TAG,
            "search_history pull: merged ${fromCloud.size} cloud query/queries into " +
                "${local.size} local (now ${SearchHistoryStore.recentQueries.value.size})."
        )
        return true
    }

    /**
     * Most-recent-first union of this device's [local] queries with the cloud's
     * [fromCloud], LOCAL FIRST.
     *
     * Local-first is the ordering rule because a query typed here is by
     * definition fresher than one this device has never seen — the cloud list is
     * another device's recency, already stamped. Queries are matched
     * case-insensitively (the doc id is, and `SearchHistoryStore.record` dedupes
     * that way), so a re-typed query keeps the local spelling instead of
     * consuming a second slot. The cap is applied by the store's own
     * normalization inside `importSnapshot`, not here, so a restored list and a
     * loaded one are indistinguishable.
     */
    private fun mergeSearchQueries(local: List<String>, fromCloud: List<String>): List<String> {
        val out = ArrayList<String>(local.size + fromCloud.size)
        val seen = HashSet<String>((local.size + fromCloud.size) * 2)
        for (query in local + fromCloud) {
            val clean = query.trim()
            if (clean.isEmpty()) continue
            if (seen.add(clean.lowercase())) out += clean
        }
        return out
    }

    private suspend fun pushSearchHistory(db: Databases, userId: String, cloudHasData: Boolean) {
        val local = SearchHistoryStore.exportSnapshot()
        if (local.isEmpty()) {
            if (cloudHasData) {
                Log.i(TAG, "pushSearchHistory skipped (local empty, cloud has data).")
            }
            return
        }
        val existing = listForUser(db, SEARCH_HISTORY, userId) ?: return
        // Settle check: `ranked_at_ms` is re-stamped every pass (see the section
        // header), so equality is decided on the ORDER the cloud would restore,
        // which is the only part of this push a user can observe. When it already
        // matches, the doc id sets match too — the id hashes the same
        // lowercased query — so the prune below would find nothing to delete and
        // the whole collection costs one list read.
        val cloudOrder = existing
            .sortedByDescending { asLong(it.data["ranked_at_ms"]) ?: Long.MIN_VALUE }
            .map { ((it.data["query"] as? String)?.trim().orEmpty()).lowercase() }
        if (cloudOrder == local.map { it.trim().lowercase() }) {
            Log.i(TAG, "search_history push: cloud already carries the local order; nothing to write.")
            return
        }
        val now = System.currentTimeMillis()
        val localDocIds = mutableSetOf<String>()
        var skipped = 0
        for ((index, raw) in local.withIndex()) {
            val query = raw.trim()
            // `query` is string 128 REQUIRED, and `record` caps the LIST at 15
            // without capping an entry's LENGTH — so an over-long search is
            // skipped here rather than allowed to fail the whole write and, with
            // it, the entire collection block.
            if (query.isEmpty() || query.length > SEARCH_HISTORY_QUERY_MAX_CHARS) {
                skipped++
                Log.w(
                    TAG,
                    "search_history push: skipping a ${query.length}-char query " +
                        "(column max $SEARCH_HISTORY_QUERY_MAX_CHARS)."
                )
                continue
            }
            val docId = searchHistoryDocId(userId, query)
            localDocIds += docId
            val rankedAt = now - index
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "query" to query,
                "ranked_at_ms" to rankedAt,
            )
            upsert(db, SEARCH_HISTORY, docId, data)
        }
        if (skipped > 0) {
            Log.w(TAG, "search_history push: $skipped local quer(y/ies) did not fit the column.")
        }
        // Sound because [pullSearchHistory] ran first in the same block and unioned
        // the cloud in: after it, local ⊇ cloud, so a doc missing from
        // `localDocIds` is one this device really has dropped.
        for (doc in existing) {
            if (doc.id in localDocIds) continue
            try {
                db.deleteDocument(DB_ID, SEARCH_HISTORY, doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "delete stale search_history doc ${doc.id} failed", e)
            }
        }
    }

    // ------------------------------------------------- editorial catalog
    //
    // Read-only pull of the public catalog (`playlists` with
    // `is_public = true`, limit 20 + `playlist_items` per playlist, limit 50
    // ordered by `position`) into [EditorialRepository]. Mirrors the public
    // `reciters`/`reciter_stats` read pattern (no user filter, no login
    // requirement on the client — the server still requires an authenticated
    // user per the collection permissions, so signed-out calls fail into the
    // best-effort null below). Never throws: on ANY failure the last-good
    // cache is kept so offline keeps showing the previous shelf.

    /**
     * Pulls public playlists + their items (see section header). Updates
     * [EditorialRepository] only when at least one usable playlist parses —
     * an empty/error result keeps the previous shelf. Never throws.
     */
    actual suspend fun refreshEditorial() {
        val ctx = appContext ?: return
        if (!AppwriteConfig.isConfigured()) return
        EditorialRepository.setRefreshing(true)
        try {
            withContext(Dispatchers.IO) {
                val db = databases(ctx)
                val docs = try {
                    db.listDocuments(
                        DB_ID,
                        PLAYLISTS,
                        listOf(Query.equal("is_public", true), Query.limit(20)),
                    ).documents
                } catch (e: Exception) {
                    if (!isNotFound(e)) Log.w(TAG, "editorial playlists list failed", e)
                    return@withContext
                }
                val out = docs.mapNotNull { doc ->
                    parseEditorialPlaylist(db, doc.id, doc.data)
                }
                if (out.isNotEmpty()) {
                    EditorialRepository.setPlaylists(out)
                    Log.i(TAG, "editorial pull: adopted ${out.size} public playlist(s).")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "editorial refresh failed", e)
        } finally {
            EditorialRepository.setRefreshing(false)
        }
    }

    private suspend fun parseEditorialPlaylist(
        db: Databases,
        docId: String,
        data: Map<String, Any?>,
    ): EditorialPlaylist? {
        val title = (data["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val items = try {
            db.listDocuments(
                DB_ID,
                PLAYLIST_ITEMS,
                listOf(
                    Query.equal("playlist_id", docId),
                    Query.orderAsc("position"),
                    Query.limit(50),
                ),
            ).documents
        } catch (e: Exception) {
            if (!isNotFound(e)) Log.w(TAG, "editorial items list failed for '$docId'", e)
            emptyList()
        }.mapNotNull { item ->
            val slug = (item.data["reciter_slug"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val surahId = asInt(item.data["surah_id"])?.takeIf { it in 1..114 }
                ?: return@mapNotNull null
            EditorialPlaylistItem(
                reciterSlug = slug,
                surahId = surahId,
                ayahFrom = asInt(item.data["ayah_from"])?.coerceAtLeast(0) ?: 0,
                ayahTo = asInt(item.data["ayah_to"])?.coerceAtLeast(0) ?: 0,
                position = asInt(item.data["position"])?.coerceAtLeast(0) ?: 0,
            )
        }.sortedBy { it.position }
        return EditorialPlaylist(
            id = docId,
            title = title,
            description = (data["description"] as? String)?.trim().orEmpty(),
            coverUrl = (data["cover_url"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            items = items,
        )
    }

    // --------------------------------------------------------------- routines
    //
    // v1: push-only. Each PUBLIC routine is published to `playlists` /
    // `playlist_items` (playlist doc id "rtn-<routineId>"); routines flipped
    // back to private (or deleted locally) have their previously published
    // docs + items removed. No pull in v1: public catalog browsing is a future
    // screen, so cloud state never writes back into CustomRoutinesStore.
    // Skipped when signed out: syncNow returns DISABLED before any network.
    // Private routines are never published (upsert runs for public ones only);
    // the delete pass below only removes stale cloud docs this app owns.

    private suspend fun pushRoutines(db: Databases, userId: String) {
        val local = CustomRoutinesStore.routines.value
        val publicRoutines = local.filter { it.isPublic }
        val publicDocIds = publicRoutines.map { routineDocId(it.id) }.toSet()

        // Previously published docs owned by this user (client-side prefix
        // filter: only docs this app created, id starting "rtn-").
        val owned = listPlaylistsForOwner(db, userId) ?: return
        for (doc in owned) {
            if (doc.id.startsWith(ROUTINE_DOC_PREFIX) && doc.id !in publicDocIds) {
                deletePlaylistWithItems(db, doc.id)
            }
        }

        for (routine in publicRoutines) {
            val playlistDocId = routineDocId(routine.id)
            val playlistData: Map<String, Any?> = mapOf(
                "owner_id" to userId,
                "title" to routine.title,
                "description" to routine.description,
                "is_public" to true,
                "cover_url" to "",
            )
            upsert(db, PLAYLISTS, playlistDocId, playlistData)
            // Replace items wholesale so reorders/removals converge.
            val existingItems = listPlaylistItems(db, playlistDocId) ?: continue
            for (item in existingItems) {
                try {
                    db.deleteDocument(DB_ID, PLAYLIST_ITEMS, item.id)
                } catch (e: Exception) {
                    Log.e(TAG, "delete stale playlist item ${item.id} failed", e)
                }
            }
            routine.items.forEachIndexed { index, item ->
                val itemData: Map<String, Any?> = mapOf(
                    "playlist_id" to playlistDocId,
                    "position" to index,
                    "reciter_slug" to item.reciterSlug,
                    "surah_id" to item.surahId,
                    "ayah_from" to 0,
                    "ayah_to" to 0,
                )
                upsert(db, PLAYLIST_ITEMS, routineItemDocId(playlistDocId, index), itemData)
            }
        }
    }

    private suspend fun listPlaylistsForOwner(
        db: Databases,
        userId: String,
    ): List<io.appwrite.models.Document<Map<String, Any>>>? {
        return try {
            db.listDocuments(DB_ID, PLAYLISTS, listOf(Query.equal("owner_id", userId))).documents
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                Log.w(TAG, "Collection '$PLAYLISTS' not found; skipping.")
                null
            } else {
                throw e
            }
        }
    }

    private suspend fun listPlaylistItems(
        db: Databases,
        playlistId: String,
    ): List<io.appwrite.models.Document<Map<String, Any>>>? {
        return try {
            db.listDocuments(DB_ID, PLAYLIST_ITEMS, listOf(Query.equal("playlist_id", playlistId))).documents
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                Log.w(TAG, "Collection '$PLAYLIST_ITEMS' not found; skipping.")
                null
            } else {
                throw e
            }
        }
    }

    private suspend fun deletePlaylistWithItems(db: Databases, playlistDocId: String) {
        val items = listPlaylistItems(db, playlistDocId)
        for (item in items.orEmpty()) {
            try {
                db.deleteDocument(DB_ID, PLAYLIST_ITEMS, item.id)
            } catch (e: Exception) {
                Log.e(TAG, "delete stale playlist item ${item.id} failed", e)
            }
        }
        try {
            db.deleteDocument(DB_ID, PLAYLISTS, playlistDocId)
        } catch (e: Exception) {
            Log.e(TAG, "delete stale playlist $playlistDocId failed", e)
        }
    }

    private fun routineDocId(routineId: String): String =
        "$ROUTINE_DOC_PREFIX${sanitizeId(routineId)}".take(36)

    private fun routineItemDocId(playlistDocId: String, position: Int): String =
        "$playlistDocId-$position".take(36)

    // ------------------------------------------ routine backups (private)
    //
    // Private backup of ALL routines (public + private) to `routine_backups`
    // (`routine_backups{user_id,routine_id,routine_json,updated_at}`, doc id
    // `rtn-<routineId>` via [routineDocId]). This is separate from the public
    // `playlists`/`playlist_items` publish flow above, which stays unchanged:
    // private routines must never leak into the public catalog.
    //
    // The pull restores via [CustomRoutinesStore.mergeJson], which unions by
    // routine id, preserves the ids it decodes, and skips malformed entries
    // per-item. It replaced an empty-gated `restoreAllJson`: that gate meant a
    // device which had adopted one signed-out routine never restored the rest
    // of its cloud backups at all.
    //
    // ## KNOWN GAP (store-side, unchanged) + what this side does about it
    //
    // This is the one remaining `*IfEmpty` gate, and it is a real data-loss
    // path: a routine created while signed out is adopted into the new account,
    // so the store is no longer empty, the gate skips the cloud restore, and
    // [pushRoutineBackups] would then prune every cloud backup whose
    // `routine_id` the local store lacks.
    //
    // The id-preserving merge cannot be written from this file, and the reason
    // is more specific than "the store mints ids":
    // [CustomRoutinesStore.restoreAllJson] DOES preserve the ids it decodes - it
    // is gated purely on the store being empty (`if (_routines.value.isNotEmpty())
    // return 0`) - and the other two entry points cannot stand in for it:
    // [CustomRoutinesStore.update] only REPLACES an entry whose id is already
    // present (it maps over the current list; it never inserts), and
    // [CustomRoutinesStore.create] inserts under a freshly minted
    // `"rtn-<now>-<rand>"` id. Re-adding cloud routines through `create` would
    // preserve the CONTENT but change every id, so the paired push would write
    // new docs and prune the originals - a bigger regression than the one being
    // fixed. The clean fix is one store-side method (an id-preserving `mergeJson`
    // that unions into a non-empty store, the routine-side twin of
    // [SchedulesStore.restoreAllFromCloud]); that is the store owner's call, so
    // the gap stays documented here.
    //
    // What this file does do is make the push's DELETE half conditional on
    // proof instead of assuming the restore happened: the pull hands its push
    // the set of `routine_id`s it read from the cloud, and the prune only runs
    // when every one of them is present in the local store. When the store was
    // non-empty at pull time (the gap) that check fails, the deletes are
    // quarantined and logged, and the uploads still go out - so the destructive
    // case is unreachable from here. The cost is confined to convergence: while
    // any cloud backup is unmatched, backups of routines deleted on THIS device
    // stay on the cloud until some pass has a local store that covers them all.
    // Inert documents, never lost routines.

    /**
     * Outcome of [pullRoutineBackupsIfEmpty], carried into the paired push.
     *
     * @param cloudHasData true when the cloud holds at least one backup document.
     * @param cloudIds every `routine_id` the pull saw in the cloud, so the push
     *   can tell a COMPLETE restore (prune allowed - and a no-op besides, since
     *   the restore is id-preserving) from a skipped one (prune quarantined).
     */
    private class RoutineBackupPull(
        val cloudHasData: Boolean,
        val cloudIds: Set<String>,
    )

    /**
     * Restores the cloud `routine_backups` snapshot into an empty local
     * [CustomRoutinesStore]. Reports whether the cloud holds any backup (caller
     * must then skip pushing, otherwise the still-empty local state would
     * overwrite it) and which `routine_id`s it saw, which is what arms the
     * push's prune quarantine - see the KNOWN GAP above.
     */
    private suspend fun pullRoutineBackupsIfEmpty(
        db: Databases,
        userId: String,
    ): RoutineBackupPull {
        val docs = listForUser(db, ROUTINE_BACKUPS, userId)
            ?: return RoutineBackupPull(cloudHasData = false, cloudIds = emptySet())
        val cloudIds = LinkedHashSet<String>()
        val rawJsons = ArrayList<String>(docs.size)
        for (doc in docs) {
            val routineId = (doc.data["routine_id"] as? String)?.trim()
            if (!routineId.isNullOrEmpty()) cloudIds += routineId
            val raw = (doc.data["routine_json"] as? String)?.takeIf { it.isNotBlank() }
            if (raw != null) rawJsons += raw
        }
        if (rawJsons.isEmpty()) {
            // Every doc is payload-less, so nothing here was ever restorable.
            // These are NOT counted as covered: they cannot be quarantined by a
            // store-side merge either, and an unreadable row is a prune
            // candidate, not a reason to disable the prune forever.
            return RoutineBackupPull(cloudHasData = false, cloudIds = emptySet())
        }
        // `mergeJson` unions by routine id and preserves the ids it decodes, so
        // it is correct whether or not the local store already holds routines.
        // `restoreAllJson` was store-gated on an EMPTY store, which is why this
        // pull used to skip a device that had adopted a signed-out routine - and
        // why the paired push had to quarantine its prune.
        val restored = CustomRoutinesStore.mergeJson(rawJsons)
        if (restored > 0) {
            Log.i(TAG, "routine_backups pull: restored $restored routine(s) from cloud.")
        } else {
            Log.i(
                TAG,
                "routine_backups pull: cloud holds ${rawJsons.size} backup(s) but none parseable" +
                    " - keeping local empty, push skipped."
            )
        }
        return RoutineBackupPull(cloudHasData = true, cloudIds = cloudIds)
    }

    private suspend fun pushRoutineBackups(
        db: Databases,
        userId: String,
        pull: RoutineBackupPull,
    ) {
        val local = CustomRoutinesStore.routines.value
        if (local.isEmpty() && pull.cloudHasData) {
            // Fresh login whose cloud snapshot couldn't be applied locally yet:
            // don't overwrite it with nothing.
            Log.i(TAG, "pushRoutineBackups skipped (local empty, cloud has data).")
            return
        }
        val existing = listForUser(db, ROUTINE_BACKUPS, userId) ?: return
        val localIds = local.mapTo(mutableSetOf()) { it.id }
        // Prune only on PROOF that the local set covers the whole cloud
        // snapshot. The pull now merges with `mergeJson`, which preserves ids,
        // so after a successful pass every cloud `routine_id` has a local
        // counterpart and this holds. It stays a proof rather than an
        // assumption because a missed delete is recoverable and a deleted
        // backup is not.
        val unmatched = pull.cloudIds.filterNot { it in localIds }
        val canPrune = unmatched.isEmpty()
        if (!canPrune) {
            Log.w(
                TAG,
                "routine_backups prune quarantined: ${unmatched.size} cloud backup(s) have no " +
                    "id-preserved local counterpart - uploads continue, deletes do not."
            )
        }
        if (canPrune) {
            for (doc in existing) {
                val routineId = doc.data["routine_id"] as? String ?: continue
                if (routineId !in localIds) {
                    try {
                        db.deleteDocument(DB_ID, ROUTINE_BACKUPS, doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "delete stale routine backup ${doc.id} failed", e)
                    }
                }
            }
        }
        // CustomRoutine is @Serializable: full fidelity via JSON string.
        for (routine in local) {
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "routine_id" to routine.id,
                "routine_json" to json.encodeToString(routine),
                "updated_at" to System.currentTimeMillis(),
            )
            upsert(db, ROUTINE_BACKUPS, routineDocId(routine.id), data)
        }
    }

    // ------------------------------------------------------------ user prefs
    //
    // Onboarding goals only: `user_prefs{user_id,goal,daily_minutes}`, single
    // doc per user (docId = userId). Pull-if-empty adopts the cloud goal +
    // daily minutes via [OnboardingStore.setGoal]/[setDailyGoalMinutes]; push
    // uploads them. Onboarding seen/done/step flags are NEVER synced (they
    // stay local-only; syncing them would replay or suppress onboarding on a
    // fresh device).

    /**
     * Adopts the cloud `user_prefs` doc when local goals are still at fresh
     * defaults (`goal == null`, `dailyGoalMinutes == 15`). Never overwrites a
     * locally chosen goal or customized minutes.
     */
    private suspend fun pullUserPrefsIfEmpty(db: Databases, userId: String) {
        if (OnboardingStore.goal.value != null) return
        if (OnboardingStore.dailyGoalMinutes.value != 15) return
        val doc = getDocument(db, USER_PREFS, userId) ?: return
        val data = doc.data
        val cloudGoal = (data["goal"] as? String)?.trim().orEmpty()
        val cloudMinutes = asInt(data["daily_minutes"])
        if (cloudGoal.isEmpty() && cloudMinutes == null) return
        if (cloudGoal.isNotEmpty()) {
            OnboardingStore.setGoal(cloudGoal)
        }
        if (cloudMinutes != null) {
            // Setter coerces to 5..180.
            OnboardingStore.setDailyGoalMinutes(cloudMinutes)
        }
        Log.i(
            TAG,
            "user_prefs pull: adopted goal='${cloudGoal.ifEmpty { "<none>" }}'" +
                " dailyMinutes=${OnboardingStore.dailyGoalMinutes.value}."
        )
    }

    private suspend fun pushUserPrefs(db: Databases, userId: String) {
        val data: Map<String, Any?> = mapOf(
            "user_id" to userId,
            // `goal` is optional server-side: blank = no goal chosen yet
            // (pull reads blank back as null).
            "goal" to (OnboardingStore.goal.value ?: ""),
            "daily_minutes" to OnboardingStore.dailyGoalMinutes.value,
        )
        upsert(db, USER_PREFS, userId, data)
    }

    // ------------------------------------------------------- device telemetry
    //
    // Minimal per-device usage snapshot for the admin dashboard:
    // `telemetry{user_id,device_id,platform,app_version,total_seconds,
    // updated_at}` (USER_RW, DB `ghais`, see appwrite/collections.json). One
    // doc per user+device, id `t-<userId>-<deviceId>` (truncated to 36 chars
    // like every other doc-id scheme here). Throttled to at most one upload
    // per 24h per device; runs only after a fully successful syncNow pass.
    // Create = the shared [upsert] (create, then update on 409). ANY failure
    // is swallowed (Log.w) so telemetry never breaks sync — this covers the
    // unprovisioned-collection/404 case the same way runCollection skips.

    /**
     * Upserts the telemetry doc when the 24h per-device throttle has expired.
     * The last-upload epoch is keyed per telemetry doc id (user+device), so
     * an account switch still uploads for the new account. The throttle is
     * stamped only on success, so a failed attempt retries on the next sync.
     * Never throws.
     */
    private suspend fun pushTelemetryIfDue(
        db: Databases,
        userId: String,
        ctx: android.content.Context,
    ) {
        try {
            // Device identity: the canonical persisted UUID from
            // UserUsageRepository.deviceId() (owner-independent
            // `ghais_device_id` key + in-memory cache). Deliberately NOT
            // Settings.Secure.ANDROID_ID (an older draft used it; the stats
            // v2 sync standardised on this UUID).
            val devId = UserUsageRepository.deviceId().trim()
            if (userId.isBlank() || devId.isEmpty()) return
            val docId = "t-${sanitizeId(userId)}-${sanitizeId(devId)}".take(36)
            val prefKey = TELEMETRY_PREF_PREFIX + docId
            val now = System.currentTimeMillis()
            val last = try {
                telemetryPrefs.getLong(prefKey, 0L)
            } catch (_: Exception) {
                0L
            }
            if (last > 0L && now - last < TELEMETRY_THROTTLE_MS) return
            // Payload total: the live aggregate from UserUsageRepository.stats
            // (KEY_TOTAL_SECONDS-backed `totalSecondsListened`, accumulated in
            // recordListeningTime — same value pushStats uploads).
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "device_id" to devId,
                "platform" to "android",
                "app_version" to appVersion(ctx),
                "total_seconds" to UserUsageRepository.stats.value.totalSecondsListened,
                "updated_at" to now,
            )
            upsert(db, TELEMETRY, docId, data)
            try {
                telemetryPrefs.putLong(prefKey, now)
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            Log.w(TAG, "telemetry upload skipped", e)
        }
    }

    /**
     * App version for the telemetry payload. No version accessor existed in
     * shared code (androidApp `versionName = "1.0"` lives in the app-module
     * gradle config, unreachable from `:shared`), so it is read via
     * PackageManager from the same [appContext] pattern the cookie jar
     * already uses. Returns "unknown" when unreadable. Never throws.
     */
    private fun appVersion(ctx: android.content.Context): String {
        return try {
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0)?.versionName
                ?.trim()?.takeIf { it.isNotEmpty() } ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * 404-safe single-document read (single-doc-per-user collections:
     * [PLAYBACK], [STATS]). Null when the doc/collection doesn't exist yet;
     * other failures throw into the per-collection `runCollection` guard.
     */
    private suspend fun getDocument(
        db: Databases,
        collection: String,
        documentId: String,
    ): io.appwrite.models.Document<Map<String, Any>>? {
        return try {
            db.getDocument(DB_ID, collection, documentId)
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                Log.w(TAG, "Document '$collection/$documentId' not found; skipping.")
                null
            } else {
                throw e
            }
        }
    }

    /** Lenient number coercion: Appwrite decodes numerics as [Number], but tolerate numeric strings. */
    private fun asLong(v: Any?): Long? = when (v) {
        is Number -> v.toLong()
        is String -> v.trim().toLongOrNull() ?: v.trim().toDoubleOrNull()?.toLong()
        else -> null
    }

    private fun asInt(v: Any?): Int? = when (v) {
        is Number -> v.toInt()
        is String -> v.trim().toIntOrNull() ?: v.trim().toDoubleOrNull()?.toInt()
        else -> null
    }

    /** Lenient double coercion (Appwrite decodes `percent` float as [Number]). */
    private fun asDouble(v: Any?): Double? = when (v) {
        is Number -> v.toDouble()
        is String -> v.trim().toDoubleOrNull()
        else -> null
    }

    /** Same epoch-day check (`millis/86400000`, mirroring UserUsageRepository's day math). */
    private fun sameEpochDay(aMs: Long, bMs: Long): Boolean {
        if (aMs <= 0L || bMs <= 0L) return false
        return aMs / 86_400_000L == bMs / 86_400_000L
    }

    /**
     * Page size for [listForUser] (same 100-doc page [fetchReciterCatalog] uses).
     */
    private const val USER_PAGE_SIZE = 100

    /**
     * Hard ceiling on how many docs one [listForUser] walk will collect, ~50
     * pages. Far above any real per-user count in these collections (favourites,
     * follows, khatma plans, schedules, routine backups), so it only exists to
     * stop a server that ignores `offset` from spinning forever. Hitting it is
     * logged, and — because the walk is read-only — the affected pass simply
     * sees a window of the collection: the prune loops below only ever consider
     * the docs they were handed, so documents past the ceiling are never
     * deleted, only not refreshed.
     */
    private const val USER_DOCS_MAX = 5000

    /**
     * Every `user_id == userId` document in [collection], PAGINATED.
     *
     * Pagination is not an optimisation here, it is a correctness requirement:
     * this used to pass only `Query.equal("user_id", userId)`, so Appwrite's
     * bounded default page silently truncated the read. A user with more
     * favourites/follows/plans/schedules/backups than one page restored only
     * that many — and because the restore then made the local list non-empty,
     * the `*IfEmpty` gate never re-ran and the rest was never recovered. Worse,
     * the PRUNE loops below converge the cloud to the exact local set, so every
     * document the truncated read never saw was deleted as "stale".
     *
     * Failure semantics are deliberately unchanged and all-or-nothing: a 404
     * (collection not provisioned) returns null exactly as before, and any
     * other error THROWS into the per-collection `runCollection` guard. A
     * partial read therefore never reaches a caller — and a partial read must
     * never reach a prune, because "not in this list" is only evidence of
     * absence when the list is the whole collection.
     */
    private suspend fun listForUser(
        db: Databases,
        collection: String,
        userId: String,
    ): List<io.appwrite.models.Document<Map<String, Any>>>? {
        val out = ArrayList<io.appwrite.models.Document<Map<String, Any>>>(USER_PAGE_SIZE)
        var offset = 0
        while (true) {
            val page = try {
                db.listDocuments(
                    DB_ID,
                    collection,
                    listOf(
                        Query.equal("user_id", userId),
                        Query.limit(USER_PAGE_SIZE),
                        Query.offset(offset),
                    ),
                ).documents
            } catch (e: AppwriteException) {
                if (e.code == 404) {
                    Log.w(TAG, "Collection '$collection' not found; skipping.")
                    return null
                }
                throw e
            }
            if (page.isEmpty()) break
            out.addAll(page)
            if (page.size < USER_PAGE_SIZE) break
            offset += page.size
            if (out.size >= USER_DOCS_MAX) {
                Log.w(
                    TAG,
                    "Collection '$collection' list for this user hit the $USER_DOCS_MAX cap;" +
                        " the prune for this collection is now working on a partial view."
                )
                break
            }
        }
        return out
    }

    private suspend fun upsert(
        db: Databases,
        collection: String,
        documentId: String,
        data: Map<String, Any?>,
    ) {
        try {
            db.createDocument(DB_ID, collection, documentId, data)
        } catch (e: AppwriteException) {
            if (e.code == 409) {
                db.updateDocument(DB_ID, collection, documentId, data)
            } else {
                throw e
            }
        }
    }

    /** Stable doc-ID schemes per the SyncEngine.kt contract (max 36 chars). */
    private fun sanitizeId(s: String): String =
        s.replace(Regex("[^A-Za-z0-9-_]"), "-")

    private const val HEX_DIGITS = "0123456789abcdef"

    /**
     * Deterministic [chars]-char (even, ≤ 64) SHA-256 hex prefix of the FULL
     * [value], for doc ids that must fit Appwrite's 36-char limit without
     * truncating their inputs.
     *
     * Why a hash and not `sanitizeId(...) + take(36)`: `takeslice` on a slug
     * silently merges any two slugs sharing a ~32-char prefix, and prefixing
     * with a truncated userId does the same across users. Hashing the whole
     * input makes the id a function of every character, so two ids differ
     * unless their full inputs share a digest prefix — 24 hex chars is 96 bits,
     * i.e. birthday-safe across every account size this app will ever see.
     */
    private fun shortHash(value: String, chars: Int = 24): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(chars)
        var i = 0
        while (sb.length < chars) {
            val b = digest[i].toInt() and 0xff
            sb.append(HEX_DIGITS[b ushr 4]).append(HEX_DIGITS[b and 0x0f])
            i++
        }
        return sb.toString()
    }

    /**
     * Like doc id: `"fav-" + sha256("<userId>|<slug>|<surahId>|<ayahNo>")[0..24)`
     * (28 chars). Hashing the full tuple keeps the id unique per user even
     * though `likes` is `documentSecurity: true` (two users liking the same
     * ayah must not contend for one doc), and no longer truncates long slugs.
     */
    private fun likeDocId(userId: String, slug: String, surahId: Int, ayahNo: Int): String =
        "fav-${shortHash("$userId|$slug|$surahId|$ayahNo")}"

    /**
     * Follow doc id: `"fol-" + sha256("<userId>|<slug>")[0..24)` (28 chars).
     * The `userId` is part of the hashed input on purpose: with
     * `documentSecurity: true` on `follows` a slug-only id means the second
     * user to follow a reciter gets 409 on create and then 401/403 on the
     * `upsert` fallback, which rethrows and fails the whole sync pass — that
     * user's follow would never reach the cloud.
     */
    private fun followDocId(userId: String, slug: String): String =
        "fol-${shortHash("$userId|$slug")}"

    private fun scheduleDocId(userId: String, scheduleId: String): String =
        scheduleId.take(36)

    private fun khatmaDocId(planId: String): String =
        "khatma-${sanitizeId(planId)}".take(36)

    /**
     * Hifz doc id: `"hifz-" + sha256("<userId>|<surahId>|<ayahNo>")[0..24)`
     * (29 chars) — one document per (user, ayah), which is exactly the grain of
     * the collection's `user_surah_ayah_idx` key index and of the store's own
     * `mastery_{surahId}_{ayahNo}` key.
     *
     * `userId` is hashed in, as everywhere else in this file, because
     * `hifz_mastery` is `documentSecurity: true`: a coordinate-only id would make
     * the second user to memorise 2:255 get 409 on create and then 401/403 on
     * the `upsert` fallback, which rethrows and fails the whole collection
     * block. The status is an ATTRIBUTE, not part of the id, so cycling an ayah
     * updates one document in place instead of creating and pruning a new one on
     * every tap.
     */
    private fun hifzMasteryDocId(userId: String, surahId: Int, ayahNo: Int): String =
        "hifz-${shortHash("$userId|$surahId|$ayahNo")}"

    /**
     * Search-history doc id: `"shr-" + sha256("<userId>|<query lowercased>")[0..24)`
     * (28 chars) — one document per (user, query), matching the collection's
     * `user_query_unique` unique index.
     *
     * Hashed on the LOWERCASED query because `SearchHistoryStore.record` dedupes
     * case-insensitively and stores the spelling the user last typed, so "Allah"
     * and "allah" are one query to the user; keying the id on the raw spelling
     * would let the unique index hold two documents for one query whenever two
     * devices disagreed about its capitalisation. `userId` is in the hash for the
     * same `documentSecurity` reason as every other id here. `ranked_at_ms` is an
     * attribute, so repeating a search re-stamps the existing row instead of
     * appending a duplicate.
     */
    private fun searchHistoryDocId(userId: String, query: String): String =
        "shr-${shortHash("$userId|${query.trim().lowercase()}")}"

    private fun jsonString(s: String): String = buildString {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }
}
