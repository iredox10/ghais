package com.ghais.android

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ghais.data.repository.ScheduleEngine
import com.ghais.data.repository.SchedulesStore

/**
 * Fires scheduled recitations: looks up the schedule, then hands playback to
 * [QuranPlaybackService] via ACTION_PLAY_SCHEDULE, which invokes
 * [SchedulePlayback.run] to queue the tracks, apply the sleep timer and
 * re-arm the next alarm.
 *
 * Also re-programs all alarms on BOOT_COMPLETED.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Schedules"
        private const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    /**
     * Binds SchedulesStore to the last signed-in owner from disk (no
     * network). Without this, a fresh receiver process reads the "local"
     * namespace and misses user schedules saved under their user id.
     */
    private fun bindScheduleOwner() {
        try {
            val owner = com.ghais.data.auth.AuthRepository.cachedSession.value?.userId
                ?: return
            com.ghais.data.repository.SchedulesStore.setOwner(owner)
        } catch (e: Exception) {
            Log.w(TAG, "Owner bind failed; using local namespace.", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val appContext = context.applicationContext
            ScheduleEngine.init(appContext)
            com.ghais.data.auth.AuthRepository.init(appContext)
            if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            ) {
                bindScheduleOwner()
                ScheduleEngine.refresh(SchedulesStore.schedules.value)
                return
            }

            val id = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: run {
                Log.w(TAG, "Alarm fired without schedule id; ignoring.")
                return
            }
            // The receiver usually runs in a fresh process where the store
            // still uses the "local" owner — bind the last signed-in owner
            // from disk first, or the lookup below misses and the recitation
            // never starts.
            bindScheduleOwner()
            val schedule = SchedulesStore.get(id) ?: run {
                Log.w(TAG, "Schedule $id not found; ignoring.")
                return
            }
            if (!schedule.enabled) {
                Log.i(TAG, "Schedule $id disabled; ignoring.")
                return
            }

            Log.i(TAG, "dispatching schedule $id")
            QuranPlaybackService.startWithSchedule(appContext, id)
        } catch (e: Exception) {
            Log.e(TAG, "ScheduleAlarmReceiver failed", e)
        }
    }
}
