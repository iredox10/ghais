package com.ghais.ui.screens.reciters.deck

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deck-done-today persistence (day-scoped, device-global).
 *
 * A single `Long` epoch-day key: marking done stores today, so the flag
 * auto-resets at midnight with no cleanup job. Device-global on purpose
 * (no owner namespace): guests have no owner yet, and finishing the deck
 * pre-auth counts post-auth. Never throws — Settings failures are swallowed.
 */
object DeckDoneStore {
    private const val KEY_DONE_DAY = "ghais_deck_done_day"

    private val settings: Settings by lazy { Settings() }

    private val _doneDay = MutableStateFlow(loadDay())
    val doneDay: StateFlow<Long?> = _doneDay.asStateFlow()

    fun isDoneToday(today: Long = todayEpochDay()): Boolean =
        isDone(_doneDay.value, today)

    fun markDone(today: Long = todayEpochDay()) {
        if (_doneDay.value == today) return
        _doneDay.value = today
        try {
            settings.putLong(KEY_DONE_DAY, today)
        } catch (_: Exception) {
        }
    }

    fun clear() {
        _doneDay.value = null
        try {
            settings.remove(KEY_DONE_DAY)
        } catch (_: Exception) {
        }
    }

    private fun loadDay(): Long? = try {
        if (settings.hasKey(KEY_DONE_DAY)) settings.getLong(KEY_DONE_DAY, -1L).takeIf { it >= 0 }
        else null
    } catch (_: Exception) {
        null
    }
}

/** Pure day comparison for tests: done iff the stored day is today. */
fun isDone(stored: Long?, today: Long): Boolean = stored == today
