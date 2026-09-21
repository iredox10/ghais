package com.ghais.data.share

import android.content.Intent
import android.util.Log

/**
 * Android live implementation of the `ShareSheet` expect object declared in
 * commonMain.
 *
 * Needs an Android context for `startActivity`: the owner must call
 * `ShareSheet.init(this)` once in `MainActivity.onCreate` (alongside the
 * existing `AuthRepository.init(this)` / `SyncEngine.init(this)` lines).
 * Shares via `ACTION_SEND` + `createChooser`; `FLAG_ACTIVITY_NEW_TASK` is set
 * because the cached context is the application context.
 */
actual object ShareSheet {

    private const val TAG = "GhaisShare"

    @Volatile
    private var appContext: android.content.Context? = null

    /**
     * Extra (non-expect) member: caches the app context for `startActivity`.
     * Actual objects may declare members beyond the expect contract.
     * MUST be called once from MainActivity.onCreate:
     * `com.ghais.data.share.ShareSheet.init(this)`.
     */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    actual fun shareText(title: String, text: String) {
        val ctx = appContext
        if (ctx == null) {
            Log.e(TAG, "ShareSheet.init(context) has not been called yet.")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(
                Intent.createChooser(intent, title)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(TAG, "shareText failed", e)
        }
    }

    actual fun isAvailable(): Boolean = true
}
