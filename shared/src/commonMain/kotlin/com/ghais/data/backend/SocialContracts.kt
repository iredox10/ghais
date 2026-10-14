package com.ghais.data.backend

/**
 * A13 social contracts: pure day-boundary + league-pooling + friend-streak
 * semantics shared by client and (later) server.
 *
 * Everything here is a pure function over primitives / the data classes
 * below — no clock, no platform timezone database, no network. Timezones
 * enter only as an explicit offset ([epochDay]) or as pre-computed
 * `YYYY-MM-DD` day strings, so both sides agree even when one side has no
 * TZ data (the classic server/client day-boundary skew).
 *
 * Day-string format is ISO `YYYY-MM-DD` (zero-padded), which orders
 * lexicographically and converts to an epoch day via [dayToEpochDay] using
 * the civil-date algorithm (no platform APIs).
 */
object SocialContracts {

    // ---- devotion ----

    /** Persisted per-user devotion state (mirrors the `devotion` collection). */
    data class DevotionState(
        val userId: String,
        val streakCount: Int = 0,
        val longestStreak: Int = 0,
        /** Last day with qualifying activity, `YYYY-MM-DD`, or null if never. */
        val lastActiveDay: String? = null,
        /** Missed days forgiven before the streak resets. */
        val graceDays: Int = 1,
        /** IANA name or opaque label; informational only, never parsed here. */
        val timezone: String? = null,
    )

    /** Result of folding one calendar day into a [DevotionState]. */
    data class DevotionResult(
        val state: DevotionState,
        /** True when [todayDay] advanced the streak (fresh active day). */
        val streakAdvanced: Boolean,
        /** True when a grace day was consumed to bridge a gap. */
        val graceUsed: Boolean,
    )

    /**
     * Folds [todayDay] (`YYYY-MM-DD` in the user's own timezone) into [state].
     *
     * - Same day as [DevotionState.lastActiveDay]: no change.
     * - Exactly the next day: streak + 1 (longest tracks the max).
     * - Gap of 2..(graceDays+1) days: streak + 1, consuming one grace day.
     * - Larger gap: streak resets to 1. First-ever day also starts at 1.
     *
     * Malformed day strings never throw: an unparseable [todayDay] returns the
     * state unchanged; an unparseable stored [lastActiveDay] is treated as no
     * history (streak starts at 1).
     */
    fun evaluateDevotionDay(state: DevotionState, todayDay: String): DevotionResult {
        val today = dayToEpochDay(todayDay) ?: return DevotionResult(state, false, false)
        val last = state.lastActiveDay?.let(::dayToEpochDay)
        if (last == null) {
            val fresh = state.copy(
                streakCount = 1,
                longestStreak = maxOf(state.longestStreak, 1),
                lastActiveDay = todayDay,
            )
            return DevotionResult(fresh, true, false)
        }
        val gap = today - last
        if (gap <= 0) return DevotionResult(state, false, false)
        if (gap == 1) {
            val advanced = state.streakCount + 1
            return DevotionResult(
                state.copy(
                    streakCount = advanced,
                    longestStreak = maxOf(state.longestStreak, advanced),
                    lastActiveDay = todayDay,
                ),
                true, false,
            )
        }
        return if (gap <= state.graceDays + 1 && state.graceDays > 0) {
            val advanced = state.streakCount + 1
            DevotionResult(
                state.copy(
                    streakCount = advanced,
                    longestStreak = maxOf(state.longestStreak, advanced),
                    lastActiveDay = todayDay,
                    graceDays = state.graceDays - 1,
                ),
                true, true,
            )
        } else {
            DevotionResult(state.copy(streakCount = 1, lastActiveDay = todayDay), true, false)
        }
    }

    // ---- leagues ----

    /** One pool of league members for a given week. */
    data class LeaguePool(
        val leagueId: String,
        val weekStart: String,
        val memberIds: List<String>,
        val tier: String = "bronze",
    )

    /**
     * Deterministically pools [memberIds] into groups of at most [poolSize]
     * for the week starting [weekStart] (`YYYY-MM-DD`).
     *
     * Shuffling is a stable sort by `hash(memberId + "|" + weekStart)`, so
     * client and server derive identical pools without exchanging them, and
     * pools for different weeks differ. Pool ids are `"league-<weekStart>-<n>"`
     * (1-based). Empty input yields no pools. [poolSize] below 1 is coerced
     * to [BackendSchema.Leagues.MAX_MEMBERS]; above the cap it is clamped.
     */
    fun poolLeague(
        memberIds: List<String>,
        weekStart: String,
        poolSize: Int = BackendSchema.Leagues.MAX_MEMBERS,
    ): List<LeaguePool> {
        if (memberIds.isEmpty()) return emptyList()
        val size = poolSize.coerceIn(1, BackendSchema.Leagues.MAX_MEMBERS)
        val distinct = memberIds.distinct()
        val shuffled = distinct.sortedBy { stableHash(it + "|" + weekStart) }
        return shuffled.chunked(size).mapIndexed { index, chunk ->
            LeaguePool(
                leagueId = "league-" + weekStart + "-" + (index + 1),
                weekStart = weekStart,
                memberIds = chunk,
            )
        }
    }

