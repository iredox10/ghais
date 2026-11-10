package com.ghais.android

import androidx.media3.common.FlagSet
import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import com.ghais.domain.model.RepeatMode
import com.ghais.player.AudioEngine

internal interface NotificationPlaybackControls {
    val positionMs: Long
    val repeatMode: RepeatMode
    val shuffle: Boolean
    fun resume()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setRepeatMode(mode: RepeatMode)
    fun toggleShuffle()
    fun next()
    fun previous()
}

internal object EngineNotificationControls : NotificationPlaybackControls {
    override val positionMs get() = AudioEngine.currentPositionMs.value
    override val repeatMode get() = AudioEngine.playbackState.value.settings.repeatMode
    override val shuffle get() = AudioEngine.playbackState.value.settings.shuffle
    override fun resume() = AudioEngine.resume()
    override fun pause() = AudioEngine.pause()
    override fun stop() = AudioEngine.stop()
    override fun seekTo(positionMs: Long) = AudioEngine.seekTo(positionMs)
    override fun setSpeed(speed: Float) = AudioEngine.setSpeed(speed)
    override fun setRepeatMode(mode: RepeatMode) = AudioEngine.setRepeatMode(mode)
    override fun toggleShuffle() = AudioEngine.toggleShuffle()
    override fun next() = AudioEngine.next()
    override fun previous() = AudioEngine.previous()
}

/** Keeps system controllers on the same playback path as the app. */
@OptIn(UnstableApi::class)
internal class NotificationPlayer(player: Player, private val controls: NotificationPlaybackControls) : ForwardingPlayer(player) {
    private val settingListeners = linkedSetOf<Player.Listener>()
    private var publishedRepeat = repeatMode
    private var publishedShuffle = shuffleModeEnabled

    override fun addListener(listener: Player.Listener) {
        settingListeners.add(listener)
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        settingListeners.remove(listener)
        super.removeListener(listener)
    }

    /** Engine settings are logical queue state, so ExoPlayer emits no event. */
    fun publishSettingsChanges() {
        val repeat = repeatMode
        val shuffle = shuffleModeEnabled
        val repeatChanged = repeat != publishedRepeat
        val shuffleChanged = shuffle != publishedShuffle
        if (!repeatChanged && !shuffleChanged) return
        publishedRepeat = repeat
        publishedShuffle = shuffle
        val flags = FlagSet.Builder().apply {
            if (repeatChanged) add(Player.EVENT_REPEAT_MODE_CHANGED)
            if (shuffleChanged) add(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
        }.build()
        for (listener in settingListeners.toList()) {
            if (repeatChanged) listener.onRepeatModeChanged(repeat)
            if (shuffleChanged) listener.onShuffleModeEnabledChanged(shuffle)
            listener.onEvents(this, Player.Events(flags))
        }
    }

            override fun play() = controls.resume()
            override fun pause() = controls.pause()
            override fun stop() = controls.stop()
            override fun setPlayWhenReady(playWhenReady: Boolean) {
                if (playWhenReady) controls.resume() else controls.pause()
            }

            // Controllers (lockscreen, Bluetooth and Android Auto) also use
            // these direct APIs, not just the notification's custom buttons.
            // Forwarding them to ExoPlayer bypasses shared queue/settings state.
            override fun seekTo(positionMs: Long) = controls.seekTo(positionMs)

            override fun seekToDefaultPosition() = controls.seekTo(0L)

            override fun seekToDefaultPosition(mediaItemIndex: Int) {
                if (mediaItemIndex == currentMediaItemIndex) controls.seekTo(0L)
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                // ExoPlayer contains one item; the app owns the logical queue.
                if (mediaItemIndex == currentMediaItemIndex) controls.seekTo(positionMs)
            }

            override fun setPlaybackSpeed(speed: Float) = controls.setSpeed(speed)

            override fun setPlaybackParameters(playbackParameters: PlaybackParameters) =
                controls.setSpeed(playbackParameters.speed)

            override fun getRepeatMode(): Int = when (controls.repeatMode) {
                RepeatMode.SURAH, RepeatMode.AYAH -> Player.REPEAT_MODE_ONE
                RepeatMode.QUEUE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }

            override fun setRepeatMode(repeatMode: Int) { controls.setRepeatMode(when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.SURAH
                Player.REPEAT_MODE_ALL -> RepeatMode.QUEUE
                else -> RepeatMode.OFF
            }); publishSettingsChanges() }

            override fun getShuffleModeEnabled(): Boolean = controls.shuffle

            override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
                if (shuffleModeEnabled != getShuffleModeEnabled()) controls.toggleShuffle()
                publishSettingsChanges()
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_BACK,
                    Player.COMMAND_SEEK_FORWARD -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            // Shade ±10s: route rewind/ffwd buttons through AudioEngine so the
            // shared position/speed state stays the single source of truth.
            override fun getSeekBackIncrement(): Long = 10_000L

            override fun getSeekForwardIncrement(): Long = 10_000L

            override fun seekBack() {
                try {
                    controls.seekTo((controls.positionMs - 10_000L).coerceAtLeast(0L))
                } catch (_: Exception) { }
            }

            override fun seekForward() {
                try {
                    controls.seekTo(controls.positionMs + 10_000L)
                } catch (_: Exception) { }
            }

            override fun seekToNext() {
                try { controls.next() } catch (_: Exception) { }
            }

            override fun seekToNextMediaItem() {
                try { controls.next() } catch (_: Exception) { }
            }

            override fun seekToPrevious() {
                try { controls.previous() } catch (_: Exception) { }
            }

            override fun seekToPreviousMediaItem() {
                try { controls.previous() } catch (_: Exception) { }
            }
        
}
