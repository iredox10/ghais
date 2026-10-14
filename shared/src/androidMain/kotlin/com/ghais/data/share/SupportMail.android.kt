package com.ghais.data.share

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.lang.ref.WeakReference

private const val GMAIL_PACKAGE = "com.google.android.gm"

/**
 * Foreground activity holder: the compose intent needs an Activity context.
 * Wired from `MainActivity.onCreate`; weak so it never leaks.
 */
object SupportMailBackend {
    var activity: WeakReference<Activity>? = null
}

actual fun appVersionName(): String {
    val activity = SupportMailBackend.activity?.get() ?: return "Ghaith unknown"
    return try {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        "Ghaith ${info.versionName} ($code)"
    } catch (_: Exception) {
        "Ghaith unknown"
    }
}

actual fun deviceSummary(): String {
    val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }.orEmpty()
    val model = Build.MODEL.orEmpty()
    val device = listOf(manufacturer, model).filter { it.isNotBlank() }.joinToString(" ")
    return "$device · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
}

actual fun openSupportMail(kind: SupportKind, accountEmail: String?) {
    val activity = SupportMailBackend.activity?.get() ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, supportSubject(kind))
        putExtra(Intent.EXTRA_TEXT, buildSupportBody(kind, accountEmail))
    }
    // Prefer the Gmail app when present (needs the <queries> entry in the
    // manifest on API 30+); otherwise any email client via chooser.
    val gmailIntent = Intent(intent).setPackage(GMAIL_PACKAGE)
    val launch: Intent = if (gmailIntent.resolveActivity(activity.packageManager) != null) {
        gmailIntent
    } else {
        Intent.createChooser(intent, "Send email")
    }
    try {
        activity.startActivity(launch)
    } catch (_: Exception) {
        Toast.makeText(activity, "No email app found", Toast.LENGTH_LONG).show()
    }
}
