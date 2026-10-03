package com.ghais.data.repository

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
    private const val RECEIVER_CLASS = "com.ghais.android.ScheduleAlarmReceiver"

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
            if (!exactAllowed) {
                Log.w(TAG, "exactAllowed=false; using inexact alarms. Grant exact alarms under Settings -> Alarm access for on-time recitation reminders.")
            }
            val programmed = mutableListOf<Long>()
            for (schedule in enabled) {
                try {
                    val triggerAt = nextTriggerFor(schedule.hour, schedule.minute)
                    val pi = pendingIntentFor(
                        ctx,
                        schedule.id,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ) ?: continue
                    if (exactAllowed) {
                        // Alarm-clock (not setExact): the documented way to
                        // start foreground audio from the background on
                        // Android 12+ — setExact delivery does NOT grant the
                        // FGS-start exemption, so playback would die silently.
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(triggerAt, null),
                            pi,
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi,
                        )
                    }
                    programmed += triggerAt
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule alarm for ${schedule.id}", e)
                }
            }
            previousIds = currentIds
            val nextTriggerAt = programmed.minOrNull()
            Log.d(TAG, "refresh: ${enabled.size} enabled, exactAllowed=$exactAllowed, next trigger=${formatHourMinute(nextTriggerAt)}")
        } catch (e: Exception) {
            Log.e(TAG, "ScheduleEngine.refresh failed", e)
        }
    }

    actual fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val ctx = appContext ?: return true // pre-init: don't nag before we can open the grant screen
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
        return try { am.canScheduleExactAlarms() } catch (_: Exception) { true }
    }

    actual fun openExactAlarmSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val ctx = appContext ?: return false
        return try {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(android.net.Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not open exact-alarm settings", e)
            false
        }
    }

    internal fun nextOccurrenceMillis(hour: Int, minute: Int): Long =
        nextTriggerFor(hour, minute)

    internal fun nextTriggerFor(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    private fun formatHourMinute(millis: Long?): String {
        if (millis == null) return "-"
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
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
