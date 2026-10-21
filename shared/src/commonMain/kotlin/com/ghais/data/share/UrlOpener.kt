package com.ghais.data.share

/**
 * Opens an external URL in the platform's default handler — Play Store
 * links, web pages, whatever a screen needs. Lives beside ShareSheet and
 * SupportMail:
 *
 * - Android: `ACTION_VIEW`; the Play Store app claims play.google.com links
 *   on GMS devices, otherwise the browser shows the listing page. Wired
 *   through the same foreground-activity holder as openSupportMail
 *   (SupportMailBackend, set once in MainActivity.onCreate).
 * - iOS: `UIApplication.openURL`, same call shape as the Gmail compose path
 *   in SupportMail.ios.kt.
 */
expect fun openUrl(url: String)
