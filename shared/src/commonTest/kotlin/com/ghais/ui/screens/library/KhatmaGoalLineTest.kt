package com.ghais.ui.screens.library

import com.ghais.domain.model.KhatmaPlan
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KhatmaGoalLineTest {

    private fun planAt(read: Int, cursorSurah: Int = 2, cursorAyah: Int = 1) = KhatmaPlan(
        id = "goal",
        title = "Goal",
        targetDays = 30,
        currentSurahId = cursorSurah,
        currentAyahNo = cursorAyah,
        streak = 0,
        startDateMs = 0L,
        lastProgressMs = 1_000L,
        totalAyahsRead = read,
    )

    @Test
    fun testNullBelow85() {
        // Plan starting at 2:1: total = read + remaining; with 0 read the
        // progress is 0 — well below the line.
        assertNull(khatmaGoalLine(planAt(0), 0.5f, false))
    }

    @Test
    fun testNullWhenComplete() {
        assertNull(khatmaGoalLine(planAt(6000), 1f, true))
    }

    @Test
    fun testMentionsJuzAbove85() {
        val line = khatmaGoalLine(planAt(6000), 0.9f, false)
        assertTrue(line != null && "Juz" in line, "line=$line")
    }
}
