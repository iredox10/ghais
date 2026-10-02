package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny UI prefs for the hifz player chrome.
 *
 * [showLoopSettings] controls whether the loop-settings card (Rep / Pause /
 * Loops steppers, A–B picker, reciter-lock chip) is expanded in ayah mode.
 * Session-only would force the user to re-open it on every launch, so the
 * flag persists in multiplatform [Settings] under [KEY_SHOW_LOOP_SETTINGS].
 * Default collapsed: the player stays a clean reader until the user asks
 * for the memorization tools.
 */
object HifzUiPrefs {

    private const val KEY_SHOW_LOOP_SETTINGS = "hifz_show_loop_settings"

    private val settings: Settings by lazy { Settings() }

    private val _showLoopSettings = MutableStateFlow(load())
    val showLoopSettings: StateFlow<Boolean> = _showLoopSettings.asStateFlow()

    fun setShowLoopSettings(visible: Boolean) {
        _showLoopSettings.value = visible
        try {
            settings.putBoolean(KEY_SHOW_LOOP_SETTINGS, visible)
        } catch (_: Exception) {
        }
    }

    private fun load(): Boolean = try {
        settings.getBoolean(KEY_SHOW_LOOP_SETTINGS, false)
    } catch (_: Exception) {
        false
    }
}
