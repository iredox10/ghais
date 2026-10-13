package com.ghais.data.share

/**
 * Support channel: bug reports, feature suggestions and contact messages go
 * out as a pre-filled email to the support address.
 *
 * The template ([buildBody]) always carries the diagnostics the support
 * address needs: app version, device model and OS version, plus the signed-in
 * account when there is one. Callers only supply the user's own words.
 *
 * Platform notes: Android fires `ACTION_SENDTO mailto:` with a chooser;
 * iOS is a no-op stub until a `MFMailComposeViewController` binding lands —
 * gate on [isAvailable].
 */
expect object SupportMail {

    /** Multi-line diagnostics block for the template (`App:… / Device:… / OS:…`). */
    fun deviceSummary(): String

    /**
     * Opens the mail client with [to]/[subject]/[body] pre-filled.
     * Returns false when no mail client handled it (caller shows a fallback).
     */
    fun composeEmail(to: String, subject: String, body: String): Boolean

    /** Whether composing is wired on this platform (Android: true; iOS stub: false). */
    fun isAvailable(): Boolean
}

/** Support inbox for every channel below. */
const val SUPPORT_EMAIL = "iredoxtech@gmail.com"

/** Email subject per channel. */
fun supportSubject(kind: SupportKind): String = when (kind) {
    SupportKind.BUG -> "[Ghaith] Bug report"
    SupportKind.FEATURE -> "[Ghaith] Feature suggestion"
    SupportKind.CONTACT -> "[Ghaith] Contact"
}

/** Report / suggestion / contact channels. */
enum class SupportKind {
    BUG,
    FEATURE,
    CONTACT
}

/**
 * Full email body: the user's words on top, diagnostics below the fold.
 * [accountEmail] is null for guests.
 */
fun buildSupportBody(
    kind: SupportKind,
    userText: String,
    accountEmail: String?,
): String {
    val prompt = when (kind) {
        SupportKind.BUG -> "What happened:"
        SupportKind.FEATURE -> "Your idea:"
        SupportKind.CONTACT -> "Your message:"
    }
    return buildString {
        appendLine(prompt)
        appendLine(userText.trim())
        appendLine()
        appendLine("---")
        appendLine(SupportMail.deviceSummary())
        appendLine("User: ${accountEmail?.takeIf { it.isNotBlank() } ?: "guest"}")
    }
}
