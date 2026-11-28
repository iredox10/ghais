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
    fun `streakDays and bestStreakDays compute consecutive completed days`() {
        val plan = HifzListeningPlan(
            id = "plan-streak",
            surahIds = listOf(1),
            sessionsPerDay = 2,
            completedSessionsByDay = mapOf(
                100L to 2,
                101L to 3,
                102L to 2,
                // gap on 103
                104L to 2,
                105L to 1, // incomplete on 105
            ),
        )

        // Completed on 102 -> streak ending at 102 is 3
        assertEquals(3, plan.streakDays(102L))
        // Incomplete on 103, but 102 is completed -> grace streak is still 3
        assertEquals(3, plan.streakDays(103L))
        // Incomplete on 105, yesterday 104 is completed -> streak is 1
        assertEquals(1, plan.streakDays(105L))
        // Gap of 2 days (106, 105 incomplete) -> streak is 0
        assertEquals(0, plan.streakDays(106L))
        // Best streak across history is 3 (days 100..102)
        assertEquals(3, plan.bestStreakDays())
    }

    @Test
    fun `estimatedMinutesPerSession estimates from surah ayahs and repeats`() {
        val plan = HifzListeningPlan(
            id = "plan-est",
            surahIds = listOf(2), // Al-Baqarah: 286 ayahs * 7s * 1 = 2002s -> 33 min
            timesPerSession = 1,
        )
        assertEquals(33, plan.estimatedMinutesPerSession())

        val tinyPlan = HifzListeningPlan(
            id = "plan-est-tiny",
            surahIds = listOf(108), // Al-Kawthar: 3 ayahs * 7s * 1 = 21s -> coerced to 1 min
            timesPerSession = 1,
        )
        assertEquals(1, tinyPlan.estimatedMinutesPerSession())
    }

    @Test
    fun `escalateReminder creates and cleans up escalation schedule`() {
        HifzListeningPlannerStore.setOwner("planner-test-esc")
        val plan = HifzListeningPlannerStore.savePlan(
            HifzListeningPlan(
                id = "plan-esc",
                title = "Al-Kahf",
                reciterSlugs = listOf("husary"),
                surahIds = listOf(18),
                reminderEnabled = true,
                reminderHour = 19,
                reminderMinute = 15,
                autoPlayOnSchedule = true,
                escalateReminder = true,
            )
        )
        val primaryId = HifzListeningPlannerStore.scheduleIdFor(plan.id)
        val escId = HifzListeningPlannerStore.escalationScheduleIdFor(plan.id)

        val primary = SchedulesStore.get(primaryId)
        val esc = SchedulesStore.get(escId)
        assertEquals(true, primary?.autoStart)
        assertEquals(19, primary?.hour)
        assertEquals(15, primary?.minute)
        assertEquals(false, esc?.autoStart)
        assertEquals(21, esc?.hour)
        assertEquals(15, esc?.minute)
        assertTrue(HifzListeningPlannerStore.isEscalationScheduleId(escId))
        assertEquals(plan.id, HifzListeningPlannerStore.planIdFromScheduleId(escId))
        assertEquals(plan.id, HifzListeningPlannerStore.planIdFromScheduleId(primaryId))

        // Disabling escalateReminder removes the escalation schedule
        HifzListeningPlannerStore.savePlan(plan.copy(escalateReminder = false))
        assertEquals(null, SchedulesStore.get(escId))

        // Re-enable and then deletePlan removes both
        HifzListeningPlannerStore.savePlan(plan.copy(escalateReminder = true))
        assertTrue(SchedulesStore.get(escId) != null)
        HifzListeningPlannerStore.deletePlan(plan.id)
        assertEquals(null, SchedulesStore.get(primaryId))
        assertEquals(null, SchedulesStore.get(escId))
        HifzListeningPlannerStore.setOwner("local")
    }

    @Test
    fun `recordSessionCompleted emits PlanCompletionEvent and dismiss clears it`() {
        HifzListeningPlannerStore.setOwner("planner-test-event")
        HifzListeningPlannerStore.dismissCompletionEvent()
        val startDay = 20_100L
        val plan = HifzListeningPlannerStore.savePlan(
            HifzListeningPlan(
                id = "plan-evt",
                title = "Yasin",
                reciterSlugs = listOf("husary"),
                surahIds = listOf(36),
                sessionsPerDay = 2,
                startEpochDay = startDay,
            )
        )

        HifzListeningPlannerStore.recordSessionCompleted(plan.id, startDay)
        val firstEvent = HifzListeningPlannerStore.lastCompletionEvent.value
        assertEquals(plan.id, firstEvent?.planId)
        assertEquals("Yasin", firstEvent?.planTitle)
        assertEquals(1, firstEvent?.sessionsDoneToday)
        assertEquals(2, firstEvent?.sessionsPerDay)
        assertEquals(false, firstEvent?.dayCompleted)
        assertEquals(0, firstEvent?.streak)

        HifzListeningPlannerStore.recordSessionCompleted(plan.id, startDay)
        val secondEvent = HifzListeningPlannerStore.lastCompletionEvent.value
        assertEquals(2, secondEvent?.sessionsDoneToday)
        assertEquals(true, secondEvent?.dayCompleted)
        assertEquals(1, secondEvent?.streak)

        HifzListeningPlannerStore.dismissCompletionEvent()
        assertEquals(null, HifzListeningPlannerStore.lastCompletionEvent.value)

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
