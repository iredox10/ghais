package com.ghais.data.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * Android live implementation of the `SupportMail` expect object declared in
 * commonMain.
 *
 * Needs an Android context for `startActivity` and `PackageManager`: the owner
 * must call `SupportMail.init(this)` once in `MainActivity.onCreate`
 * (alongside the existing `ShareSheet.init(this)` line). Actual objects may
 * declare members beyond the expect contract.
 *
 * Mails via `ACTION_SENDTO mailto:` + `createChooser` (mail clients only, not
 * every share target); `FLAG_ACTIVITY_NEW_TASK` is set because the cached
 * context is the application context. Never throws — failures return false so
 * the UI can show its fallback.
 */
actual object SupportMail {

    private const val TAG = "GhaisSupportMail"

    @Volatile
    private var appContext: android.content.Context? = null

    /**
     * Extra (non-expect) member: caches the app context.
     * MUST be called once from MainActivity.onCreate:
     * `com.ghais.data.share.SupportMail.init(this)`.
     */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    actual fun deviceSummary(): String {
        val ctx = appContext
        val app = try {
            @Suppress("DEPRECATION")
            ctx?.packageManager?.getPackageInfo(ctx.packageName, 0)?.versionName
                ?.trim()?.takeIf { it.isNotEmpty() } ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
        val device = try {
            val maker = Build.MANUFACTURER?.trim().orEmpty()
            val model = Build.MODEL?.trim().orEmpty()
            "$maker $model".trim().ifBlank { "unknown" }
        } catch (_: Exception) {
            "unknown"
        }
        val os = try {
            "Android ${Build.VERSION.RELEASE ?: "?"} (SDK ${Build.VERSION.SDK_INT})"
        } catch (_: Exception) {
            "Android unknown"
        }
        return "App: $app\nDevice: $device\nOS: $os"
    }

    actual fun composeEmail(to: String, subject: String, body: String): Boolean {
        val ctx = appContext
        if (ctx == null) {
            Log.e(TAG, "SupportMail.init(context) has not been called yet.")
            return false
        }
        if (to.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${to.trim()}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(ctx.packageManager) == null) return false
            ctx.startActivity(
                Intent.createChooser(intent, "Send email")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "composeEmail failed", e)
            false
        }
    }

    actual fun isAvailable(): Boolean = true
}
