package com.ghais.hifz.quests

import com.ghais.data.repository.MasteryStatus

/**
 * AGENT A9 — Hifz daily quests: pure quest engine.
 *
 * No Compose, no Settings, no clocks. [questsFor] builds the deterministic
 * 3-quest daily set from a snapshot of [HifzMasteryStore]-adjacent state, and
 * every progress transition below is a pure copy — callers persist or award XP
 * (see [xpFor]) themselves.
 *
 * Quest ids are day-scoped (`...-<epochDay>`) so a midnight rollover yields a
 * fresh set without any stored "claimed" bookkeeping leaking across days.
 */
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int,
    val rewardXp: Int,
    val done: Boolean = progress >= target,
)

/** Id prefixes (before the trailing `-<epochDay>`) of the three daily quests. */
const val REVIEW_QUEST_PREFIX = "hifz-quest-review"
const val MASTER_QUEST_PREFIX = "hifz-quest-master"
const val RECORD_QUEST_PREFIX = "hifz-quest-record"

/** XP shown on each quest row. Kept in sync with [xpFor] semantics. */
const val REVIEW_QUEST_XP = 10
const val MASTER_QUEST_XP = 20
const val RECORD_QUEST_XP = 20

/**
 * Builds the deterministic 3-quest daily set:
 * 1. "Review N due ayahs" — target = [dailyGoal], progress = [todayReviewed].
 * 2. "Master 1 new ayah" — fresh each day, completed via [List.afterMastered].
 * 3. "Record & compare once" — fresh each day, completed via [List.afterRecording].
 *
 * [mastery] feeds the review quest's description (how many ayahs sit in
 * REVIEW_NEEDED); [epochDay] scopes the ids to the day. Same inputs always
 * produce the same list.
 */
fun questsFor(
    mastery: Map<Pair<Int, Int>, MasteryStatus>,
    todayReviewed: Int,
    dailyGoal: Int,
    epochDay: Long,
): List<Quest> {
    val goal = dailyGoal.coerceAtLeast(1)
    val dueTotal = mastery.count { it.value == MasteryStatus.REVIEW_NEEDED }
    return listOf(
        Quest(
            id = "$REVIEW_QUEST_PREFIX-$epochDay",
            title = "Review $goal due ayahs",
            description = if (dueTotal == 0) {
                "No weak ayahs waiting — review anything to stay sharp"
            } else {
                "$dueTotal ${if (dueTotal == 1) "ayah" else "ayahs"} waiting for review"
            },
            target = goal,
            progress = todayReviewed.coerceIn(0, goal),
            rewardXp = REVIEW_QUEST_XP,
        ),
        Quest(
            id = "$MASTER_QUEST_PREFIX-$epochDay",
            title = "Master 1 new ayah",
            description = "Take one ayah from new to mastered",
            target = 1,
            progress = 0,
            rewardXp = MASTER_QUEST_XP,
        ),
        Quest(
            id = "$RECORD_QUEST_PREFIX-$epochDay",
            title = "Record & compare once",
            description = "Recite from memory, then compare with your reciter",
            target = 1,
            progress = 0,
            rewardXp = RECORD_QUEST_XP,
        ),
    )
}

/** Returns a copy with [newProgress] clamped to `0..target`, [Quest.done] recomputed. */
fun Quest.withProgress(newProgress: Int): Quest {
    val clamped = newProgress.coerceIn(0, target)
    return copy(progress = clamped, done = clamped >= target)
}

/** Returns a copy snapped to full completion. */
fun Quest.markDone(): Quest = copy(progress = target, done = true)

/** Replaces the quest with [id] by its [withProgress] copy; unknown ids pass through. */
fun List<Quest>.withQuestProgress(id: String, progress: Int): List<Quest> =
    map { if (it.id == id) it.withProgress(progress) else it }

/**
 * Refreshes the review quest after a review is recorded ([todayReviewed] is the
 * store's fresh `todayReviewedCount`). Other quests are untouched.
 */
fun List<Quest>.afterReview(todayReviewed: Int): List<Quest> =
    map { if (it.id.startsWith(REVIEW_QUEST_PREFIX)) it.withProgress(todayReviewed) else it }

/** Bumps the master-one-ayah quest to done. */
fun List<Quest>.afterMastered(): List<Quest> =
    map { if (it.id.startsWith(MASTER_QUEST_PREFIX)) it.markDone() else it }

/** Completes the record-and-compare quest. */
fun List<Quest>.afterRecording(): List<Quest> =
    map { if (it.id.startsWith(RECORD_QUEST_PREFIX)) it.markDone() else it }

/** True when every quest in the daily set is done (vacuously true when empty). */
fun List<Quest>.allDone(): Boolean = all { it.done }
