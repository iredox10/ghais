package com.ghais.data.sync

import android.util.Log
import com.ghais.data.auth.AppwriteConfig
import com.ghais.data.auth.AuthRepository
import com.ghais.data.auth.PersistentCookieJar
import com.ghais.data.repository.CustomRoutine
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.SchedulesStore
import com.ghais.domain.model.Reciter
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.seed.JumpBackInItem
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
import kotlinx.serialization.decodeFromString
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
 * failure wins for [lastError]). Push skips only when local state is still
 * empty but the cloud holds a snapshot (fresh login), so an empty local
 * never clobbers the restore source. `lastSyncedAt` is set on full success only.
 * All network runs on Dispatchers.IO. Failures go to Log.e("GhaisSync").
 */
actual object SyncEngine {

    private const val TAG = "GhaisSync"

    private const val PLAYLISTS = "playlists"
    private const val PLAYLIST_ITEMS = "playlist_items"
    private const val ROUTINE_DOC_PREFIX = "rtn-"
    private const val HISTORY_CAP = 100

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
            runCollection(HISTORY) {
                val cloudHasData = pullHistoryIfEmpty(db, userId)
                pushHistory(db, userId, cloudHasData)
            }
            runCollection(SCHEDULES) { pushSchedules(db, userId) }
            runCollection(PLAYLISTS) { pushRoutines(db, userId) }
            runCollection(ROUTINE_BACKUPS) {
                val cloudHasData = pullRoutineBackupsIfEmpty(db, userId)
                pushRoutineBackups(db, userId, cloudHasData)
            }
            runCollection(USER_PREFS) {
                pullUserPrefsIfEmpty(db, userId)
                pushUserPrefs(db, userId)
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

    /**
     * Pulls the cloud `listening_stats` doc (docId = [userId]) when local stats
     * are still at fresh-install defaults, so a fresh login can adopt the
     * cloud snapshot instead of starting from zero.
     *
     * Returns true when the cloud holds a usable snapshot (caller must then
     * skip pushing, otherwise local zeros would overwrite it). Returns false
     * when local stats are already non-zero (never overwrite those) or the
     * cloud has nothing, so push proceeds.
     *
     * `minutes_today` date semantics: [UserUsageRepository] derives it from
     * seconds-listened-today for the CURRENT epoch day (`now/86400000`) and
     * resets on day rollover, so a cloud `minutes_today` is only valid when
     * cloud `updated_at` falls on today's epoch day — otherwise it restores
     * as 0. `days_streak`/`total_seconds`/uniques carry over as-is.
     *
     * Adopt is currently GUARDED, not applied: [UserUsageRepository] exposes
     * no restore/import API (private `_stats`/keys; `setOwner` only reloads
     * from disk), and this file may not grow new deps or touch other files.
     * So the snapshot is validated + logged and the zero-push is skipped to
     * protect the cloud copy. Follow-up: add
     * `UserUsageRepository.restoreStats(...)` (day-rollover aware) and call it
     * here.
     */
    private suspend fun pullStatsIfEmpty(db: Databases, userId: String): Boolean {
        if (!isLocalStatsEmpty()) return false
        val doc = getDocument(db, STATS, userId) ?: return false
        val data = doc.data
        val total = asLong(data["total_seconds"]) ?: 0L
        val streak = asInt(data["days_streak"]) ?: 1
        val uniquesReciters = asInt(data["unique_reciters"]) ?: 0
        val uniquesSurahs = asInt(data["unique_surahs"]) ?: 0
        val updatedAt = asLong(data["updated_at"])
        var minutes = asInt(data["minutes_today"]) ?: 0
        if (updatedAt == null || !sameEpochDay(updatedAt, System.currentTimeMillis())) {
            minutes = 0
        }
        if (total <= 0L && streak <= 1 && minutes <= 0 && uniquesReciters <= 0 && uniquesSurahs <= 0) {
            return false // cloud snapshot itself is empty; let push converge
        }
        Log.i(
            TAG,
            "stats pull: cloud total=${total}s streak=$streak minutesToday=$minutes " +
                "reciters=$uniquesReciters surahs=$uniquesSurahs" +
                (if (updatedAt != null) " updatedAt=$updatedAt" else "") +
                " — no UserUsageRepository restore API yet; keeping local zeros, push skipped."
        )
        return true
    }

    private suspend fun pushStats(db: Databases, userId: String, cloudHasData: Boolean) {
        if (cloudHasData && isLocalStatsEmpty()) {
            // Fresh login whose cloud snapshot couldn't be applied locally yet:
            // don't overwrite it with zeros.
            Log.i(TAG, "pushStats skipped (local empty, cloud has data).")
            return
        }
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

    /**
     * True when local stats are still at fresh-install defaults
     * (`daysStreak = 1`, `minutesToday = 0`, `totalSeconds = 0`, uniques of 1
     * are just the `"mishary"`/`"18"` placeholders [UserUsageRepository]
     * seeds when storage is empty). Anything beyond that counts as real local
     * activity that pull must never overwrite.
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
    // Pull-if-empty + push-capped (push skips when local is still empty but
    // cloud has data, so a fresh login never clobbers its restore source with
    // an empty state — no-data-loss guard, same as playback/stats).
    //
    // Cloud shape: `history{user_id,track_json,played_at_ms}` (index on
    // `user_id,played_at_ms`). `track_json = Json.encodeToString(TrackItem)`
    // ([TrackItem] is `@Serializable`; rebuilt from the local
    // [JumpBackInItem] via reciter/surah lookups so the JSON carries real
    // display names + audio URL). Doc ids are stable per entry:
    // `"h-<sanitizedSlug>-<surahId>-<playedAtMs>"` (max 36 chars). Push sends
    // at most [HISTORY_CAP] recent entries and deletes cloud docs beyond the
    // local set so reorders/removals converge.

    /**
     * Pulls the cloud `history` docs (ordered by `played_at_ms` desc) when
     * local history is still empty, so a fresh login can adopt the cloud
     * copy instead of starting from zero.
     *
     * Returns true when the cloud holds at least one usable snapshot (caller
     * must then skip pushing, otherwise the still-empty local state would
     * overwrite it). Returns false when local history is non-empty or the
     * cloud has nothing usable, so push proceeds.
     *
     * Adopt is currently GUARDED, not applied: [UserUsageRepository] exposes
     * no history restore/import API (private `_history`; `recordProgress` is
     * private and driven only by [AudioEngine] polls), and this file may not
     * grow new deps or touch other files. So the snapshot is validated +
     * logged and the empty-push is skipped to protect the cloud copy.
     * Follow-up: add `UserUsageRepository.restoreHistory(items)` (taking
     * decoded `TrackItem` + `playedAtMs` pairs, most-recent-first, capped)
     * and call it here.
     */
    private suspend fun pullHistoryIfEmpty(db: Databases, userId: String): Boolean {
        if (UserUsageRepository.history.value.isNotEmpty()) return false
        val docs = listHistoryForUser(db, userId) ?: return false
        if (docs.isEmpty()) return false
        var valid = 0
        for (doc in docs) {
            val trackJson = (doc.data["track_json"] as? String)?.trim().orEmpty()
            if (trackJson.isEmpty()) continue
            val playedAt = asLong(doc.data["played_at_ms"]) ?: continue
            try {
                json.decodeFromString<TrackItem>(trackJson)
                if (playedAt > 0L) valid++
            } catch (_: Exception) {
            }
        }
        if (valid <= 0) return false // cloud snapshot itself is empty; let push converge
        Log.i(
            TAG,
            "history pull: cloud has $valid entries (most recent playedAt=" +
                "${asLong(docs.firstOrNull()?.data?.get("played_at_ms"))})" +
                " — no UserUsageRepository restore API yet; keeping local empty, push skipped."
        )
        return true
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
        val now = System.currentTimeMillis()
        val localIds = mutableSetOf<String>()
        local.forEachIndexed { index, item ->
            val track = buildHistoryTrack(item)
            if (track == null) {
                Log.w(
                    TAG,
                    "history push: skipping unresolvable entry " +
                        "'${item.reciterSlug}/${item.surahId}'."
                )
                return@forEachIndexed
            }
            // Zero-timestamp entries are in-session items not yet reloaded from
            // disk (PersistedHistoryItem stamps on save): stagger them so the
            // local most-recent-first order survives the played_at_ms ranking.
            val playedAt = item.lastPlayedTimestampMs.takeIf { it > 0L }
                ?: (now - index)
            val docId = historyDocId(track.reciterSlug, track.surahId, playedAt)
            localIds.add(docId)
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "track_json" to json.encodeToString(track),
                "played_at_ms" to playedAt,
            )
            upsert(db, HISTORY, docId, data)
        }
        for (doc in existing) {
            if (doc.id !in localIds) {
                try {
                    db.deleteDocument(DB_ID, HISTORY, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "delete stale history ${doc.id} failed", e)
                }
            }
        }
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

    private fun historyDocId(slug: String, surahId: Int, playedAtMs: Long): String {
        val s = sanitizeId(slug).take(12)
        return "h-$s-$surahId-$playedAtMs".take(36)
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

    // ------------------------------------------ routine backups (private)
    //
    // Private backup of ALL routines (public + private) to `routine_backups`
    // (`routine_backups{user_id,routine_id,routine_json,updated_at}`, doc id
    // `rtn-<routineId>` via [routineDocId]). This is separate from the public
    // `playlists`/`playlist_items` publish flow above, which stays unchanged:
    // private routines must never leak into the public catalog.
    //
    // Pull-if-empty is GUARDED, not applied: [CustomRoutinesStore] exposes no
    // bulk restore/import API (`create()` regenerates ids/timestamps;
    // `update()`/`delete()` need existing entries), so the snapshot is
    // validated + logged and the empty-push is skipped to protect the cloud
    // copy. Follow-up: add `CustomRoutinesStore.restoreAll(...)` and call it
    // here (same pattern as the [STATS] follow-up above).

    /**
     * Validates the cloud `routine_backups` snapshot when local routines are
     * still empty. Returns true when the cloud holds at least one parseable
     * backup (caller must then skip pushing, otherwise the still-empty local
     * state would overwrite it). Returns false when local routines exist or
     * the cloud has nothing, so push proceeds.
     */
    private suspend fun pullRoutineBackupsIfEmpty(db: Databases, userId: String): Boolean {
        if (CustomRoutinesStore.routines.value.isNotEmpty()) return false
        val docs = listForUser(db, ROUTINE_BACKUPS, userId) ?: return false
        var usable = 0
        for (doc in docs) {
            val raw = doc.data["routine_json"] as? String ?: continue
            if (raw.isBlank()) continue
            if (runCatching { json.decodeFromString<CustomRoutine>(raw) }.getOrNull() != null) {
                usable++
            }
        }
        if (usable <= 0) return false
        Log.i(
            TAG,
            "routine_backups pull: $usable usable backup(s) in cloud" +
                " — no CustomRoutinesStore restore API yet; keeping local empty, push skipped."
        )
        return true
    }

    private suspend fun pushRoutineBackups(db: Databases, userId: String, cloudHasData: Boolean) {
        val local = CustomRoutinesStore.routines.value
        if (local.isEmpty() && cloudHasData) {
            // Fresh login whose cloud snapshot couldn't be applied locally yet:
            // don't overwrite it with nothing.
            Log.i(TAG, "pushRoutineBackups skipped (local empty, cloud has data).")
            return
        }
        val existing = listForUser(db, ROUTINE_BACKUPS, userId) ?: return
        val localIds = local.map { it.id }.toSet()
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

    /** Same epoch-day check (`millis/86400000`, mirroring UserUsageRepository's day math). */
    private fun sameEpochDay(aMs: Long, bMs: Long): Boolean {
        if (aMs <= 0L || bMs <= 0L) return false
        return aMs / 86_400_000L == bMs / 86_400_000L
    }

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
