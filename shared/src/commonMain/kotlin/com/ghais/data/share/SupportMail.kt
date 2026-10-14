package com.ghais.data.share

/**
 * Support entry points.
 *
 * Tapping a row opens the Gmail app straight into a compose window with the
 * recipient, subject and a diagnostics footer pre-filled — the user only
 * writes their message and hits send. No Ghaith-side composer, no copy/paste.
 *
 * - Android: `ACTION_SEND` addressed to `com.google.android.gm` when
 *   installed, otherwise a generic email chooser.
 * - iOS: `googlegmail:///co?to=&subject=&body=`, falling back to `mailto:`.
 */
enum class SupportKind { BUG, FEATURE, CONTACT }

/** Inbox that receives every support mail. */
const val SUPPORT_EMAIL = "iredoxtech@gmail.com"

/** Prefilled subject per support channel. */
fun supportSubject(kind: SupportKind): String = when (kind) {
    SupportKind.BUG -> "[Ghaith] Bug report"
    SupportKind.FEATURE -> "[Ghaith] Feature suggestion"
    SupportKind.CONTACT -> "[Ghaith] Contact"
}

/**
 * Prefilled body: a blank writing area up top (Gmail puts the cursor at the
 * start), diagnostics below the fold so every report is actionable without
 * a back-and-forth asking for app version / device / OS.
 */
fun buildSupportBody(kind: SupportKind, accountEmail: String?): String {
    val prompt = when (kind) {
        SupportKind.BUG -> "What happened:"
        SupportKind.FEATURE -> "Your idea:"
        SupportKind.CONTACT -> "Your message:"
    }
    val user = accountEmail?.trim().orEmpty().ifEmpty { "guest" }
    return buildString {
        appendLine(prompt)
        appendLine()
        appendLine()
        appendLine("---")
        appendLine("App: ${appVersionName()}")
        appendLine("Device: ${deviceSummary()}")
        append("User: $user")
    }
}

/** e.g. "Ghaith 1.0 (12)" — platform-specific. */
expect fun appVersionName(): String

/** e.g. "Pixel 6 · Android 14 (SDK 34)" — platform-specific. */
expect fun deviceSummary(): String

/** Opens Gmail compose with [SUPPORT_EMAIL], subject and body pre-filled. */
expect fun openSupportMail(kind: SupportKind, accountEmail: String?)
