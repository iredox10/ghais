package com.ghais.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.ghais.data.repository.RecitationSchedule
import java.util.Locale

/**
 * Posts user-facing notifications for scheduled recitations.
 *
 * Exists because a schedule must never fail silently: `autoStart = false`
 * reminders and every playback failure from [SchedulePlayback] reach the
 * user here even when no UI is on screen (alarm/service process).
 *
 * All paths are fully guarded and never throw; problems are logged under
 * TAG "Schedules" like the receivers. Channel `schedule_alerts` is created
 * idempotently before every post.
 */
object ScheduleStartNotifier {

    private const val TAG = "Schedules"
    private const val CHANNEL_ID = "schedule_alerts"
    private const val CHANNEL_NAME = "Recitation schedules"

    // Notification ids stay in dedicated bands (2000..2499 reminders,
    // 2500..2999 failures) so they can never collide with the media
    // notification (1001) that QuranPlaybackService owns.
    private const val REMINDER_ID_BASE = 2000
    private const val FAILURE_ID_BASE = 2500
    private const val ID_SPAN = 500

    /**
     * Heads-up reminder for a schedule with `autoStart = false`
     * (the caller decides when to show it).
     */
    fun showReminder(context: Context, schedule: RecitationSchedule) {
        try {
            ensureChannel(context)
            val time = try {
                String.format(Locale.US, "%02d:%02d", schedule.hour, schedule.minute)
            } catch (e: Exception) {
                Log.w(TAG, "time format failed for ${schedule.id}", e)
                "--:--"
            }
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("Recitation scheduled")
                .setContentText("Your recitation is set for $time — open to review")
                .setSmallIcon(R.drawable.ghaith_mark)
                .setAutoCancel(true)
            launchIntent(context, schedule.id.hashCode())?.let { builder.setContentIntent(it) }
            notify(
                context,
                REMINDER_ID_BASE + Math.floorMod(schedule.id.hashCode(), ID_SPAN),
                builder.build(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "showReminder failed", e)
        }
    }

    /** Notifies that a schedule did not start; [msg] is the reason text. */
    fun showFailure(context: Context, schedule: RecitationSchedule?, msg: String) {
        try {
            ensureChannel(context)
            val text = try {
                msg.ifBlank { "Tap to open Ghaith" }
            } catch (e: Exception) {
                Log.w(TAG, "failure text failed", e)
                "Tap to open Ghaith"
            }
            val key = schedule?.id ?: "fail"
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("Recitation couldn't start")
                .setContentText(text)
                .setSmallIcon(R.drawable.ghaith_mark)
                .setAutoCancel(true)
            launchIntent(context, key.hashCode())?.let { builder.setContentIntent(it) }
            notify(
                context,
                FAILURE_ID_BASE + Math.floorMod(key.hashCode(), ID_SPAN),
                builder.build(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "showFailure failed", e)
        }
    }

    /**
     * Creates `schedule_alerts` (IMPORTANCE_HIGH, heads-up) if absent —
     * mirrors the defensive idiom of QuranPlaybackService's
     * `createNotificationChannel`. Never throws.
     */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (try { manager.getNotificationChannel(CHANNEL_ID) } catch (_: Exception) { null } != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Reminders and failures for scheduled recitations"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.w(TAG, "ensureChannel failed", e)
        }
    }

    /** Launch intent (open the app) wrapped as an immutable PendingIntent. */
    private fun launchIntent(context: Context, requestCode: Int): PendingIntent? {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return null
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } catch (e: Exception) {
            Log.w(TAG, "launch PendingIntent failed", e)
            null
        }
    }

    /**
     * Posts via the plain framework NotificationManager (same idiom as
     * QuranPlaybackService); skips and logs on API 33+ without
     * POST_NOTIFICATIONS. Never throws.
     */
    private fun notify(context: Context, id: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping notification $id")
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager == null) {
                Log.w(TAG, "NotificationManager unavailable; dropping notification $id")
                return
            }
            manager.notify(id, notification)
        } catch (e: Exception) {
            Log.w(TAG, "notify($id) failed", e)
        }
    }
}
