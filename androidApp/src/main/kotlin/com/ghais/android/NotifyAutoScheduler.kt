package com.ghais.android

import android.content.Context
import android.util.Log
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.UserUsageRepository
import com.ghais.notify.NotifyChannel
import com.ghais.notify.NotifyScheduler
import com.ghais.notify.autoHour
import com.ghais.notify.channelScheduleId
import java.util.Calendar

/**
 * Habitual-hour auto-scheduler (Android only): ensures a gentle daily
 * devotion reminder exists at the user's habitual listening hour without
 * ever overwriting a user-edited time.
 *
 * Hours come from [UserUsageRepository.history] timestamps (device-local
 * hour buckets); empty history falls back to After-Fajr 05:00 via
 * [autoHour]. Doctrine schedules are reminder-only (`autoStart = false`)
 * and fire display through `ScheduleStartNotifier`'s per-channel copy.
 * Never throws.
 */
object NotifyAutoScheduler {
    private const val TAG = "Schedules"

    fun ensureDefaults(appContext: Context) {
        try {
            val id = channelScheduleId(NotifyChannel.DEVOTION_REMINDER)
            val existing = try {
                SchedulesStore.get(id)
            } catch (_: Exception) {
                null
            }
            if (existing != null) return
            val hours = try {
                UserUsageRepository.history.value.mapNotNull { item ->
                    val ts = item.lastPlayedTimestampMs
                    if (ts <= 0L) return@mapNotNull null
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    cal.get(Calendar.HOUR_OF_DAY)
                }
            } catch (_: Exception) {
                emptyList()
            }
            val hour = autoHour(hours)
            try {
                NotifyScheduler.scheduleDaily(hour, 0, NotifyChannel.DEVOTION_REMINDER)
            } catch (e: Exception) {
                Log.w(TAG, "auto devotion reminder failed", e)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureDefaults failed", e)
        }
    }
}
