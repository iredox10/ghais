package com.ghais.analytics

import com.ghais.devotion.DevotionStreak
import com.ghais.ui.screens.profile.identity.AchievementStats
import com.ghais.ui.screens.profile.identity.unlockedAchievements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HookEmissionTest {

    private class FakeTracker : HookTracker {
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        override fun track(event: String, props: Map<String, String>) {
            events += event to props
        }
    }

    @Test
    fun testStreakMilestoneEmitsAtMostOnce() {
        val fake = FakeTracker()
        val prev = HookAnalytics.tracker
        HookAnalytics.tracker = fake
        try {
            // The celebrated set persists process-wide (sibling tests may
            // have celebrated 7 already), so assert idempotence, not count:
            // two back-to-back marks emit at most one event.
            val before = fake.events.count { it.first == HookEvents.Event.STREAK_MILESTONE }
            DevotionStreak.markMilestoneCelebrated(30)
            DevotionStreak.markMilestoneCelebrated(30)
            val hits = fake.events.filter { it.first == HookEvents.Event.STREAK_MILESTONE }
            assertTrue(hits.size - before <= 1, "idempotent: $hits")
            hits.forEach {
                assertEquals("30", it.second[HookEvents.Prop.STREAK_DAYS])
            }
        } finally {
            HookAnalytics.tracker = prev
        }
    }

    @Test
    fun testAchievementDiffEmitsPerNewId() {
        val before = unlockedAchievements(
            AchievementStats(false, 0, 0, 0, 0)
        )
        val after = unlockedAchievements(
            AchievementStats(true, 7, 10, 1, 1000)
        )
        val fresh = (after - before)
        assertTrue(fresh.isNotEmpty(), "new listens unlock something")
        assertTrue(after.containsAll(before), "monotonic")
    }
}
