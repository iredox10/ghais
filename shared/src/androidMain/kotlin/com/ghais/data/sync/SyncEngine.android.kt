package com.ghais.data.sync

import android.util.Log
import com.ghais.data.auth.AppwriteConfig
import com.ghais.data.auth.AuthRepository
import com.ghais.data.auth.PersistentCookieJar
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.SchedulesStore
import com.ghais.domain.model.Reciter
import com.ghais.data.repository.UserUsageRepository
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
 * collection pull-if-empty + push-always, each in its own try/catch (first
 * failure wins for [lastError]). `lastSyncedAt` is set on full success only.
 * All network runs on Dispatchers.IO. Failures go to Log.e("GhaisSync").
 */
actual object SyncEngine {

    private const val TAG = "GhaisSync"

    private const val PLAYLISTS = "playlists"
    private const val PLAYLIST_ITEMS = "playlist_items"
    private const val ROUTINE_DOC_PREFIX = "rtn-"

    // Database/collection ids come from the sibling-owned SyncModels.kt
    // (same package: DB_ID, LIKES, PLAYBACK, FOLLOWS, STATS, SCHEDULES).
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
                pullLikesIfEmpty(db, userId)
                pushLikes(db, userId)
            }
            runCollection(FOLLOWS) {
                pullFollowsIfEmpty(db, userId)
                pushFollows(db, userId)
            }
            // Best-effort follower-count refresh from public reciter_stats
            // (covers login pull + debounced push after follow/unfollow; the
            // server-side increment via Function lands on a later refresh).
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
                val cloudHasData = pullStatsIfEmpty(db, userId)
                pushStats(db, userId, cloudHasData)
            }
            runCollection(SCHEDULES) { pushSchedules(db, userId) }
            runCollection(PLAYLISTS) { pushRoutines(db, userId) }
        }
        if (firstError == null) {
            _lastError.value = null
            _lastSyncedAt.value = System.currentTimeMillis()
            _status.value = SyncStatus.IDLE
        } else {
            _lastError.value = firstError
            _status.value = SyncStatus.ERROR
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

    private suspend fun pullLikesIfEmpty(db: Databases, userId: String) {
        if (FavoritesStore.favoriteTracks.value.isNotEmpty()) return
        val docs = listForUser(db, LIKES, userId) ?: return
        FavoritesStore.clear()
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
            val playable = buildPlayableLike(rawSlug, surahId, ayahNo)
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
                        audioUrl = "appwrite://likes/${doc.id}",
                    )
                )
            }
        }
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

    private suspend fun pushLikes(db: Databases, userId: String) {
        val local = FavoritesStore.favoriteTracks.value
        val existing = listForUser(db, LIKES, userId) ?: return
        val localKeys = local.map { likeKey(it.surahId, it.ayahNo) }.toSet()
        // Per-pass flag: cleared on the first unknown-attribute rejection so the
        // rest of this pass goes slug-less; next pass retries with the slug.
        var includeReciterSlug = true
        for (doc in existing) {
            val data = doc.data
            val key = likeKey(
                (data["surah_id"] as? Number)?.toInt() ?: continue,
                (data["ayah_no"] as? Number)?.toInt() ?: 0,
            )
            if (key !in localKeys) {
                try {
                    db.deleteDocument(DB_ID, LIKES, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "delete stale like ${doc.id} failed", e)
                }
            }
        }
        for (track in local) {
            // `reciter_slug` (string 64, optional) is sent when the cloud
            // collection has it; when the sibling hasn't added the attribute
            // yet the server rejects unknown attributes, so fall back to a
            // slug-less upsert once (flag) and keep syncing the other fields.
            val full: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "surah_id" to track.surahId,
                "ayah_no" to track.ayahNo,
                "level" to "like",
                "reciter_slug" to track.reciterSlug,
            )
            val docId = likeDocId(userId, track.reciterSlug, track.surahId, track.ayahNo)
            if (includeReciterSlug) {
                try {
                    upsert(db, LIKES, docId, full)
                    continue
                } catch (e: Exception) {
                    if (!isUnknownAttribute(e, "reciter_slug")) throw e
                    Log.w(TAG, "likes.reciter_slug not provisioned yet; pushing without it.")
                    includeReciterSlug = false
                }
            }
            upsert(db, LIKES, docId, full - "reciter_slug")
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

    // ---------------------------------------------------------------- follows

    private suspend fun pullFollowsIfEmpty(db: Databases, userId: String) {
        if (FollowStore.followedSlugs.value.isNotEmpty()) return
        val docs = listForUser(db, FOLLOWS, userId) ?: return
        // Bulk replace: no per-slug count refresh (syncNow refreshes once).
        val slugs = docs.mapNotNull { it.data["reciter_slug"] as? String }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        FollowStore.setAll(slugs)
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
    // likes_count,updated_at}`, doc id = slug). Single-doc read per slug —
    // never an N× fan-out over `follows`. Null on 404/any failure so the UI
    // hides counts when unknown. Client never writes these docs.

    /**
     * Reads `followers_count` for [reciterSlug] from `reciter_stats`
     * (direct doc id=slug first, `slug`-equal query limit 1 as fallback).
     */
    suspend fun followerCount(reciterSlug: String): Long? =
        reciterStat(reciterSlug, "followers_count")

    /** Reads `likes_count` for [reciterSlug] from `reciter_stats`. */
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
                val data: Map<String, Any?>? = try {
                    db.getDocument(DB_ID, RECITER_STATS, slug).data
                } catch (e: Exception) {
                    if (!isNotFound(e)) {
                        Log.w(TAG, "reciter_stats get '$slug' failed; trying query", e)
                    }
                    try {
                        db.listDocuments(
                            DB_ID,
                            RECITER_STATS,
                            listOf(Query.equal("slug", slug), Query.limit(1)),
                        ).documents.firstOrNull()?.data
                    } catch (e2: Exception) {
                        if (!isNotFound(e2)) {
                            Log.w(TAG, "reciter_stats query '$slug' failed", e2)
                        }
                        null
                    }
                }
                (data?.get(field) as? Number)?.toLong()
            } catch (_: Exception) {
                null
            }
        }

    // --------------------------------------------------------------- playback
    //
    // Pull-if-empty + push-always (push skips when local is still empty but
    // cloud has data, so a fresh login never clobbers its restore source with
    // an empty snapshot).

    /**
     * Pulls the cloud `playback_state` doc (docId = [userId]) when the local
     * player is empty, and rebuilds the [TrackItem] for a paused restore.
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
     *
     * Restore is METADATA-ONLY (parse + validate + log): [AudioEngine] has no
     * paused-load path — only [AudioEngine.playTrack]/[AudioEngine.playQueue]
     * (both autoplay via `PlayerBridge.play`) plus `pause`/`seekTo` — so there
     * is nothing that loads a track + seeks while staying paused. Follow-up:
     * add `AudioEngine.prepareTrack(track, positionMs)` (load + seek, paused)
     * and call it here instead of just logging.
     */
    private suspend fun pullPlaybackIfEmpty(db: Databases, userId: String): Boolean {
        if (AudioEngine.currentTrack.value != null) return false
        val doc = getDocument(db, PLAYBACK, userId) ?: return false
        val data = doc.data
        val ref = (data["current_ref"] as? String)?.trim().orEmpty()
        if (ref.isEmpty()) return false // idle snapshot the pusher wrote; nothing to restore
        val positionMs = asLong(data["position_ms"])?.coerceAtLeast(0L) ?: 0L
        val updatedAt = asLong(data["updated_at"])
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
        // Metadata-only: validated rebuild (no autoplay path to load it paused).
        Log.i(
            TAG,
            "playback pull: cloud '${reciter.slug}/$surahId' ayah=$ayahNo pos=${positionMs}ms" +
                (if (updatedAt != null) " updatedAt=$updatedAt" else "") +
                " — metadata validated only; AudioEngine has no paused-load API yet."
        )
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

    private suspend fun pushStats(db: Databases, userId: String) {
        val s = UserUsageRepository.stats.value
        val data: Map<String, Any?> = mapOf(
            "user_id" to userId,
            "days_streak" to s.daysStreak,
            "minutes_today" to s.minutesToday,
            "unique_reciters" to s.uniqueRecitersCount,
            "unique_surahs" to s.uniqueSurahsCount,
            "total_seconds" to s.totalSecondsListened,
            "updated_at" to System.currentTimeMillis(),
        )
        upsert(db, STATS, userId, data)
    }

    // --------------------------------------------------------------- schedules

    private suspend fun pushSchedules(db: Databases, userId: String) {
        val local = SchedulesStore.schedules.value
        val existing = listForUser(db, SCHEDULES, userId) ?: return
        val localIds = local.map { it.id }.toSet()
        for (doc in existing) {
            val scheduleId = doc.data["schedule_id"] as? String ?: doc.id
            if (scheduleId !in localIds) {
                try {
                    db.deleteDocument(DB_ID, SCHEDULES, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "delete stale schedule ${doc.id} failed", e)
                }
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

    // ---------------------------------------------------------------- helpers

    private suspend fun listForUser(
        db: Databases,
        collection: String,
        userId: String,
    ): List<io.appwrite.models.Document<Map<String, Any>>>? {
        return try {
            db.listDocuments(DB_ID, collection, listOf(Query.equal("user_id", userId))).documents
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                Log.w(TAG, "Collection '$collection' not found; skipping.")
                null
            } else {
                throw e
            }
        }
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

    private fun likeDocId(userId: String, slug: String, surahId: Int, ayahNo: Int): String =
        "fav-${sanitizeId(slug)}-$surahId-$ayahNo".take(36)

    private fun followDocId(userId: String, slug: String): String =
        "fol-${sanitizeId(slug)}".take(36)

    private fun scheduleDocId(userId: String, scheduleId: String): String =
        scheduleId.take(36)

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
