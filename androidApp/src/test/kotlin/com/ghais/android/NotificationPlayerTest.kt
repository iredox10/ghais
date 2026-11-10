package com.ghais.android

import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import com.ghais.domain.model.RepeatMode
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class NotificationPlayerTest {
    private class Controls : NotificationPlaybackControls {
        override var positionMs = 30_000L
        var storedRepeatMode = RepeatMode.OFF
        override val repeatMode get() = storedRepeatMode
        override var shuffle = false
        var action = ""
        var storedSpeed = 1f
        override fun resume() { action = "play" }
        override fun pause() { action = "pause" }
        override fun stop() { action = "stop" }
        override fun seekTo(positionMs: Long) { this.positionMs = positionMs }
        override fun setSpeed(speed: Float) { this.storedSpeed = speed }
        override fun setRepeatMode(mode: RepeatMode) { storedRepeatMode = mode }
        override fun toggleShuffle() { shuffle = !shuffle }
        override fun next() { action = "next" }
        override fun previous() { action = "previous" }
    }
    private fun player(controls: Controls): NotificationPlayer {
        val delegate = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getCurrentMediaItemIndex" -> 0
                "getAvailableCommands" -> Player.Commands.EMPTY
                "isCommandAvailable" -> false
                else -> error("System controls bypassed shared state: ${method.name}")
            }
        } as Player
        return NotificationPlayer(delegate, controls)
    }

    @Test fun transportAndPositionControlsUseSharedState() {
        val c = Controls(); val p = player(c)
        p.play(); assertEquals("play", c.action)
        p.setPlayWhenReady(false); assertEquals("pause", c.action)
        p.seekTo(5_000); assertEquals(5_000L, c.positionMs)
        p.seekTo(0, 12_000); assertEquals(12_000L, c.positionMs)
        p.seekTo(1, 99_000); assertEquals(12_000L, c.positionMs)
        p.seekBack(); assertEquals(2_000L, c.positionMs)
        p.seekForward(); assertEquals(12_000L, c.positionMs)
        p.seekToDefaultPosition(); assertEquals(0L, c.positionMs)
        p.seekToNextMediaItem(); assertEquals("next", c.action)
        p.seekToPrevious(); assertEquals("previous", c.action)
        p.stop(); assertEquals("stop", c.action)
    }

    @Test fun systemSettingsUseSharedStateAndShuffleIsIdempotent() {
        val c = Controls(); val p = player(c)
        p.setRepeatMode(Player.REPEAT_MODE_ALL)
        assertEquals(RepeatMode.QUEUE, c.repeatMode)
        assertEquals(Player.REPEAT_MODE_ALL, p.repeatMode)
        p.setShuffleModeEnabled(true); p.setShuffleModeEnabled(true)
        assertTrue(c.shuffle); assertTrue(p.shuffleModeEnabled)
        p.setPlaybackSpeed(1.25f); assertEquals(1.25f, c.storedSpeed)
        p.setPlaybackParameters(PlaybackParameters(1.5f)); assertEquals(1.5f, c.storedSpeed)
    }
}
