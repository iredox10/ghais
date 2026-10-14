package com.ghais.hifz.quests

import com.ghais.data.repository.MasteryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AGENT A9 — guards the pure quest engine: the deterministic 3-quest set,
 * clamping/edge cases, and the pure progress updaters (immutability + routing).
 */
class HifzQuestsTest {

    private val sampleMastery = mapOf(
        (1 to 1) to MasteryStatus.MASTERED,
        (1 to 2) to MasteryStatus.REVIEW_NEEDED,
        (1 to 3) to MasteryStatus.REVIEW_NEEDED,
        (2 to 1) to MasteryStatus.REVIEW_NEEDED,
    )

    @Test
    fun testQuestsForReturnsThreeDeterministicQuests() {
        val first = questsFor(sampleMastery, todayReviewed = 2, dailyGoal = 5, epochDay = 100L)
        val second = questsFor(sampleMastery, todayReviewed = 2, dailyGoal = 5, epochDay = 100L)

        assertEquals(3, first.size)
        assertEquals(first, second)
        assertEquals(
            listOf("$REVIEW_QUEST_PREFIX-100", "$MASTER_QUEST_PREFIX-100", "$RECORD_QUEST_PREFIX-100"),
            first.map { it.id },
        )
    }

    @Test
    fun testQuestIdsAreDayScoped() {
        val monday = questsFor(emptyMap(), 0, 5, epochDay = 100L)
        val tuesday = questsFor(emptyMap(), 0, 5, epochDay = 101L)

        assertEquals(monday.map { it.title }, tuesday.map { it.title })
        assertTrue(monday.map { it.id }.zip(tuesday.map { it.id }).all { (a, b) -> a != b })
    }

    @Test
    fun testReviewQuestTracksGoalAndDuePool() {
        val quests = questsFor(sampleMastery, todayReviewed = 2, dailyGoal = 5, epochDay = 7L)
        val review = quests.first { it.id.startsWith(REVIEW_QUEST_PREFIX) }

        assertEquals("Review 5 due ayahs", review.title)
        assertEquals(5, review.target)
        assertEquals(2, review.progress)
        assertFalse(review.done)
        assertTrue(review.description.contains("3"), "due pool (3 REVIEW_NEEDED) in description")
    }

    @Test
    fun testReviewQuestClampsProgressAndFlagsDone() {
        val negative = questsFor(emptyMap(), todayReviewed = -4, dailyGoal = 5, epochDay = 7L)
            .first { it.id.startsWith(REVIEW_QUEST_PREFIX) }
        assertEquals(0, negative.progress)
        assertFalse(negative.done)

        val over = questsFor(emptyMap(), todayReviewed = 99, dailyGoal = 5, epochDay = 7L)
            .first { it.id.startsWith(REVIEW_QUEST_PREFIX) }
        assertEquals(5, over.progress)
        assertTrue(over.done)

        val zeroGoal = questsFor(emptyMap(), todayReviewed = 0, dailyGoal = 0, epochDay = 7L)
            .first { it.id.startsWith(REVIEW_QUEST_PREFIX) }
        assertEquals(1, zeroGoal.target, "goal clamps to at least 1")
    }

    @Test
    fun testUpdatersArePureCopies() {
        val quests = questsFor(emptyMap(), todayReviewed = 0, dailyGoal = 5, epochDay = 7L)
        val review = quests.first { it.id.startsWith(REVIEW_QUEST_PREFIX) }

        val bumped = review.withProgress(4)
        assertEquals(0, review.progress, "original untouched")
        assertEquals(4, bumped.progress)
        assertFalse(bumped.done)

        val done = review.markDone()
        assertEquals(review.target, done.progress)
        assertTrue(done.done)
        assertFalse(review.done, "original untouched")
    }

    @Test
    fun testListUpdatersRouteToTheRightQuests() {
        var quests = questsFor(emptyMap(), todayReviewed = 0, dailyGoal = 5, epochDay = 7L)
        assertFalse(quests.allDone())

        quests = quests.afterReview(todayReviewed = 5)
        assertTrue(quests.first { it.id.startsWith(REVIEW_QUEST_PREFIX) }.done)
        assertFalse(quests.allDone())

        quests = quests.afterMastered()
        assertTrue(quests.first { it.id.startsWith(MASTER_QUEST_PREFIX) }.done)
        assertFalse(quests.allDone())

        quests = quests.afterRecording()
        assertTrue(quests.first { it.id.startsWith(RECORD_QUEST_PREFIX) }.done)
        assertTrue(quests.allDone())
    }

    @Test
    fun testWithQuestProgressIgnoresUnknownIds() {
        val quests = questsFor(emptyMap(), todayReviewed = 1, dailyGoal = 5, epochDay = 7L)
        val untouched = quests.withQuestProgress("no-such-quest", 99)
        assertEquals(quests, untouched)
    }

    @Test
    fun testCeremonyTextContainsSurahReciterDaysAndCounts() {
        val text = surahCeremonyText(
            surahName = "Al-Fatihah",
            reciterName = "Mishary Alafasy",
            daysTaken = 6,
            masteredCount = 7,
            totalAyahs = 7,
            streakDays = 4,
        )
        assertTrue(text.contains("Al-Fatihah"))
        assertTrue(text.contains("Mishary Alafasy"))
        assertTrue(text.contains("6 days"))
        assertTrue(text.contains("7 of 7"))
        assertTrue(text.contains("4-day streak"))
    }
}
