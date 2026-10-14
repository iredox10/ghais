package com.ghais.data.share

import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

actual fun appVersionName(): String {
    val info = platform.Foundation.NSBundle.mainBundle.infoDictionary
    val version = info?.get("CFBundleShortVersionString") as? String ?: "?"
    val build = info?.get("CFBundleVersion") as? String ?: "?"
    return "Ghaith $version ($build)"
}

actual fun deviceSummary(): String {
    val device = UIDevice.currentDevice
    return "${device.model} · ${device.systemName} ${device.systemVersion}"
}

/**
 * Opens `googlegmail:///co?to=&subject=&body=` (Gmail compose). `openURL`
 * with the completion handler needs no `LSApplicationQueriesSchemes` entry:
 * when Gmail is absent the completion returns false and we fall back to a
 * `mailto:` URL handled by Apple Mail / the default client.
 */
actual fun openSupportMail(kind: SupportKind, accountEmail: String?) {
    val subject = supportSubject(kind)
    val body = buildSupportBody(kind, accountEmail)
    val gmail = NSURLComponents().apply {
        scheme = "googlegmail"
        path = "/co"
        queryItems = listOf(
            NSURLQueryItem(name = "to", value = SUPPORT_EMAIL),
            NSURLQueryItem(name = "subject", value = subject),
            NSURLQueryItem(name = "body", value = body)
        )
    }.URL
    val app = UIApplication.sharedApplication
    if (gmail != null) {
        app.openURL(gmail, options = emptyMap<Any?, Any>(), completionHandler = { opened ->
            if (!opened) openMailtoFallback(app, subject, body)
        })
    } else {
        openMailtoFallback(app, subject, body)
    }
}

private fun openMailtoFallback(app: UIApplication, subject: String, body: String) {
    val url = NSURLComponents().apply {
        scheme = "mailto"
        path = SUPPORT_EMAIL
        queryItems = listOf(
            NSURLQueryItem(name = "subject", value = subject),
            NSURLQueryItem(name = "body", value = body)
        )
    }.URL ?: return
    app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}
