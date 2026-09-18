package com.quranify.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AmbientType {
    Rain, OceanWaves, Birdsong, GentleWind, ForestStream, NightCrickets
}

data class AmbientChannel(
    val type: AmbientType,
    val isEnabled: Boolean = false,
    val volume: Float = 0.5f
)

class AmbientMixer {
    private val _channels = MutableStateFlow(
        AmbientType.values().map { AmbientChannel(it) }
    )
    val channels: StateFlow<List<AmbientChannel>> = _channels.asStateFlow()

    private val _masterAmbientVolume = MutableStateFlow(1.0f)
    val masterAmbientVolume: StateFlow<Float> = _masterAmbientVolume.asStateFlow()

    private val _quranVolume = MutableStateFlow(1.0f)
    val quranVolume: StateFlow<Float> = _quranVolume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun toggleChannel(type: AmbientType) {
        _channels.update { list ->
            list.map {
                if (it.type == type) it.copy(isEnabled = !it.isEnabled) else it
            }
        }
    }

    fun setVolume(type: AmbientType, volume: Float) {
        _channels.update { list ->
            list.map {
                if (it.type == type) it.copy(volume = volume.coerceIn(0f, 1f)) else it
            }
        }
    }

    fun applyPreset(name: String) {
        _channels.update { list ->
            when (name) {
                "Study Focus" -> list.map {
                    when (it.type) {
                        AmbientType.Rain -> it.copy(isEnabled = true, volume = 0.6f)
                        AmbientType.GentleWind -> it.copy(isEnabled = true, volume = 0.2f)
                        else -> it.copy(isEnabled = false)
                    }
                }
                "Deep Sleep" -> list.map {
                    when (it.type) {
                        AmbientType.OceanWaves -> it.copy(isEnabled = true, volume = 0.7f)
                        AmbientType.NightCrickets -> it.copy(isEnabled = true, volume = 0.3f)
                        else -> it.copy(isEnabled = false)
                    }
                }
                "Nature Calm" -> list.map {
                    when (it.type) {
                        AmbientType.Birdsong -> it.copy(isEnabled = true, volume = 0.5f)
                        AmbientType.ForestStream -> it.copy(isEnabled = true, volume = 0.4f)
                        else -> it.copy(isEnabled = false)
                    }
                }
                else -> list
            }
        }
    }

    fun muteAll() {
        _isMuted.update { !it }
    }

    fun setMasterAmbientVolume(volume: Float) {
        _masterAmbientVolume.value = volume.coerceIn(0f, 1f)
    }

    fun setQuranVolume(volume: Float) {
        _quranVolume.value = volume.coerceIn(0f, 1f)
    }
}
