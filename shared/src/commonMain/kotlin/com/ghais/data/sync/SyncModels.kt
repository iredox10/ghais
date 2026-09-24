package com.ghais.data.sync

/**
 * Sync lifecycle status for the cross-device cloud sync.
 *
 * - [IDLE]: no sync in progress; last operation succeeded or none ran yet.
 * - [SYNCING]: a pull and/or push pass is currently running.
 * - [ERROR]: the last pass failed; see `SyncEngine.lastError` for details.
 * - [DISABLED]: sync is turned off (e.g. signed out); [SyncEngine.syncNow] is a no-op.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    ERROR,
    DISABLED
}

/** Appwrite Cloud database ID shared by all sync collections. */
const val DB_ID = "ghais"

/** Cloud collection holding one document per liked ayah. */
const val LIKES = "likes"

/** Cloud collection holding the single playback-state document per user. */
const val PLAYBACK = "playback_state"

/** Cloud collection holding one document per followed reciter. */
const val FOLLOWS = "follows"

/** Cloud collection holding the single listening-stats document per user. */
const val STATS = "listening_stats"

/** Cloud collection holding one document per reminder schedule. */
const val SCHEDULES = "schedules"

/**
 * Cloud collection holding one document per history entry
 * (`history{user_id,track_json,played_at_ms}`). Index on
 * (`user_id`,`played_at_ms`); pull orders by `played_at_ms` desc.
 */
const val HISTORY = "history"

/**
 * Cloud collection holding one public document per reciter with aggregate
 * counters (`reciter_stats{slug,followers_count,likes_count,updated_at}`).
 * Read-only for clients (public read); written server-side. Doc id = slug.
 */
const val RECITER_STATS = "reciter_stats"

/** Cloud collection holding a full-JSON backup of every routine (private + public). */
const val ROUTINE_BACKUPS = "routine_backups"

/** Cloud collection holding the single onboarding-goal document per user. */
const val USER_PREFS = "user_prefs"

/**
 * Cloud collection holding khatma completion plans
 * (`khatma_plans{user_id,title,total_days,current_surah,current_ayah,percent}`).
 * NOT synced yet: no readable local store exists (see SyncEngine KDoc).
 */
const val KHATMA = "khatma_plans"

/**
 * Cloud collection holding public announcements
 * (`broadcasts{title,body,audience,urgency}`). Public read-only; the client
 * never writes. Pulled newest-first (`$createdAt` desc, limit 50) on every
 * sync pass and on inbox open into
 * `BroadcastRepository` (see `com.ghais.data.repository`).
 */
const val BROADCASTS = "broadcasts"
