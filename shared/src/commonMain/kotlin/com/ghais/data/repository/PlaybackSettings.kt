package com.ghais.data.repository

import com.ghais.domain.model.HifzLoopMode
import com.ghais.domain.model.RepeatMode
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
    private const val KEY_SPEED = "playback_speed"
    private const val KEY_REPEAT_MODE = "playback_repeat_mode"
    private const val KEY_SHUFFLE = "playback_shuffle"
    private const val KEY_VOLUME = "playback_volume"
    private const val KEY_AYAH_REP_TARGET = "playback_ayah_rep_target"
    private const val KEY_RECITATION_GAP_SEC = "playback_recitation_gap_sec"
    private const val KEY_HIFZ_LOOP_MODE = "playback_hifz_loop_mode"
    private const val KEY_HIFZ_PAUSE_MULT = "playback_hifz_pause_mult"

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

    fun getSpeed(): Float = try {
        settings.getFloat(KEY_SPEED, 1.0f).coerceIn(0.25f, 3.0f)
    } catch (_: Exception) {
        1.0f
    }

    fun setSpeed(v: Float) {
        try {
            settings.putFloat(KEY_SPEED, v.coerceIn(0.25f, 3.0f))
        } catch (_: Exception) {
        }
    }

    fun getRepeatMode(): RepeatMode = try {
        val raw = settings.getStringOrNull(KEY_REPEAT_MODE)
        RepeatMode.entries.firstOrNull { it.name == raw } ?: RepeatMode.OFF
    } catch (_: Exception) {
        RepeatMode.OFF
    }

    fun setRepeatMode(mode: RepeatMode) {
        try {
            settings.putString(KEY_REPEAT_MODE, mode.name)
        } catch (_: Exception) {
        }
    }

    fun getShuffle(): Boolean = try {
        settings.getBoolean(KEY_SHUFFLE, false)
    } catch (_: Exception) {
        false
    }

    fun setShuffle(v: Boolean) {
        try {
            settings.putBoolean(KEY_SHUFFLE, v)
        } catch (_: Exception) {
        }
    }

    fun getVolume(): Float = try {
        settings.getFloat(KEY_VOLUME, 1.0f).coerceIn(0f, 1f)
    } catch (_: Exception) {
        1.0f
    }

    fun setVolume(v: Float) {
        try {
            settings.putFloat(KEY_VOLUME, v.coerceIn(0f, 1f))
        } catch (_: Exception) {
        }
    }

    fun getAyahRepetitionTarget(): Int = try {
        settings.getInt(KEY_AYAH_REP_TARGET, 1).let { if (it == -1) -1 else it.coerceAtLeast(1) }
    } catch (_: Exception) {
        1
    }

    fun setAyahRepetitionTarget(v: Int) {
        try {
            val clean = if (v == -1) -1 else v.coerceAtLeast(1)
            settings.putInt(KEY_AYAH_REP_TARGET, clean)
        } catch (_: Exception) {
        }
    }

    fun getRecitationGapSeconds(): Int = try {
        settings.getInt(KEY_RECITATION_GAP_SEC, 0).let { if (it == -1) -1 else it.coerceAtLeast(0) }
    } catch (_: Exception) {
        0
    }

    fun setRecitationGapSeconds(v: Int) {
        try {
            val clean = if (v == -1) -1 else v.coerceAtLeast(0)
            settings.putInt(KEY_RECITATION_GAP_SEC, clean)
        } catch (_: Exception) {
        }
    }

    fun getHifzLoopMode(): HifzLoopMode = try {
        val raw = settings.getStringOrNull(KEY_HIFZ_LOOP_MODE)
        HifzLoopMode.entries.firstOrNull { it.name == raw } ?: HifzLoopMode.FLAT
    } catch (_: Exception) {
        HifzLoopMode.FLAT
    }

    fun setHifzLoopMode(mode: HifzLoopMode) {
        try {
            settings.putString(KEY_HIFZ_LOOP_MODE, mode.name)
        } catch (_: Exception) {
        }
    }

    fun getHifzPauseMultiplier(): Float = try {
        settings.getFloat(KEY_HIFZ_PAUSE_MULT, 0f).coerceAtLeast(0f)
    } catch (_: Exception) {
        0f
    }

    fun setHifzPauseMultiplier(v: Float) {
        try {
            settings.putFloat(KEY_HIFZ_PAUSE_MULT, v.coerceAtLeast(0f))
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
