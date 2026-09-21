package com.ghais.data.sync

import android.util.Log
import com.ghais.data.auth.AppwriteConfig
import com.ghais.data.auth.AuthRepository
import com.ghais.data.auth.PersistentCookieJar
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.SchedulesStore
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
            runCollection(PLAYBACK) { pushPlayback(db, userId) }
            runCollection(STATS) { pushStats(db, userId) }
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
            // NOTE: the server `likes` collection has no reciter_slug/audio_url
            // attributes (see gaps): pulled items carry a synthetic audioUrl so
            // FavoritesStore's audioUrl-identity dedup keeps working. They play
            // only after the attribute gap is closed (see file KDoc/return).
            val reciterSlug = data["reciter_slug"] as? String ?: ""
            FavoritesStore.add(
                TrackItem(
                    reciterSlug = reciterSlug,
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

    private suspend fun pushLikes(db: Databases, userId: String) {
        val local = FavoritesStore.favoriteTracks.value
        val existing = listForUser(db, LIKES, userId) ?: return
        val localKeys = local.map { likeKey(it.surahId, it.ayahNo) }.toSet()
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
            // Only attributes present in appwrite/collections.json are sent;
            // `reciter_slug` is intentionally omitted until the attribute is
            // added server-side (unknown attributes are rejected).
            val data: Map<String, Any?> = mapOf(
                "user_id" to userId,
                "surah_id" to track.surahId,
                "ayah_no" to track.ayahNo,
                "level" to "like",
            )
            upsert(db, LIKES, likeDocId(userId, track.reciterSlug, track.surahId, track.ayahNo), data)
        }
    }

    private fun likeKey(surahId: Int, ayahNo: Int): String = "$surahId:$ayahNo"

    // ---------------------------------------------------------------- follows

    private suspend fun pullFollowsIfEmpty(db: Databases, userId: String) {
        if (FollowStore.followedSlugs.value.isNotEmpty()) return
        val docs = listForUser(db, FOLLOWS, userId) ?: return
        FollowStore.clear()
        for (doc in docs) {
            val slug = doc.data["reciter_slug"] as? String
            if (!slug.isNullOrBlank()) FollowStore.follow(slug)
        }
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

    // --------------------------------------------------------------- playback

    private suspend fun pushPlayback(db: Databases, userId: String) {
        val track = AudioEngine.currentTrack.value
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
        )
        // Single doc per user, id = userId (matches user_state_unique index).
        upsert(db, PLAYBACK, userId, data)
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
