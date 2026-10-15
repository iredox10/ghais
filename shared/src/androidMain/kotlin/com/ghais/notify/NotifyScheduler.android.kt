package com.ghais.notify

import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore

/**
 * Agent A8 — Android actual for [NotifyScheduler].
 *
 * REUSE, not duplication: each doctrine channel becomes a reminder-only
 * (`autoStart = false`) [RecitationSchedule] with the reserved id
 * `notify-<channel>` in [SchedulesStore]. The store persists and calls
 * `ScheduleEngine.refresh`, which programs the exact `AlarmManager` daily
 * alarm (or the inexact fallback when exact alarms are denied) — the same
 * tested path `AppSettingsScreen.setDailyReminder` uses. Firing display
 * stays with `ScheduleStartNotifier` (`schedule_alerts` channel).
 *
 * Known limit (no title/body fields exist on [RecitationSchedule]): only
 * the fire TIME is persisted here. The words shown come from [copyFor] and
 * need `ScheduleStartNotifier.showReminder` to resolve them per schedule
 * id — flagged as a follow-up for that file's owner (A8 is new-files-only).
 */
actual object NotifyScheduler : Notifier {
    override fun scheduleDaily(hour: Int, minute: Int, channel: String) {
        try {
            val id = channelScheduleId(channel)
            val existing = try {
                SchedulesStore.get(id)
            } catch (_: Exception) {
                null
            }
            SchedulesStore.add(
                RecitationSchedule(
                    id = id,
                    hour = hour.coerceIn(0, 23),
                    minute = minute.coerceIn(0, 59),
                    reciterSlug = existing?.reciterSlug ?: "mishary",
                    fromSurah = existing?.fromSurah ?: 1,
                    toSurah = existing?.toSurah ?: 1,
                    durationMin = existing?.durationMin,
                    autoStart = false,
                    enabled = true,
                ),
            )
        } catch (_: Exception) {
        }
    }

    override fun cancel(channel: String) {
        try {
            SchedulesStore.remove(channelScheduleId(channel))
        } catch (_: Exception) {
        }
    }
}
