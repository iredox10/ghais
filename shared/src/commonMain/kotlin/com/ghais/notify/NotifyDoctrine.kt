package com.ghais.notify

/**
 * Agent A8 — notification doctrine: channels, copy, frequency caps, and a
 * thin scheduler contract.
 *
 * ## Scheduler reuse decision (checked before writing — see A8 report)
 *
 * An OS alarm programmer AND a notification poster already exist:
 * - `ScheduleEngine` (expect/actual in
 *   `data/repository/SchedulesStore.kt` + `player/ScheduleEngine.*.kt`):
 *   programs one exact daily `AlarmManager` alarm per enabled
 *   `RecitationSchedule` on Android, no-op on iOS.
 * - `ScheduleStartNotifier` (`androidApp/.../ScheduleStartNotifier.kt`):
 *   posts into the `schedule_alerts` channel (IMPORTANCE_HIGH) with
 *   dedicated id bands, never throws.
 * - `SchedulesStore` already owns a reminder-only daily path:
 *   `AppSettingsScreen.setDailyReminder` creates a
 *   `RecitationSchedule(autoStart = false)` that fires a heads-up
 *   reminder instead of playback.
 *
 * Duplicating any of that (a second `AlarmManager` wrapper, a second
 * channel) would split exact-alarm permission handling and id bands, so
 * this file does NOT program alarms itself. Instead:
 * - [NotifyScheduler] is a thin expect/actual contract;
 * - the Android actual maps each doctrine channel to a reserved
 *   reminder-only `RecitationSchedule` id (`notify-<channel>`) in
 *   `SchedulesStore`, which programs the OS alarm through the existing
 *   tested path; display stays with `ScheduleStartNotifier`;
 * - the iOS actual is a no-op, mirroring `ScheduleEngine.ios`.
 *
 * ## Follow-up NOT in scope (new-files-only constraint)
 *
 * `ScheduleStartNotifier.showReminder` currently uses fixed copy
 * ("Recitation scheduled …"). Per-channel doctrine copy from [copyFor]
 * needs that file edited to look the schedule id up in this doctrine —
 * flagged in the A8 report for the owner of that file.
 *
 * ## Tone doctrine (all channels)
 *
 * Gentle, celebratory, ZERO guilt. No "you missed", no counts of absent
 * days, no streak-threat language. A quiet user gets invitation, never
 * reproach.
 */

/** Stable doctrine channel ids (also used as schedule-id suffixes). */
object NotifyChannel {
    const val DEVOTION_REMINDER: String = "devotion_reminder"
    const val STREAK_SAFE: String = "streak_safe"
    const val PRAYER_MOMENT: String = "prayer_moment"
    const val RAMADAN: String = "ramadan"
    const val KHATMA_PACE: String = "khatma_pace"

    val all: List<String> = listOf(
        DEVOTION_REMINDER,
        STREAK_SAFE,
        PRAYER_MOMENT,
        RAMADAN,
        KHATMA_PACE,
    )
}

/** Title + body template for one channel. */
data class NotifyCopy(
    val title: String,
    val body: String,
)

/**
 * Copy templates per channel. Pure: safe to call at fire time from any
 * thread, and the single place to review tone.
 */
fun copyFor(channel: String): NotifyCopy = when (channel) {
    NotifyChannel.DEVOTION_REMINDER -> NotifyCopy(
        title = "A moment for the heart",
        body = "Your daily verses are waiting — open whenever you're ready.",
    )
    NotifyChannel.STREAK_SAFE -> NotifyCopy(
        title = "Your rhythm is safe",
        body = "A beautiful run of returns. A verse or two keeps it gentle.",
    )
    NotifyChannel.PRAYER_MOMENT -> NotifyCopy(
        title = "After prayer, a verse",
        body = "Carry the prayer's calm with you — a short recitation is ready.",
    )
    NotifyChannel.RAMADAN -> NotifyCopy(
        title = "A blessed night for a few verses",
        body = "No rush, no target — just a quiet moment with the Quran.",
    )
    NotifyChannel.KHATMA_PACE -> NotifyCopy(
        title = "Steady on your path",
        body = "You're on pace with your khatma. Today's portion is ready when you are.",
    )
    else -> NotifyCopy(
        title = "A quiet invitation",
        body = "A few verses are waiting — whenever you're ready.",
    )
}

/**
 * Frequency cap for one channel: at most [maxPerDay] fires per epoch day,
 * ideally anchored at the user's [habitualHour] (the hour they usually
 * listen — supplied by the caller from usage stats; null = no anchor).
 */
data class FrequencyCap(
    val channel: String,
    val maxPerDay: Int = 1,
    val habitualHour: Int? = null,
)

/** Default doctrine: 1/day per channel (habitual hour), nothing more. */
fun defaultCaps(): List<FrequencyCap> = NotifyChannel.all.map { FrequencyCap(channel = it) }

/**
 * Pure cap check: true when [channel] may fire today given the epoch day
 * it last fired. `lastFiredEpochDay = null` (never fired) always passes.
 */
fun mayFireToday(
    cap: FrequencyCap,
    lastFiredEpochDay: Long?,
    todayEpochDay: Long,
): Boolean {
    if (cap.maxPerDay <= 0) return false
    if (lastFiredEpochDay == null) return true
    return lastFiredEpochDay != todayEpochDay
}

/** Reserved `RecitationSchedule` id for a doctrine channel. */
fun channelScheduleId(channel: String): String = "notify-$channel"

/**
 * Thin scheduler contract. Platform wiring lives in the expect/actual
 * [NotifyScheduler]; app code depends only on this interface.
 */
interface Notifier {
    /** Schedule (or reschedule) a daily fire at [hour]:[minute] for [channel]. */
    fun scheduleDaily(hour: Int, minute: Int, channel: String)

    /** Cancel any scheduled fire for [channel]. Never throws. */
    fun cancel(channel: String)
}

/**
 * Platform scheduler (expect/actual-light).
 *
 * - Android actual (`notify/NotifyScheduler.android.kt`): upserts a
 *   reminder-only (`autoStart = false`) `RecitationSchedule` under
 *   [channelScheduleId] in `SchedulesStore` — `SchedulesStore` persists
 *   and reprograms the `AlarmManager` alarms via `ScheduleEngine.refresh`
 *   on every mutation. Cancel removes that schedule. No new alarm,
 *   channel, or permission code.
 * - iOS actual (`notify/NotifyScheduler.ios.kt`): no-op, mirroring
 *   `ScheduleEngine.ios` (iOS needs BGTask + notification infra, out of
 *   scope — same documented boundary).
 */
expect object NotifyScheduler : Notifier
