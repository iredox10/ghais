package com.ghais.data.repository

/**
 * iOS actual: no-op. iOS background audio scheduling needs BGTask +
 * notification infra — out of scope.
 */
actual object ScheduleEngine {
    actual fun refresh(schedules: List<RecitationSchedule>) {
        // No-op on iOS (see above).
    }
}
