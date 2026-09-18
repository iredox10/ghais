package com.quranify.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.quranify.player.AudioEngine
import com.quranify.player.PlayerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground playback service for background Quran audio (task_0002).
 *
 * Single-ExoPlayer rule: reuse PlayerBridge.playerOrNull() when present,
 * else create + PlayerBridge.attachPlayer(). AudioEngine stays the single
 * source of truth for queue/track state; the service mirrors metadata into
 * the MediaSession for lockscreen / notification / BT controls.
 *
 * Media3 posts the MediaStyle notification automatically (channel
 * `quran_playback`, play/pause/next/prev/seek). Explicit ACTION_* intents
 * are handled in onStartCommand for compat direct starts.
 */
@OptIn(UnstableApi::class)
class QuranPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "quran_playback"

        const val ACTION_TOGGLE = "com.quranify.android.action.TOGGLE"
        const val ACTION_PLAY = "com.quranify.android.action.PLAY"
        const val ACTION_PAUSE = "com.quranify.android.action.PAUSE"
        const val ACTION_NEXT = "com.quranify.android.action.NEXT"
        const val ACTION_PREV = "com.quranify.android.action.PREV"
        const val ACTION_SEEK_FORWARD = "com.quranify.android.action.SEEK_FORWARD"
        const val ACTION_SEEK_BACK = "com.quranify.android.action.SEEK_BACK"

        private const val SEEK_STEP_MS = 10_000L

        fun start(context: android.content.Context) {
            val intent = Intent(context, QuranPlaybackService::class.java)
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                try { context.startService(intent) } catch (_: Exception) { }
            }
        }

        fun displayTitle(surahNameEn: String, ayahNo: Int): String =
            if (surahNameEn.isBlank()) "Quranify" else "$surahNameEn - Ayah $ayahNo"
    }

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var ownsPlayer = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val sessionListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && AudioEngine.currentTrack.value == null) {
                AudioEngine.resume()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val existing = PlayerBridge.playerOrNull()
        val exo: ExoPlayer = if (existing != null) {
            ownsPlayer = false
            existing
        } else {
            ownsPlayer = true
            ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()
                .also { PlayerBridge.attachPlayer(it) }
        }
        player = exo
        if (!ownsPlayer) {
            PlayerBridge.attachPlayer(exo)
        }
        exo.removeListener(sessionListener)
        exo.addListener(sessionListener)
        mediaSession = MediaSession.Builder(this, exo)
            .setCallback(QuranSessionCallback())
            .build()
        observeEngine()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep audio alive when swiped away; stop via notification or in-app.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> AudioEngine.togglePlayPause()
            ACTION_PLAY -> AudioEngine.resume()
            ACTION_PAUSE -> AudioEngine.pause()
            ACTION_NEXT -> AudioEngine.next()
            ACTION_PREV -> AudioEngine.previous()
            ACTION_SEEK_FORWARD ->
                AudioEngine.seekTo(AudioEngine.currentPositionMs.value + SEEK_STEP_MS)
            ACTION_SEEK_BACK ->
                AudioEngine.seekTo((AudioEngine.currentPositionMs.value - SEEK_STEP_MS).coerceAtLeast(0L))
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.let {
            try { it.player.removeListener(sessionListener) } catch (_: Exception) { }
            try { it.release() } catch (_: Exception) { }
        }
        mediaSession = null
        if (ownsPlayer) {
            try { player?.release() } catch (_: Exception) { }
        } else {
            try { player?.removeListener(sessionListener) } catch (_: Exception) { }
        }
        player = null
        super.onDestroy()
    }

    private inner class QuranSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build(),
            )
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            AudioEngine.resume()
            val track = AudioEngine.currentTrack.value
            val item = MediaItem.Builder()
                .setMediaId(track?.audioUrl.orEmpty())
                .setUri(track?.audioUrl.orEmpty())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(if (track == null) "Quranify" else displayTitle(track.surahNameEn, track.ayahNo))
                        .setArtist(track?.reciterName)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(listOf(item), 0, 0L),
            )
        }
    }

    private fun observeEngine() {
        serviceScope.launch {
            AudioEngine.currentTrack.collect { track ->
                if (track == null) return@collect
                PlayerBridge.updateMetadata(
                    displayTitle(track.surahNameEn, track.ayahNo),
                    track.reciterName,
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Quran playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Background Quran recitation controls"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }
}
