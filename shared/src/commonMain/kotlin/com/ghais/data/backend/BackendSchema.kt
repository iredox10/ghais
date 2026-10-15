package com.ghais.data.backend

/**
 * A13 backend contracts: collection ids + attribute-name constants for the
 * five hook collections the client will read/write once provisioned.
 *
 * Conventions mirrored from `appwrite/collections.json` + `SyncModels.kt`:
 * - Database id is [DATABASE_ID] (`ghais`, same as `SyncModels.DB_ID`).
 * - Per-user docs use docId = userId (cf. `listening_stats`, `playback_state`).
 * - Pair docs use a stable synthetic docId (cf. likes `fav-<sha>`,
 *   follows `fol-<sha>`); see [Friendships.PAIR_KEY].
 * - Maps that Appwrite cannot type natively ride as JSON strings
 *   (cf. `daily_json`, `surah_plays`, `queue_json`); see [Leagues.XP_JSON].
 * - Attribute keys are camelCase for these five collections, matching the
 *   newer `play_events` / `charts` style (`reciterSlug`, `topQaris7d`) rather
 *   than the older snake_case style. The task spec pins these names.
 *
 * Nothing here talks to the network: no credentials exist yet. When the
 * backend is provisioned, run [CREATE_SCRIPT] (Appwrite CLI, mechanical),
 * then wire repositories to these constants the way `FollowStore` uses
 * `SyncModels.FOLLOWS`.
 */
object BackendSchema {
    /** Appwrite database id shared with every existing collection. */
    const val DATABASE_ID = "ghais"

    /** devotion{userId,streakCount,longestStreak,lastActiveDay,graceDays,timezone}: one doc per user, docId = userId. */
    const val DEVOTION = "devotion"

    /** leagues{leagueId,weekStart,memberIds[30],xpJson,tier}: one doc per pool per week. */
    const val LEAGUES = "leagues"

    /** friendships{pairKey,aId,bId,sharedStreak,lastBothActiveDay}: one doc per pair, docId = pairKey. */
    const val FRIENDSHIPS = "friendships"

    /** global_charts{window,entries,generatedAt}: server-written aggregates, client read-only (cf. `charts`). */
    const val GLOBAL_CHARTS = "global_charts"

    /** achievements{userId,achievementId,unlockedMs}: one doc per unlock. */
    const val ACHIEVEMENTS = "achievements"

    /** Attribute keys for [DEVOTION]. Day strings are `YYYY-MM-DD` (see SocialContracts). */
    object Devotion {
        const val USER_ID = "userId"
        const val STREAK_COUNT = "streakCount"
        const val LONGEST_STREAK = "longestStreak"
        const val LAST_ACTIVE_DAY = "lastActiveDay"
        const val GRACE_DAYS = "graceDays"
        const val TIMEZONE = "timezone"
    }

    /** Attribute keys for [LEAGUES]. */
    object Leagues {
        const val LEAGUE_ID = "leagueId"
        const val WEEK_START = "weekStart"
        const val MEMBER_IDS = "memberIds"
        const val XP_JSON = "xpJson"
        const val TIER = "tier"

        /** Hard cap on pool size; enforced by `poolLeague`, mirrored by the CLI script. */
        const val MAX_MEMBERS = 30
    }

    /** Attribute keys for [FRIENDSHIPS]. */
    object Friendships {
        const val PAIR_KEY = "pairKey"
        const val A_ID = "aId"
        const val B_ID = "bId"
        const val SHARED_STREAK = "sharedStreak"
        const val LAST_BOTH_ACTIVE_DAY = "lastBothActiveDay"

        /**
         * Stable doc id + [PAIR_KEY] value for a pair: the two user ids sorted
         * lexicographically and joined with `|`. Sorting keeps A/B assignment
         * canonical so either device derives the same key.
         */
        fun pairKey(firstUserId: String, secondUserId: String): String =
            if (firstUserId <= secondUserId) "$firstUserId|$secondUserId"
            else "$secondUserId|$firstUserId"
    }

    /** Attribute keys for [GLOBAL_CHARTS]. */
    object GlobalCharts {
        const val WINDOW = "window"
        const val ENTRIES = "entries"
        const val GENERATED_AT = "generatedAt"

        /** Known [WINDOW] values. */
        const val WINDOW_7D = "7d"
        const val WINDOW_30D = "30d"
        const val WINDOW_TRENDING = "trending"
    }

    /** Attribute keys for [ACHIEVEMENTS]. */
    object Achievements {
        const val USER_ID = "userId"
        const val ACHIEVEMENT_ID = "achievementId"
        const val UNLOCKED_MS = "unlockedMs"

        /** Stable doc id for an unlock: one doc per user per achievement. */
        fun docId(userId: String, achievementId: String): String = "ach-" + userId + "-" + achievementId
    }

