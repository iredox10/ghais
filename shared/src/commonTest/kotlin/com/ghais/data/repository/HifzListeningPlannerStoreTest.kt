package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HifzListeningPlannerStoreTest {

    @Test
    fun `reminder is due only when day is incomplete and within duration`() {
        HifzListeningPlannerStore.setOwner("planner-test-1")
        val startDay = 20_000L
        val plan = HifzListeningPlannerStore.savePlan(
            HifzListeningPlan(
                id = "plan-a",
                title = "Al-Mulk Daily",
                reciterSlugs = listOf("husary"),
                surahIds = listOf(67),
                timesPerSession = 3,
                sessionsPerDay = 2,
                durationDays = 5,
                startEpochDay = startDay,
                reminderEnabled = true,
                reminderHour = 20,
                reminderMinute = 30,
            )
        )

        val scheduleId = HifzListeningPlannerStore.scheduleIdFor(plan.id)

        // Day 1 with 0 sessions -> reminder IS due
        assertTrue(HifzListeningPlannerStore.shouldNotifyIncomplete(scheduleId, startDay))

        // After 1 of 2 sessions -> still incomplete -> reminder IS due
        HifzListeningPlannerStore.recordSessionCompleted(plan.id, startDay)
        assertTrue(HifzListeningPlannerStore.shouldNotifyIncomplete(scheduleId, startDay))

        // After 2 of 2 sessions -> completed for today -> reminder is NOT due
        HifzListeningPlannerStore.recordSessionCompleted(plan.id, startDay)
        assertFalse(HifzListeningPlannerStore.shouldNotifyIncomplete(scheduleId, startDay))

        // Next day (still within 5 days) -> incomplete again -> reminder IS due
        assertTrue(HifzListeningPlannerStore.shouldNotifyIncomplete(scheduleId, startDay + 1))

        // After durationDays (day + 5) -> expired -> reminder is NOT due
        assertFalse(HifzListeningPlannerStore.shouldNotifyIncomplete(scheduleId, startDay + 5))

        // Cleanup
        HifzListeningPlannerStore.deletePlan(plan.id)
        HifzListeningPlannerStore.setOwner("local")
    }

    @Test
    fun `buildSessionTracks repeats each surah timesPerSession across selected reciters`() {
        val plan = HifzListeningPlan(
            id = "plan-b",
            reciterSlugs = listOf("husary"),
            surahIds = listOf(1, 112),
            timesPerSession = 3,
            sessionsPerDay = 2,
            durationDays = 7,
        )
        val tracks = HifzListeningPlannerStore.buildSessionTracks(plan)
        assertEquals(6, tracks.size)
        assertEquals(listOf(1, 1, 1, 112, 112, 112), tracks.map { it.surahId })
    }
}
