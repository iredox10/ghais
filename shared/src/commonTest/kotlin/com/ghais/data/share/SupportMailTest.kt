package com.ghais.data.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the Gmail-direct support flow: every row opens a compose window
 * with [SUPPORT_EMAIL], [supportSubject] and [buildSupportBody] pre-filled,
 * so the user only writes their message and hits send.
 */
class SupportMailTest {

    @Test
    fun testSupportAddress() {
        assertEquals("iredoxtech@gmail.com", SUPPORT_EMAIL)
    }

    @Test
    fun testSubjectsPerChannel() {
        assertEquals("[Ghaith] Bug report", supportSubject(SupportKind.BUG))
        assertEquals("[Ghaith] Feature suggestion", supportSubject(SupportKind.FEATURE))
        assertEquals("[Ghaith] Contact", supportSubject(SupportKind.CONTACT))
    }

    @Test
    fun testBugBodyHasPromptAndDiagnostics() {
        val body = buildSupportBody(SupportKind.BUG, "a@b.c")
        assertTrue(body.contains("What happened:"))
        assertTrue(body.contains("App:"))
        assertTrue(body.contains("Device:"))
        assertTrue(body.contains("User: a@b.c"))
    }

    @Test
    fun testFeatureBodyPrompt() {
        val body = buildSupportBody(SupportKind.FEATURE, null)
        assertTrue(body.contains("Your idea:"))
        assertTrue(body.contains("User: guest"))
    }

    @Test
    fun testBlankAccountEmailFallsBackToGuestLabel() {
        val body = buildSupportBody(SupportKind.CONTACT, "  ")
        assertTrue(body.contains("Your message:"))
        assertTrue(body.contains("User: guest"))
    }
}
