package com.ghais.notify

/**
 * Agent A8 — iOS actual for [NotifyScheduler].
 *
 * No-op, mirroring `ScheduleEngine.ios`: iOS background scheduling needs
 * BGTask + UNUserNotificationCenter infra, which is the same documented
 * out-of-scope boundary. Calls never throw so shared callers need no
 * platform branches.
 */
actual object NotifyScheduler : Notifier {
    actual override fun scheduleDaily(hour: Int, minute: Int, channel: String) {
        // No-op on iOS (see above).
    }

    actual override fun cancel(channel: String) {
        // No-op on iOS (see above).
    }
}
