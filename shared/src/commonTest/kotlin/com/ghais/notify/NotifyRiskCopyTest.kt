package com.ghais.notify

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotifyRiskCopyTest {

    @Test
    fun testBehindCopyNamesPagesWithoutGuilt() {
        val copy = copyFor(NotifyChannel.KHATMA_PACE, isBehind = true, catchUpPages = 3)
        assertTrue("3" in copy.body, "body=${copy.body}")
        listOf("missed", "lost", "broken", "streak").forEach { banned ->
            assertFalse(banned in copy.body.lowercase(), "banned='$banned' in ${copy.body}")
        }
    }

    @Test
    fun testOnTrackDelegatesToBase() {
        assertTrue(
            copyFor(NotifyChannel.KHATMA_PACE, isBehind = false, catchUpPages = 0) ==
                copyFor(NotifyChannel.KHATMA_PACE)
        )
    }

    @Test
    fun testOtherChannelsDelegate() {
        assertTrue(
            copyFor(NotifyChannel.STREAK_SAFE, isBehind = true, catchUpPages = 5) ==
                copyFor(NotifyChannel.STREAK_SAFE)
        )
    }

    @Test
    fun testSinglePageSingular() {
        val copy = copyFor(NotifyChannel.KHATMA_PACE, isBehind = true, catchUpPages = 1)
        assertTrue("1 page brings" in copy.body, "body=${copy.body}")
    }
}
