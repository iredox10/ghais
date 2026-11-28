package com.ghais.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.ghais.data.repository.HifzListeningPlannerStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.notify.channelForScheduleId
import com.ghais.notify.copyFor
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
            val hifzPlan = try {
                HifzListeningPlannerStore.planIdFromScheduleId(schedule.id)
                    ?.let { HifzListeningPlannerStore.getPlan(it) }
            } catch (_: Exception) {
                null
            }
            val copy = try {
                channelForScheduleId(schedule.id)?.let { copyFor(it) }
            } catch (_: Exception) {
                null
            }
            val (title, text) = if (hifzPlan != null) {
                val epochDay = HifzListeningPlannerStore.currentEpochDay()
                val done = hifzPlan.sessionsDoneOn(epochDay)
                val remaining = (hifzPlan.sessionsPerDay - done).coerceAtLeast(1)
                val estMin = hifzPlan.estimatedMinutesPerSession()
                val streak = hifzPlan.streakDays(epochDay)
                val isEscalation = HifzListeningPlannerStore.isEscalationScheduleId(schedule.id)
                val planLabel = hifzPlan.displayTitle { id ->
                    QuranDataRepository.getSurahById(id)?.nameEn ?: "Surah $id"
                }
                if (isEscalation) {
                    val escTitle = if (streak > 0) {
                        "🔥 Don't lose your ${streak}d streak!"
                    } else {
                        "⏰ Last call for today's plan"
                    }
                    escTitle to "Only ~${estMin}m left for $planLabel ($remaining/${hifzPlan.sessionsPerDay} left before midnight)"
                } else {
                    val primaryTitle = "🎧 Time for $planLabel (~${estMin}m)"
                    val primaryText = if (streak > 0) {
                        "$remaining/${hifzPlan.sessionsPerDay} left today • 🔥 ${streak}d streak"
                    } else {
                        "$remaining of ${hifzPlan.sessionsPerDay} sessions left today"
                    }
                    primaryTitle to primaryText
                }
            } else if (copy != null) {
                copy.title to "${copy.body} ($time)"
            } else {
                "Recitation scheduled" to "Your recitation is set for $time — open to review"
            }
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ghaith_mark)
                .setAutoCancel(true)
            launchIntent(context, schedule.id.hashCode())?.let { builder.setContentIntent(it) }
            if (hifzPlan != null) {
                val estMin = hifzPlan.estimatedMinutesPerSession()
                listenNowIntent(context, schedule.id)?.let { playIntent ->
                    val action = Notification.Action.Builder(
                        null,
                        "▶ Listen Now (~${estMin}m)",
                        playIntent,
                    ).build()
                    builder.addAction(action)
                }
            }
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

    /**
     * Foreground-service PendingIntent that starts playback immediately when
     * the user taps "▶ Listen Now" on a listening-plan reminder.
     */
    private fun listenNowIntent(context: Context, scheduleId: String): PendingIntent? {
        return try {
            val intent = Intent(context, QuranPlaybackService::class.java)
                .setAction(QuranPlaybackService.ACTION_PLAY_SCHEDULE)
                .putExtra(QuranPlaybackService.EXTRA_SCHEDULE_ID, scheduleId)
                .putExtra(QuranPlaybackService.EXTRA_FORCE_PLAY, true)
            val requestCode = scheduleId.hashCode() xor 0x4C495354
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, requestCode, intent, flags)
            } else {
                PendingIntent.getService(context, requestCode, intent, flags)
            }
        } catch (e: Exception) {
            Log.w(TAG, "listenNow PendingIntent failed", e)
            null
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
