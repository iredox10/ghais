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
