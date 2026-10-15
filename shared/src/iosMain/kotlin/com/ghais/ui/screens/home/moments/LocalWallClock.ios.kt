package com.ghais.ui.screens.home.moments

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.currentCalendar

actual object LocalWallClock {
    private fun components(): NSDateComponents? = try {
        NSCalendar.currentCalendar.components(
            NSCalendar.Hour or NSCalendar.Weekday,
            NSDate()
        )
    } catch (_: Exception) {
        null
    }

    actual fun hourOfDay(): Int =
        components()?.hour?.toInt() ?: 12

    // NSCalendar weekday: 1 = Sunday. Map to Monday = 1..7.
    actual fun mondayBasedDow(): Int {
        val weekday = components()?.weekday?.toInt() ?: 1
        return ((weekday + 5) % 7) + 1
    }
}