    /**
     * Mechanical provisioning script (Appwrite CLI). No credentials are baked
     * in: set `APPWRITE_ENDPOINT`, `APPWRITE_PROJECT_ID` and an API key with
     * `databases.write` scope, then run each line in order. Idempotent-safe to
     * re-run only for the attribute/index lines that were skipped on failure —
     * `createCollection` itself fails if the collection already exists, which
     * is the signal to continue with attributes.
     *
     * Kept as a plain string constant (not executed code) on purpose: the
     * client ships no API key (see `AppwriteConfig`), so creation happens from
     * an operator shell, exactly like the existing collections in
     * `appwrite/collections.json`.
     */
    val CREATE_SCRIPT: String =
        """
        # A13 hook collections — Appwrite CLI provisioning.
        # Prerequisites (export first):
        #   export APPWRITE_ENDPOINT='https://fra.cloud.appwrite.io/v1'
        #   export APPWRITE_PROJECT_ID='tikitakatalk'
        #   export APPWRITE_API_KEY='<server key with databases.write>'
        # CLI login once (uses the endpoint exported above).
        # Database id for every --database-id flag below:
        #   DB=ghais

        # ---- 1. devotion (per-user, docId = userId; mirrors listening_stats) ----
        appwrite databases createCollection --database-id ghais --collection-id devotion --name "Devotion" --permissions '["read(\"users\")", "create(\"users\")", "update(\"users\")", "delete(\"users\")"]' --document-security true
        appwrite databases createStringAttribute --database-id ghais --collection-id devotion --key userId --size 64 --required true
        appwrite databases createIntegerAttribute --database-id ghais --collection-id devotion --key streakCount --required false --min 0 --default 0
        appwrite databases createIntegerAttribute --database-id ghais --collection-id devotion --key longestStreak --required false --min 0 --default 0
        appwrite databases createStringAttribute --database-id ghais --collection-id devotion --key lastActiveDay --size 10 --required false
        appwrite databases createIntegerAttribute --database-id ghais --collection-id devotion --key graceDays --required false --min 0 --default 1
        appwrite databases createStringAttribute --database-id ghais --collection-id devotion --key timezone --size 64 --required false
        appwrite databases createIndex --database-id ghais --collection-id devotion --key user_unique --type unique --attributes '["userId"]'

        # ---- 2. leagues (one doc per pool per week) ----
        appwrite databases createCollection --database-id ghais --collection-id leagues --name "Leagues" --permissions '["read(\"users\")", "create(\"users\")", "update(\"users\")", "delete(\"users\")"]' --document-security true
        appwrite databases createStringAttribute --database-id ghais --collection-id leagues --key leagueId --size 64 --required true
        appwrite databases createStringAttribute --database-id ghais --collection-id leagues --key weekStart --size 10 --required true
        appwrite databases createStringAttribute --database-id ghais --collection-id leagues --key memberIds --size 64 --required false --array true
        appwrite databases createStringAttribute --database-id ghais --collection-id leagues --key xpJson --size 4096 --required false
        appwrite databases createStringAttribute --database-id ghais --collection-id leagues --key tier --size 16 --required false
        appwrite databases createIndex --database-id ghais --collection-id leagues --key league_week_idx --type key --attributes '["leagueId", "weekStart"]'

        # ---- 3. friendships (one doc per pair, docId = pairKey "a|b" sorted) ----
        appwrite databases createCollection --database-id ghais --collection-id friendships --name "Friendships" --permissions '["read(\"users\")", "create(\"users\")", "update(\"users\")", "delete(\"users\")"]' --document-security true
        appwrite databases createStringAttribute --database-id ghais --collection-id friendships --key pairKey --size 128 --required true
        appwrite databases createStringAttribute --database-id ghais --collection-id friendships --key aId --size 64 --required true
        appwrite databases createStringAttribute --database-id ghais --collection-id friendships --key bId --size 64 --required true
        appwrite databases createIntegerAttribute --database-id ghais --collection-id friendships --key sharedStreak --required false --min 0 --default 0
        appwrite databases createStringAttribute --database-id ghais --collection-id friendships --key lastBothActiveDay --size 10 --required false
        appwrite databases createIndex --database-id ghais --collection-id friendships --key pair_unique --type unique --attributes '["pairKey"]'

        # ---- 4. global_charts (server-written, client read-only; mirrors charts) ----
        appwrite databases createCollection --database-id ghais --collection-id global_charts --name "Hook Global Charts" --permissions '["read(\"any\")"]' --document-security true
        appwrite databases createStringAttribute --database-id ghais --collection-id global_charts --key window --size 16 --required true
        appwrite databases createStringAttribute --database-id ghais --collection-id global_charts --key entries --size 64 --required true --array true
        appwrite databases createDatetimeAttribute --database-id ghais --collection-id global_charts --key generatedAt --required true
        appwrite databases createIndex --database-id ghais --collection-id global_charts --key window_unique --type unique --attributes '["window"]'

        # ---- 5. achievements (one doc per unlock, docId = ach-<userId>-<achievementId>) ----
        appwrite databases createCollection --database-id ghais --collection-id achievements --name "Achievements" --permissions '["read(\"users\")", "create(\"users\")", "update(\"users\")", "delete(\"users\")"]' --document-security true
        appwrite databases createStringAttribute --database-id ghais --collection-id achievements --key userId --size 64 --required true
        appwrite databases createStringAttribute --database-id ghais --collection-id achievements --key achievementId --size 64 --required true
        appwrite databases createIntegerAttribute --database-id ghais --collection-id achievements --key unlockedMs --required true
        appwrite databases createIndex --database-id ghais --collection-id achievements --key user_achievement_unique --type unique --attributes '["userId", "achievementId"]'
        """.trimIndent()
}
