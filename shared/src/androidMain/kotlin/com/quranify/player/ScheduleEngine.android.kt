package com.quranify.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Android actual: programs one exact daily alarm per enabled schedule.
 *
 * No dependency on the app module (see PlayerBridge.onPlayRequested note):
 * the alarm [Intent] targets the receiver by explicit class name.
 */
actual object ScheduleEngine {
    private const val TAG = "Schedules"
    private const val EXTRA_SCHEDULE_ID = "schedule_id"
    private const val RECEIVER_CLASS = "com.quranify.android.ScheduleAlarmReceiver"

    private var appContext: Context? = null
    private var previousIds: Set<String> = emptySet()

    /**
     * Must be called once with an application context (e.g. from
     * MainActivity.onCreate next to the other bridge inits, and from
     * ScheduleAlarmReceiver which self-heals if this was missed).
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun refresh(schedules: List<RecitationSchedule>) {
        try {
            val ctx = appContext ?: return
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val enabled = schedules.filter { it.enabled }
            val currentIds = enabled.map { it.id }.toSet()

            // Cancel stale alarms: every id we programmed before plus every
            // id we are about to program (re-program to avoid duplicates).
            for (id in previousIds union currentIds) {
                try {
                    val existing = pendingIntentFor(ctx, id, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                    if (existing != null) {
                        alarmManager.cancel(existing)
                        existing.cancel()
                    }
                } catch (_: Exception) { }
            }

            val exactAllowed =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            for (schedule in enabled) {
                try {
                    val triggerAt = nextOccurrenceMillis(schedule.hour, schedule.minute)
                    val pi = pendingIntentFor(
                        ctx,
                        schedule.id,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ) ?: continue
                    if (exactAllowed) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi,
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi,
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule alarm for ${schedule.id}", e)
                }
            }
            previousIds = currentIds
        } catch (e: Exception) {
            Log.e(TAG, "ScheduleEngine.refresh failed", e)
        }
    }

    private fun nextOccurrenceMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    private fun pendingIntentFor(ctx: Context, scheduleId: String, flags: Int): PendingIntent? {
        return try {
            val intent = Intent().setClassName(ctx.packageName, RECEIVER_CLASS)
                .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            PendingIntent.getBroadcast(ctx, scheduleId.hashCode(), intent, flags)
        } catch (_: Exception) {
            null
        }
    }
}
