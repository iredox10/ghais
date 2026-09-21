package com.ghais.data.share

/**
 * Cross-platform share sheet for plain-text shares (routine links, etc.).
 *
 * Platform notes: the live implementation is Android-only (`ShareSheet.init`
 * must be called once from `MainActivity.onCreate`, alongside the existing
 * `SyncEngine.init` / `AuthRepository.init` lines); iOS is a no-op stub until
 * a `UIActivityViewController` binding lands. Callers should gate on
 * [isAvailable] and fall back to clipboard copy when unavailable.
 */
expect object ShareSheet {

    /** Opens the system share sheet for [text] with chooser title [title]. */
    fun shareText(title: String, text: String)

    /** Whether sharing is wired on this platform (Android: true; iOS stub: false). */
    fun isAvailable(): Boolean
}
