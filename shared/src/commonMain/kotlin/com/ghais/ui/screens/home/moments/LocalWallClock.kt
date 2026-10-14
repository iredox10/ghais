package com.ghais.ui.screens.home.moments

/**
 * Device-local wall-clock for moment/recap placement (coordinator wiring).
 * Hour 0..23; day-of-week 1..7 with Monday = 1 (java.time numbering).
 */
expect object LocalWallClock {
    fun hourOfDay(): Int
    fun mondayBasedDow(): Int
}
