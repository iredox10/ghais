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
import com.quranify.domain.model.isFullSurah
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
        private const val PLACEHOLDER_NOTIFICATION_ID = 1001

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

        fun displayTitle(surahNameEn: String, ayahNo: Int = 0, isFullSurah: Boolean = true): String {
            val name = surahNameEn.trim()
            if (name.isBlank()) return "Quranify"
            // Must match AudioEngine.trackDisplayTitle exactly: updateMetadata()
            // early-returns on equal titles, and any replaceMediaItem call made
            // while buffering resets the timeline (wipes resume seeks).
            return if (ayahNo <= 0 || isFullSurah) {
                name
            } else {
                "$name - Ayah $ayahNo"
            }
        }
    }

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var ownsPlayer = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val sessionListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && !AudioEngine.isPlaying.value) {
                AudioEngine.resume()
            } else if (!isPlaying && AudioEngine.isPlaying.value) {
                val state = player?.playbackState
                if (state != Player.STATE_BUFFERING) {
                    AudioEngine.pause()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setMediaNotificationProvider(
            androidx.media3.session.DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(PLACEHOLDER_NOTIFICATION_ID)
                .build()
                .apply {
                    setSmallIcon(R.drawable.ic_notification)
                }
        )
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

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() {
                AudioEngine.next()
            }

            override fun seekToNextMediaItem() {
                AudioEngine.next()
            }

            override fun seekToPrevious() {
                AudioEngine.previous()
            }

            override fun seekToPreviousMediaItem() {
                AudioEngine.previous()
            }
        }

        val session = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(QuranSessionCallback())
            .build()
        mediaSession = session
        addSession(session)

        // Guarantee FGS promotion within the 10s rule even if the Media3
        // notification update is delayed (blank metadata, slow network).
        // Media3 replaces this placeholder with the real media notification.
        startForegroundWithPlaceholder()
        observeEngine()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep audio alive when swiped away; stop via notification or in-app.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Idle explicit start (e.g. legacy warm-start): no media -> no notification
        // within 10s -> system kills the app. Stop immediately instead.
        if (intent?.action == null && AudioEngine.currentTrack.value == null) {
            val p = player ?: PlayerBridge.playerOrNull()
            if (p == null || !p.isPlaying) {
                stopSelf(startId)
                return super.onStartCommand(intent, flags, startId)
            }
        }
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
            try { removeSession(it) } catch (_: Exception) { }
            try { it.player.removeListener(sessionListener) } catch (_: Exception) { }
            try { it.release() } catch (_: Exception) { }
        }
        mediaSession = null
        if (ownsPlayer) {
            val released = player
            try { released?.release() } catch (_: Exception) { }
            PlayerBridge.onPlayerReleased(released)
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
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build(),
            )
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val track = AudioEngine.currentTrack.value
            if (track == null || track.audioUrl.isBlank()) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L),
                )
            }
            AudioEngine.resume()
            val title = displayTitle(track.surahNameEn, track.ayahNo, track.isFullSurah)
            val artist = track.reciterName.ifBlank { "Quranify" }
            val item = MediaItem.Builder()
                .setMediaId(track.audioUrl)
                .setUri(track.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
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
                val title = displayTitle(track.surahNameEn, track.ayahNo, track.isFullSurah)
                val artist = track.reciterName.ifBlank { "Quranify" }
                PlayerBridge.updateMetadata(
                    title,
                    artist,
                )
                player?.let { p ->
                    try {
                        p.playlistMetadata = MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .build()
                    } catch (_: Exception) { }
                }
                mediaSession?.let { session ->
                    try { onUpdateNotification(session, false) } catch (_: Exception) { }
                }
            }
        }
    }

    private fun startForegroundWithPlaceholder() {
        try {
            val track = AudioEngine.currentTrack.value
            val initialTitle = track?.let { displayTitle(it.surahNameEn, it.ayahNo, it.isFullSurah) } ?: "Quranify"
            val initialSubtitle = track?.reciterName?.takeIf { it.isNotBlank() } ?: "Preparing recitation…"
            val placeholder = android.app.Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(initialTitle)
                .setContentText(initialSubtitle)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    PLACEHOLDER_NOTIFICATION_ID,
                    placeholder,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(PLACEHOLDER_NOTIFICATION_ID, placeholder)
            }
        } catch (_: Exception) { }
    }

    private fun createNotificationChannel() {        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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
