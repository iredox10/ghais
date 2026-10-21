package com.ghais.data.share

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * `UIApplication.openURL` — same call shape as the Gmail compose path in
 * SupportMail.ios.kt. When no handler exists (unpublished listing) the
 * completion reports false and the call is a no-op; iOS surfaces nothing.
 */
actual fun openUrl(url: String) {
    val link = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(
        link,
        options = emptyMap<Any?, Any>(),
        completionHandler = null
    )
}
