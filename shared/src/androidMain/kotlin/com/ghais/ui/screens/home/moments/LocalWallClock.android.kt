package com.ghais.ui.screens.home.moments

import java.util.Calendar

actual object LocalWallClock {
    actual fun hourOfDay(): Int = try {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    } catch (_: Exception) {
        12
    }

    actual fun mondayBasedDow(): Int = try {
        ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
    } catch (_: Exception) {
        1
    }
}
