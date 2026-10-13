package com.ghais.data.share

/**
 * iOS stub: no `MFMailComposeViewController` binding yet, so composing is
 * unavailable and [isAvailable] reports false (callers show their fallback).
 */
actual object SupportMail {

    actual fun deviceSummary(): String = "App: unknown\nDevice: unknown\niOS: unknown"

    actual fun composeEmail(to: String, subject: String, body: String): Boolean = false

    actual fun isAvailable(): Boolean = false
}
