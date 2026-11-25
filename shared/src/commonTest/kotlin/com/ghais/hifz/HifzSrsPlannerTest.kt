package com.ghais.hifz

import com.ghais.data.repository.MasteryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HifzSrsPlannerTest {

    @Test
    fun emptyStateSuggestsAlFatihahForSabaqAndEmptySabqiManzil() {
        val sessions = buildSrsSessions(
            masteryMap = emptyMap(),
            dueDayMap = emptyMap(),
            weakAyahs = emptyList(),
            todayEpochDay = 100L,
        )

        assertEquals(3, sessions.size)
        val sabaq = sessions.first { it.tier == HifzSessionTier.SABAQ }
        val sabqi = sessions.first { it.tier == HifzSessionTier.SABQI }
        val manzil = sessions.first { it.tier == HifzSessionTier.MANZIL }

        assertEquals((1..7).map { 1 to it }, sabaq.targets)
        assertTrue(sabqi.targets.isEmpty())
        assertTrue(manzil.targets.isEmpty())
    }

    @Test
    fun weakAyahsAppearInManzil() {
        val weak = listOf(2 to 255, 1 to 3)
        val sessions = buildSrsSessions(
            masteryMap = mapOf(
                (1 to 3) to MasteryStatus.REVIEW_NEEDED,
                (2 to 255) to MasteryStatus.REVIEW_NEEDED,
            ),
            dueDayMap = mapOf(
                (1 to 3) to 101L,
                (2 to 255) to 101L,
            ),
            weakAyahs = weak,
            todayEpochDay = 100L,
        )

        val manzil = sessions.first { it.tier == HifzSessionTier.MANZIL }
        assertEquals(weak, manzil.targets)
    }

    @Test
    fun dueDaysOnOrBeforeTodayAppearInManzilAndFutureDueDoNot() {
        val sessions = buildSrsSessions(
            masteryMap = mapOf(
                (1 to 1) to MasteryStatus.MASTERED,
                (1 to 2) to MasteryStatus.MASTERED,
                (1 to 3) to MasteryStatus.MASTERED,
            ),
            dueDayMap = mapOf(
                (1 to 1) to 98L,   // overdue
                (1 to 2) to 100L,  // due today
                (1 to 3) to 103L,  // future -> Sabqi
            ),
            weakAyahs = emptyList(),
            todayEpochDay = 100L,
        )

        val manzil = sessions.first { it.tier == HifzSessionTier.MANZIL }
        val sabqi = sessions.first { it.tier == HifzSessionTier.SABQI }

        assertEquals(listOf(1 to 1, 1 to 2), manzil.targets)
        assertEquals(listOf(1 to 3), sabqi.targets)
    }

    @Test
    fun learningAndRecentlyPracticedAyahsPopulateSabqiAndAdvanceSabaq() {
        val fatihahMastered = (1..7).associate { (1 to it) to MasteryStatus.MASTERED }
        val mastery = fatihahMastered + ((114 to 1) to MasteryStatus.NEW)
        val dueDays = (1..7).associate { (1 to it) to 105L }

        val sessions = buildSrsSessions(
            masteryMap = mastery,
            dueDayMap = dueDays,
            weakAyahs = emptyList(),
            todayEpochDay = 100L,
        )

        val sabaq = sessions.first { it.tier == HifzSessionTier.SABAQ }
        val sabqi = sessions.first { it.tier == HifzSessionTier.SABQI }
        val manzil = sessions.first { it.tier == HifzSessionTier.MANZIL }

        // Al-Fatihah is fully practiced and 114:1 is marked NEW (learning), so Sabaq suggests remaining 114:2..114:6.
        assertEquals((2..6).map { 114 to it }, sabaq.targets)
        assertEquals((1..7).map { 1 to it } + listOf(114 to 1), sabqi.targets)
        assertTrue(manzil.targets.isEmpty())
    }
}
