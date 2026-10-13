package com.ghais.data.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the support email template ([buildSupportBody]/[supportSubject]):
 * the user's words on top, diagnostics (app version, device, OS, account)
 * below the fold — so a report arriving at [SUPPORT_EMAIL] is actionable.
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
    fun testBugBodyCarriesUserTextAndDiagnostics() {
        val body = buildSupportBody(SupportKind.BUG, "audio stops", "a@b.c")
        assertTrue(body.contains("What happened:"))
        assertTrue(body.contains("audio stops"))
        assertTrue(body.contains("App:"))
        assertTrue(body.contains("Device:"))
        assertTrue(body.contains("OS:"))
        assertTrue(body.contains("User: a@b.c"))
    }

    @Test
    fun testGuestAccountFallsBackToGuestLabel() {
        val body = buildSupportBody(SupportKind.FEATURE, "dark mode", null)
        assertTrue(body.contains("Your idea:"))
        assertTrue(body.contains("dark mode"))
        assertTrue(body.contains("User: guest"))
    }

    @Test
    fun testBlankAccountEmailFallsBackToGuestLabel() {
        val body = buildSupportBody(SupportKind.CONTACT, "hello", "  ")
        assertTrue(body.contains("Your message:"))
        assertTrue(body.contains("User: guest"))
    }
}
