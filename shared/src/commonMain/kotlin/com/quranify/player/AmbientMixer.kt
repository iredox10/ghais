package com.quranify.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AmbientType {
    Rain, OceanWaves, Birdsong, GentleWind, ForestStream, NightCrickets,
    Fire, Cat, Owl, River, Whale, Thunder, Storm, Train
}

data class AmbientChannel(
    val type: AmbientType,
    val isEnabled: Boolean = false,
    val volume: Float = 0.5f
)

/** Bundled loop file per type (`composeResources/files/ambient/<key>.mp3`). */
fun AmbientType.assetKey(): String = when (this) {
    AmbientType.Rain -> "rain"
    AmbientType.Birdsong -> "birds"
    AmbientType.Fire -> "fire"
    AmbientType.OceanWaves -> "waves"
    AmbientType.GentleWind -> "wind"
    AmbientType.Cat -> "cat"
    AmbientType.Owl -> "owl"
    AmbientType.River -> "river"
    AmbientType.ForestStream -> "river"
    AmbientType.Whale -> "whale"
    AmbientType.NightCrickets -> "crickets"
    AmbientType.Thunder -> "thunder"
    AmbientType.Storm -> "storm"
    AmbientType.Train -> "train"
}

/** Bundled looping background videos per sound (`androidApp/.../assets/videos/<key>.mp4`). Empty = audio-only glow fallback. */
fun AmbientType.videoKeys(): List<String> = when (this) {
    AmbientType.Rain -> listOf("Rain1", "Rain2")
    AmbientType.Birdsong -> listOf("birds1")
    AmbientType.Fire -> listOf("fire1", "fire2")
    AmbientType.OceanWaves -> listOf("wave1")
    AmbientType.GentleWind -> listOf("wind1", "wind2", "wind3")
    AmbientType.Cat -> listOf("cat1")
    AmbientType.Owl -> emptyList()
    AmbientType.River -> listOf("river1", "river2")
    AmbientType.ForestStream -> listOf("river1", "river2")
    AmbientType.Whale -> emptyList()
    AmbientType.NightCrickets -> listOf("Night_Ambient")
    AmbientType.Thunder -> listOf("thunder1")
    AmbientType.Storm -> emptyList()
    AmbientType.Train -> emptyList()
}

/** User-facing display name per type. */
fun AmbientType.displayName(): String = when (this) {
    AmbientType.Rain -> "Rain"
    AmbientType.Birdsong -> "Birds"
    AmbientType.Fire -> "Fire"
    AmbientType.OceanWaves -> "Waves"
    AmbientType.GentleWind -> "Wind"
    AmbientType.Cat -> "Cat"
    AmbientType.Owl -> "Owl"
    AmbientType.River -> "River"
    AmbientType.ForestStream -> "Stream"
    AmbientType.Whale -> "Whale"
    AmbientType.NightCrickets -> "Crickets"
    AmbientType.Thunder -> "Thunder"
    AmbientType.Storm -> "Storm"
    AmbientType.Train -> "Train"
}

val List<AmbientChannel>.selectedAmbientChannel: AmbientChannel?
    get() = firstOrNull { it.isEnabled }

val List<AmbientChannel>.selectedAmbientType: AmbientType?
    get() = selectedAmbientChannel?.type

/**
 * Global ambient-sound state. Singleton (not per-screen) so the loop keeps
 * going across navigation and stays in sync with [AudioEngine]:
 *
 * - Quran playing   -> selected loop plays (looped, mixed underneath)
 * - Quran paused/stopped -> loop pauses in place (resumes, never restarts)
 * - "No sounds"     -> loop stopped and unloaded
 * - Track changes   -> loop continues untouched
 */
object AmbientMixer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _channels = MutableStateFlow(
        AmbientType.values().map { AmbientChannel(it) }
    )
    val channels: StateFlow<List<AmbientChannel>> = _channels.asStateFlow()

    /** Currently active ambient channel, or null if none is enabled. */
    val selectedAmbientChannel: AmbientChannel?
        get() = _channels.value.firstOrNull { it.isEnabled }

    /** Currently active ambient type, or null if no ambient sound is enabled. */
    val selectedAmbientType: AmbientType?
        get() = selectedAmbientChannel?.type

    private val _masterAmbientVolume = MutableStateFlow(1.0f)
    val masterAmbientVolume: StateFlow<Float> = _masterAmbientVolume.asStateFlow()

    private val _quranVolume = MutableStateFlow(1.0f)
    val quranVolume: StateFlow<Float> = _quranVolume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    init {
        scope.launch {
            AudioEngine.isPlaying.collect { syncAmbient() }
        }
        scope.launch {
            combine(_channels, _masterAmbientVolume, _isMuted) { channels, master, muted ->
                Triple(channels, master, muted)
            }.collect { (channels, master, muted) ->
                val selected = channels.firstOrNull { it.isEnabled }
                val effective = if (muted || selected == null) 0f
                else (master * selected.volume * 0.6f).coerceIn(0f, 1f)
                AmbientPlayerBridge.setAmbientVolume(effective)
                // A newly enabled channel while playing starts immediately.
                if (selected != null && AudioEngine.isPlaying.value && !muted) {
                    AmbientPlayerBridge.playAmbient(selected.type.assetKey())
                }
            }
        }
    }

    fun toggleChannel(type: AmbientType) {
        _channels.update { list ->
            list.map {
                if (it.type == type) it.copy(isEnabled = !it.isEnabled) else it
            }
        }
        syncAmbient()
    }

    /** Single-select behavior for the Background-sound picker: only [type] on, rest off. Null = no sounds. */
    fun selectExclusive(type: AmbientType?) {
        _channels.update { list ->
            list.map { it.copy(isEnabled = type != null && it.type == type) }
        }
        syncAmbient()
    }

    fun clearAll() = selectExclusive(null)

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
                        AmbientType.River -> it.copy(isEnabled = true, volume = 0.4f)
                        else -> it.copy(isEnabled = false)
                    }
                }
                else -> list
            }
        }
        syncAmbient()
    }

    fun muteAll() {
        _isMuted.update { !it }
    }

    fun setMasterAmbientVolume(volume: Float) {
        _masterAmbientVolume.value = volume.coerceIn(0f, 1f)
    }

    fun setQuranVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _quranVolume.value = clamped
        AudioEngine.setVolume(clamped)
    }

    /**
     * Mirror Quran transport state onto the loop:
     * playing -> loop audible (load or resume), otherwise pause in place.
     */
    private fun syncAmbient() {
        val selected = selectedAmbientChannel
        if (selected == null || _isMuted.value) {
            if (selected == null) AmbientPlayerBridge.stopAmbient()
            else AmbientPlayerBridge.pauseAmbient()
            return
        }
        if (AudioEngine.isPlaying.value) {
            AmbientPlayerBridge.playAmbient(selected.type.assetKey())
        } else {
            AmbientPlayerBridge.pauseAmbient()
        }
    }
}
