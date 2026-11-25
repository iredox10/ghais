package com.ghais.player

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ghais.data.repository.QuranDataRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Notification bar progress & controls for [QuranDownloads]:
 * - Shows active surah name + aggregate percentage progress bar.
 * - Provides Pause / Resume and Cancel actions directly in the notification shade.
 * - Surfaces "Waiting for connection..." when downloads are queued for network recovery.
 */
internal object QuranDownloadNotifier {
    private const val TAG = "QuranDownloadNotifier"
    private const val CHANNEL_ID = "ghais_downloads"
    private const val NOTIFICATION_ID = 4401

    private const val ACTION_PAUSE_ALL = "com.ghais.downloads.ACTION_PAUSE_ALL"
    private const val ACTION_RESUME_ALL = "com.ghais.downloads.ACTION_RESUME_ALL"
    private const val ACTION_CANCEL_ALL = "com.ghais.downloads.ACTION_CANCEL_ALL"

    private val registered = AtomicBoolean(false)

    @Volatile
    private var lastNotifyMs = 0L

    @Volatile
    private var lastPercent = -1

    @Volatile
    private var lastStateTag = ""

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE_ALL -> QuranDownloads.pauseAll()
                ACTION_RESUME_ALL -> QuranDownloads.resumeAll()
                ACTION_CANCEL_ALL -> QuranDownloads.cancelAll()
            }
        }
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        ensureChannel(appCtx)
        if (registered.compareAndSet(false, true)) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PAUSE_ALL)
                addAction(ACTION_RESUME_ALL)
                addAction(ACTION_CANCEL_ALL)
            }
            try {
                ContextCompat.registerReceiver(
                    appCtx,
                    actionReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register download action receiver", e)
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Surah Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows offline Quran download progress and pause/resume controls"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun update(
        context: Context?,
        progress: Map<String, Float>,
        pausedCount: Int,
        waitingNetworkCount: Int,
        force: Boolean = false,
    ) {
        val ctx = context?.applicationContext ?: return
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)

        val nm = NotificationManagerCompat.from(ctx)
        if (progress.isEmpty() && pausedCount == 0 && waitingNetworkCount == 0) {
            lastPercent = -1
            lastStateTag = ""
            try {
                nm.cancel(NOTIFICATION_ID)
            } catch (_: Exception) {
            }
            return
        }

        val stateTag = when {
            progress.isNotEmpty() -> "active:${progress.size}:$pausedCount:$waitingNetworkCount"
            waitingNetworkCount > 0 -> "waiting:$waitingNetworkCount:$pausedCount"
            else -> "paused:$pausedCount"
        }

        val knownValues = progress.values.filter { it in 0f..1f }
        val pct = if (knownValues.isNotEmpty()) {
            ((knownValues.sum() / knownValues.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            -1
        }

        val now = System.currentTimeMillis()
        if (!force && stateTag == lastStateTag && kotlin.math.abs(pct - lastPercent) < 2 && now - lastNotifyMs < 400L) {
            return
        }
        lastNotifyMs = now
        lastPercent = pct
        lastStateTag = stateTag

        val title = when {
            progress.isNotEmpty() -> describeActiveTitle(progress.keys)
            waitingNetworkCount > 0 -> "Waiting for connection ($waitingNetworkCount)"
            else -> "Downloads paused ($pausedCount)"
        }

        val contentText = when {
            progress.isNotEmpty() && pct >= 0 -> "$pct% • ${progress.size} active"
            progress.isNotEmpty() -> "Downloading ${progress.size} file(s)…"
            waitingNetworkCount > 0 -> "Will auto-resume when back online"
            else -> "Tap Resume to continue from saved progress"
        }

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(
                if (progress.isNotEmpty()) android.R.drawable.stat_sys_download
                else android.R.drawable.stat_sys_download_done,
            )
            .setContentTitle(title)
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(progress.isNotEmpty() || waitingNetworkCount > 0)
            .setSilent(true)

        if (progress.isNotEmpty()) {
            if (pct >= 0) builder.setProgress(100, pct, false)
            else builder.setProgress(0, 0, true)
            builder.addAction(
                0,
                "Pause",
                actionIntent(ctx, ACTION_PAUSE_ALL, 1),
            )
        } else {
            builder.setProgress(0, 0, false)
            if (pausedCount > 0 || waitingNetworkCount > 0) {
                builder.addAction(
                    0,
                    "Resume",
                    actionIntent(ctx, ACTION_RESUME_ALL, 2),
                )
            }
        }

        builder.addAction(
            0,
            "Cancel",
            actionIntent(ctx, ACTION_CANCEL_ALL, 3),
        )

        try {
            nm.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing notification permission", e)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post download notification", e)
        }
    }

    fun showCompleted(context: Context?, surahLabel: String? = null) {
        val ctx = context?.applicationContext ?: return
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        val text = if (surahLabel != null) "$surahLabel ready offline" else "Offline downloads complete"
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(text)
            .setAutoCancel(true)
            .setOngoing(false)
            .setSilent(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun describeActiveTitle(keys: Set<String>): String {
        val first = keys.firstOrNull() ?: return "Downloading Quran"
        val parts = first.split('/')
        val surahId = parts.getOrNull(1)?.toIntOrNull()
        val surahName = surahId?.let { QuranDataRepository.getSurahById(it)?.nameEn }
        return if (keys.size == 1 && surahName != null) {
            "Downloading $surahName"
        } else if (surahName != null) {
            "Downloading $surahName (+${keys.size - 1})"
        } else {
            "Downloading ${keys.size} files"
        }
    }
}
