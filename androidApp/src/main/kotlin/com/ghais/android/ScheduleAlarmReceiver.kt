package com.ghais.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.ScheduleEngine
import com.ghais.data.repository.SchedulesStore
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.player.PlayerBridge
import com.ghais.player.SleepTimer
import com.ghais.player.StopCondition
import kotlin.math.max
import kotlin.math.min

/**
 * Fires scheduled recitations: looks up the schedule, starts the foreground
 * playback service, queues fromSurah..toSurah for the schedule's reciter,
 * applies the optional minutes sleep timer, then re-schedules (next day).
 *
 * Also re-programs all alarms on BOOT_COMPLETED.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Schedules"
        private const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val appContext = context.applicationContext
            ScheduleEngine.init(appContext)
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                ScheduleEngine.refresh(SchedulesStore.schedules.value)
                return
            }

            val id = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
            val schedule = SchedulesStore.get(id) ?: return
            if (!schedule.enabled) return

            PlayerBridge.init(appContext)
            QuranPlaybackService.start(appContext)

            val reciter = QuranDataRepository.getReciterBySlug(schedule.reciterSlug)
            val from = min(schedule.fromSurah, schedule.toSurah)
            val to = max(schedule.fromSurah, schedule.toSurah)
            val tracks = QuranDataRepository.getSurahs()
                .filter { it.id in from..to }
                .map { s ->
                    TrackItem(
                        reciterSlug = reciter.slug,
                        reciterName = reciter.nameEn,
                        surahId = s.id,
                        surahNameEn = s.nameEn,
                        surahNameAr = s.nameAr,
                        ayahNo = 0,
                        audioUrl = reciter.getFullSurahUrl(s.id),
                        durationMs = s.ayahsCount * 15_000L,
                    )
                }
            if (tracks.isEmpty()) return

            AudioEngine.playQueue(tracks, 0)

            val minutes = schedule.durationMin
            if (minutes != null) {
                SleepTimer.startTimer(minutes, StopCondition.MINUTES)
            }

            // Re-schedule for tomorrow.
            ScheduleEngine.refresh(SchedulesStore.schedules.value)
        } catch (e: Exception) {
            Log.e(TAG, "ScheduleAlarmReceiver failed", e)
        }
    }
}
