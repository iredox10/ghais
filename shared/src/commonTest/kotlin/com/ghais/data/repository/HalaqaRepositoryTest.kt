package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HalaqaRepositoryTest {

    @Test
    fun testCreateJoinLeaveCap() {
        assertTrue(HalaqaRepository.createHalaqa("Test House", "me"))
        assertEquals(listOf("me"), HalaqaRepository.members.value.map { it.userId })
        assertTrue(HalaqaRepository.joinHalaqa("mum"))
        assertTrue(HalaqaRepository.joinHalaqa("mum"))
        assertEquals(2, HalaqaRepository.members.value.size)
        assertTrue(HalaqaRepository.leaveHalaqa("mum"))
        assertEquals(listOf("me"), HalaqaRepository.members.value.map { it.userId })
        HalaqaRepository.leaveHalaqa("me")
    }

    @Test
    fun testBlankAndCapGuards() {
        HalaqaRepository.createHalaqa("T", "me")
        assertFalse(HalaqaRepository.createHalaqa("T", "  "))
        assertFalse(HalaqaRepository.joinHalaqa("  "))
        repeat(10) { HalaqaRepository.joinHalaqa("m$it") }
        assertTrue(HalaqaRepository.members.value.size <= 8)
        HalaqaRepository.leaveHalaqa("me")
        (0..9).forEach { HalaqaRepository.leaveHalaqa("m$it") }
    }

    @Test
    fun testRenameAndPairKey() {
        HalaqaRepository.createHalaqa("T", "me")
        assertTrue(HalaqaRepository.renameMember("me", "Habibi"))
        assertEquals("Habibi", HalaqaRepository.members.value.first().alias)
        assertEquals("a|b", HalaqaRepository.pairKeyOf("b", "a"))
        HalaqaRepository.leaveHalaqa("me")
    }
}