    // ---- friend streaks ----

    /** Result of folding one day into a friendship streak. */
    data class FriendStreakResult(
        val sharedStreak: Int,
        /** New `lastBothActiveDay`, or the previous value when unchanged. */
        val lastBothActiveDay: String?,
        /** True when both users were active together on [todayDay]. */
        val bothActiveToday: Boolean,
    )

    /**
     * Folds one day into a pair's shared streak.
     *
     * @param lastBothActiveDay previous `lastBothActiveDay` (`YYYY-MM-DD` or null).
     * @param aDay [aId]'s latest active day, or null if inactive.
     * @param bDay [bId]'s latest active day, or null if inactive.
     * @param todayDay the day being evaluated (`YYYY-MM-DD`).
     * @param currentStreak the stored `sharedStreak`.
     *
     * Both active on [todayDay]: same-day repeat keeps the streak; a
     * consecutive shared day increments; any larger gap restarts at 1.
     * Otherwise the streak is untouched (a one-sided day never resets the
     * pair — it only fails to advance it).
     */
    fun evaluateFriendStreak(
        lastBothActiveDay: String?,
        aDay: String?,
        bDay: String?,
        todayDay: String,
        currentStreak: Int,
    ): FriendStreakResult {
        if (aDay != todayDay || bDay != todayDay) {
            return FriendStreakResult(currentStreak, lastBothActiveDay, false)
        }
        val last = lastBothActiveDay?.let(::dayToEpochDay)
        val today = dayToEpochDay(todayDay)
            ?: return FriendStreakResult(currentStreak, lastBothActiveDay, false)
        if (last != null && today - last <= 0) {
            return FriendStreakResult(currentStreak, lastBothActiveDay, true)
        }
        val streak = if (last != null && today - last == 1) currentStreak + 1 else 1
        return FriendStreakResult(streak, todayDay, true)
    }

    // ---- day math (pure, no platform APIs) ----

    /**
     * Epoch milliseconds → epoch day under an explicit UTC offset
     * ([offsetMinutes], e.g. +330 for Asia/Kolkata). Callers derive the offset
     * on their own platform; the boundary math itself stays shared.
     */
    fun epochDay(epochMs: Long, offsetMinutes: Int): Long =
        floorDiv(epochMs + offsetMinutes * 60_000L, 86_400_000L)

    /** Floor division for a positive divisor (common-main `Math.floorDiv` stand-in). */
    private fun floorDiv(dividend: Long, divisor: Long): Long =
        if (dividend >= 0) dividend / divisor else (dividend - divisor + 1) / divisor

    /** Epoch day → ISO `YYYY-MM-DD` (civil-date algorithm, proleptic Gregorian). */
    fun epochDayToString(epochDay: Long): String {
        var z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = (z - era * 146097).toInt()
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        var y = (yoe + era * 400).toInt()
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        y += if (m <= 2) 1 else 0
        return pad4(y) + "-" + pad2(m) + "-" + pad2(d)
    }

    private fun pad2(v: Int): String = if (v < 10) "0$v" else v.toString()

    private fun pad4(v: Int): String {
        var s = v.toString()
        while (s.length < 4) s = "0" + s
        return s
    }

    /**
     * ISO `YYYY-MM-DD` → epoch day, or null when malformed (never throws).
     * Accepts exactly 10 chars, digits + dashes in positions 4 and 7, with a
     * real month/day range check.
     */
    fun dayToEpochDay(day: String): Int? {
        if (day.length != 10 || day[4] != '-' || day[7] != '-') return null
        fun part(from: Int, to: Int): Int? {
            var v = 0
            for (i in from until to) {
                val c = day[i]
                if (c < '0' || c > '9') return null
                v = v * 10 + (c - '0')
            }
            return v
        }
        val y = part(0, 4) ?: return null
        val m = part(5, 7) ?: return null
        val d = part(8, 10) ?: return null
        if (m !in 1..12 || d !in 1..daysInMonth(y, m)) return null
        val yAdj = if (m <= 2) y - 1 else y
        val era = (if (yAdj >= 0) yAdj else yAdj - 399) / 400
        val yoe = yAdj - era * 400
        val mp = (m + 9) % 12
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return (era * 146097 + doe - 719468).toInt()
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (isLeap(year)) 29 else 28
    }

    private fun isLeap(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    /** FNV-1a 32-bit over UTF-16 code units; stable across platforms. */
    private fun stableHash(s: String): Int {
        var h = 0x811c9dc5.toInt()
        for (i in s.indices) {
            h = h xor s[i].code
            h *= 0x01000193
        }
        return h
    }
}
