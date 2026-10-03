package com.ghais.android

import android.content.Context
import android.util.Log
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.ScheduleEngine
import com.ghais.data.repository.ScheduleTracks
import com.ghais.data.repository.SchedulesStore
import com.ghais.player.AudioEngine
import com.ghais.player.PlayerBridge
import com.ghais.player.SleepTimer
import com.ghais.player.StopCondition

/**
 * Playback-start path for a scheduled recitation.
 *
 * Extracted from [ScheduleAlarmReceiver] so the foreground service can invoke
 * it directly (ACTION_PLAY_SCHEDULE -> [QuranPlaybackService.onStartCommand]
 * -> [run]) while the receiver keeps only the lookup + dispatch.
 *
 * Order (identical to the original inlined code): bind owner -> lookup ->
 * enabled guard -> [PlayerBridge.init] -> build tracks -> play -> sleep
 * timer, then re-arm tomorrow's alarm. Differences:
 * - `autoStart = false` schedules show a reminder instead of playing.
 * - every failure notifies via [ScheduleStartNotifier.showFailure].
 * - the FGS wiring gap is fixed: [PlayerBridge.onPlayRequested] is set here
 *   when no [MainActivity] has published one yet (the alarm/service process
 *   often has no activity), so [AudioEngine.playQueue] can boot
 *   [QuranPlaybackService] instead of silently never going foreground.
 * - [ScheduleEngine.refresh] runs on EVERY path (finally) so a failure never
 *   cancels tomorrow's alarm.
 *
 * Never throws.
 */
object SchedulePlayback {

    private const val TAG = "Schedules"

    /**
     * Starts (or declines to start) the recitation for [scheduleId].
     *
     * @param context any context; the application context is used for the
     *   service start lambda so it stays valid after the caller is gone.
     * @param scheduleId schedule id carried by the alarm intent.
     */
    fun run(context: Context, scheduleId: String) {
        val appContext = try {
            context.applicationContext
        } catch (e: Exception) {
            Log.w(TAG, "applicationContext unavailable; using caller context", e)
            context
        }
        var schedule: RecitationSchedule? = null
        try {
            try {
                ScheduleEngine.init(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "ScheduleEngine.init failed", e)
            }
            try {
                AuthRepository.init(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "AuthRepository.init failed", e)
            }
            // Fresh service/receiver process reads the "local" namespace and
            // misses user schedules saved under their user id.
            bindScheduleOwner()
            Log.i(TAG, "owner bound for $scheduleId")

            val found = SchedulesStore.get(scheduleId)
            if (found == null) {
                fail(appContext, null, "Schedule $scheduleId not found; not starting.")
                return
            }
            schedule = found
            Log.i(
                TAG,
                "schedule found $scheduleId " +
                    "(${found.fromSurah}..${found.toSurah}, ${found.reciterSlug})",
            )

            if (!found.enabled) {
                fail(appContext, found, "Schedule $scheduleId disabled; not starting.")
                return
            }
            if (!found.autoStart) {
                Log.i(TAG, "Schedule $scheduleId autoStart=false; showing reminder.")
                try {
                    ScheduleStartNotifier.showReminder(appContext, found)
                } catch (e: Exception) {
                    Log.w(TAG, "showReminder failed", e)
                }
                return
            }

            PlayerBridge.init(appContext)
            Log.i(TAG, "PlayerBridge initialized for $scheduleId")

            val reciter = QuranDataRepository.getReciterBySlug(found.reciterSlug)
            val tracks = ScheduleTracks.buildScheduleTracks(
                found,
                reciter,
                QuranDataRepository.getSurahs(),
            )
            Log.i(TAG, "built ${tracks.size} tracks for $scheduleId")
            if (tracks.isEmpty()) {
                fail(appContext, found, "No playable tracks for schedule $scheduleId.")
                return
            }

            // FGS wiring gap fix: MainActivity publishes this lambda, but the
            // alarm/service process may never have created the activity, so a
            // null callback would leave playback idle with no foreground
            // service. Same start() the activity uses.
            if (PlayerBridge.onPlayRequested == null) {
                PlayerBridge.onPlayRequested = {
                    QuranPlaybackService.start(appContext)
                }
                Log.i(TAG, "onPlayRequested wired to QuranPlaybackService.start")
            }

            AudioEngine.playQueue(tracks, 0)
            Log.i(TAG, "started $scheduleId with ${tracks.size} tracks")

            val minutes = found.durationMin
            if (minutes != null) {
                SleepTimer.startTimer(minutes, StopCondition.MINUTES)
                Log.i(TAG, "sleep timer set to ${minutes}m for $scheduleId")
            }
        } catch (e: Exception) {
            fail(
                appContext,
                schedule,
                "Schedule playback failed for $scheduleId: ${e.message ?: e.toString()}",
            )
        } finally {
            // Success, reminder, failure, early return — tomorrow's alarm is
            // re-programmed on every path so a bad run can't go dark.
            try {
                ScheduleEngine.init(appContext)
                ScheduleEngine.refresh(SchedulesStore.schedules.value)
                Log.i(TAG, "re-armed schedules after $scheduleId")
            } catch (e: Exception) {
                Log.w(TAG, "Re-arm failed after $scheduleId", e)
            }
        }
    }

    /**
     * Binds SchedulesStore to the last signed-in owner from disk (no
     * network). Without this, a fresh process reads the "local" namespace
     * and misses user schedules saved under their user id.
     */
    private fun bindScheduleOwner() {
        try {
            val owner = AuthRepository.cachedSession.value?.userId ?: return
            SchedulesStore.setOwner(owner)
        } catch (e: Exception) {
            Log.w(TAG, "Owner bind failed; using local namespace.", e)
        }
    }

    /** Logs the failure and notifies the user; never throws. */
    private fun fail(context: Context, schedule: RecitationSchedule?, message: String) {
        Log.w(TAG, message)
        try {
            ScheduleStartNotifier.showFailure(context, schedule, message)
        } catch (e: Exception) {
            Log.w(TAG, "showFailure failed", e)
        }
    }
}
