package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted playback toggles for the player.
 *
 * - [autoplayNext]: when true (default) a finished track advances/wraps per
 *   the active [com.ghais.domain.model.RepeatMode] (today's behaviour); when
 *   false the engine stops at queue end even if repeat QUEUE would wrap.
 *   Manual next/previous (force-advance) is unaffected.
 * - [wifiOnly]: when true (default) streaming is restricted to Wi-Fi;
 *   enforcement lives with the network/download layer, this is storage only.
 *
 * Same recipe as [TranslationPrefs]: multiplatform-settings keys with
 * StateFlow + synchronous persist on set.
 */
object PlaybackSettings {

    private const val KEY_AUTOPLAY_NEXT = "playback_autoplay_next"
    private const val KEY_WIFI_ONLY = "playback_wifi_only"

    private val settings: Settings by lazy { Settings() }

    private val _autoplayNext = MutableStateFlow(loadAutoplayNext())
    val autoplayNext: StateFlow<Boolean> = _autoplayNext.asStateFlow()

    fun setAutoplayNext(v: Boolean) {
        if (v == _autoplayNext.value) return
        _autoplayNext.value = v
        try {
            settings.putBoolean(KEY_AUTOPLAY_NEXT, v)
        } catch (_: Exception) {
        }
    }

    private val _wifiOnly = MutableStateFlow(loadWifiOnly())
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    fun setWifiOnly(v: Boolean) {
        if (v == _wifiOnly.value) return
        _wifiOnly.value = v
        try {
            settings.putBoolean(KEY_WIFI_ONLY, v)
        } catch (_: Exception) {
        }
    }

    private fun loadAutoplayNext(): Boolean = try {
        settings.getBoolean(KEY_AUTOPLAY_NEXT, true)
    } catch (_: Exception) {
        true
    }

    private fun loadWifiOnly(): Boolean = try {
        settings.getBoolean(KEY_WIFI_ONLY, true)
    } catch (_: Exception) {
        true
    }
}
