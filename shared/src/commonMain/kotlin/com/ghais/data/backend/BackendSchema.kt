package com.ghais.data.backend

/** Social collection IDs and attribute names shared by the Render client. */
object BackendSchema {
    /** Database id used by the Render document API. */
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

    /** Apply the Render database schema from the backend directory. */
    val CREATE_SCRIPT: String = "npm run migrate"
}
