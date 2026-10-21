package com.ghais.data.share

import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * `ACTION_VIEW` with the raw URL. The Play Store app handles
 * play.google.com/store links on GMS devices directly (no market:// scheme
 * juggling needed); a stripped build without a handler falls back to a
 * chooser, and failing that a toast — never a crash.
 */
actual fun openUrl(url: String) {
    val activity = SupportMailBackend.activity?.get() ?: return
    try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        try {
            activity.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    "Open link"
                )
            )
        } catch (_: Exception) {
            Toast.makeText(activity, "No app can open this link", Toast.LENGTH_LONG).show()
        }
    }
}
