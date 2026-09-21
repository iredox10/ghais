package com.ghais.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.ghais.data.repository.FavoritesStore
import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.isFullSurah
import com.ghais.player.AmbientVideoArt
import com.ghais.player.AudioEngine
import com.ghais.player.PlayerBridge
import com.ghais.player.SleepTimer
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

        const val ACTION_TOGGLE = "com.ghais.android.action.TOGGLE"
        const val ACTION_PLAY = "com.ghais.android.action.PLAY"
        const val ACTION_PAUSE = "com.ghais.android.action.PAUSE"
        const val ACTION_NEXT = "com.ghais.android.action.NEXT"
        const val ACTION_PREV = "com.ghais.android.action.PREV"
        const val ACTION_SEEK_FORWARD = "com.ghais.android.action.SEEK_FORWARD"
        const val ACTION_SEEK_BACK = "com.ghais.android.action.SEEK_BACK"

        /** Custom session commands rendered as notification custom-layout buttons. */
        const val CUSTOM_FAVORITE_TOGGLE = "FAVORITE_TOGGLE"
        const val CUSTOM_REPEAT_CYCLE = "REPEAT_CYCLE"

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
            if (name.isBlank()) return "Ghais"
            // Must match AudioEngine.trackDisplayTitle exactly: updateMetadata()
            // early-returns on equal titles, and any replaceMediaItem call made
            // while buffering resets the timeline (wipes resume seeks).
            return if (ayahNo <= 0 || isFullSurah) {
                name
            } else {
                "$name - Ayah $ayahNo"
            }
        }

        /**
         * Repeat cycle used by the shade REPEAT_CYCLE button.
         * Inlined here (OFF->SURAH->QUEUE->OFF) because shared has no
         * AudioEngine.nextRepeatMode — the only sibling (UI PlayerControls)
         * intentionally skips AYAH with the same mapping.
         */
        fun nextShadeRepeatMode(current: RepeatMode): RepeatMode = when (current) {
            RepeatMode.OFF -> RepeatMode.SURAH
            RepeatMode.SURAH -> RepeatMode.QUEUE
            else -> RepeatMode.OFF
        }

        /**
         * Live shade subtitle suffix, e.g. "Sleeps in 12:00 • 1.25×".
         * Pure read of StateFlow values — safe from any thread, never throws.
         */
        fun liveSubtitleSuffix(): String {
            return try {
                val parts = mutableListOf<String>()
                try {
                    val sleep = SleepTimer.state.value
                    if (sleep.isActive && sleep.remainingSeconds > 0) {
                        val total = sleep.remainingSeconds
                        parts.add("Sleeps in ${total / 60}:${(total % 60).toString().padStart(2, '0')}")
                    }
                } catch (_: Exception) { }
                try {
                    val speed = AudioEngine.playbackSpeed.value
                    if (speed > 0f && kotlin.math.abs(speed - 1f) > 0.001f) {
                        val label = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
                        parts.add("${label}×")
                    }
                } catch (_: Exception) { }
                parts.joinToString(" • ")
            } catch (_: Exception) {
                ""
            }
        }
    }

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var ownsPlayer = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastLiveSubtitle: String? = null
    private var lastShadeButtonsKey: String? = null

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
        // Musicolet-style shade: provider subclass that appends the live
        // subtitle (sleep countdown • speed) without touching the timeline.
        // Same channel/id as before (1001 replaces the placeholder FGS).
        // Artwork for artworkUri comes from the session BitmapLoader.
        val notificationProvider = try {
            QuranNotificationProvider(this, PLACEHOLDER_NOTIFICATION_ID, CHANNEL_ID)
                .apply { setSmallIcon(R.drawable.ic_notification) }
        } catch (_: Exception) {
            androidx.media3.session.DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(PLACEHOLDER_NOTIFICATION_ID)
                .build()
                .apply {
                    setSmallIcon(R.drawable.ic_notification)
                }
        }
        setMediaNotificationProvider(notificationProvider)
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
            override fun getSeekBackIncrement(): Long = SEEK_STEP_MS

            override fun getSeekForwardIncrement(): Long = SEEK_STEP_MS

            override fun seekBack() {
                try {
                    AudioEngine.seekTo((AudioEngine.currentPositionMs.value - SEEK_STEP_MS).coerceAtLeast(0L))
                } catch (_: Exception) { }
            }

            override fun seekForward() {
                try {
                    AudioEngine.seekTo(AudioEngine.currentPositionMs.value + SEEK_STEP_MS)
                } catch (_: Exception) { }
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

        val session = try {
            MediaSession.Builder(this, forwardingPlayer)
                .setCallback(QuranSessionCallback())
                .setSessionActivity(sessionActivityIntent())
                .setBitmapLoader(ShadeBitmapLoader(applicationContext))
                .build()
        } catch (_: Exception) {
            MediaSession.Builder(this, forwardingPlayer)
                .setCallback(QuranSessionCallback())
                .build()
        }
        mediaSession = session
        addSession(session)
        // Publish favorite/repeat custom-layout buttons for the shade.
        try { refreshShadeButtons() } catch (_: Exception) { }

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
            val sessionCommands = try {
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_FAVORITE_TOGGLE, Bundle.EMPTY))
                    .add(SessionCommand(CUSTOM_REPEAT_CYCLE, Bundle.EMPTY))
                    .build()
            } catch (_: Exception) {
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
            }
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .build(),
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            return try {
                when (customCommand.customAction) {
                    CUSTOM_FAVORITE_TOGGLE -> {
                        try {
                            AudioEngine.currentTrack.value?.let { FavoritesStore.toggle(it) }
                        } catch (_: Exception) { }
                        try { refreshShadeButtons() } catch (_: Exception) { }
                        try { refreshNotification() } catch (_: Exception) { }
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    CUSTOM_REPEAT_CYCLE -> {
                        try {
                            val current = AudioEngine.playbackState.value.settings.repeatMode
                            AudioEngine.setRepeatMode(nextShadeRepeatMode(current))
                        } catch (_: Exception) { }
                        try { refreshShadeButtons() } catch (_: Exception) { }
                        try { refreshNotification() } catch (_: Exception) { }
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_INFO_SKIPPED))
                }
            } catch (_: Exception) {
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
            }
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
            val artist = track.reciterName.ifBlank { "Ghais" }
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
                val artist = track.reciterName.ifBlank { "Ghais" }
                PlayerBridge.updateMetadata(
                    title,
                    artist,
                )
                try {
                    applyArtworkToSession(try { AmbientVideoArt.artworkUri.value } catch (_: Exception) { null })
                } catch (_: Exception) { }
                player?.let { p ->
                    try {
                        p.playlistMetadata = MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(artist)
                            .build()
                    } catch (_: Exception) { }
                }
                try { refreshShadeButtons() } catch (_: Exception) { }
                mediaSession?.let { session ->
                    try { onUpdateNotification(session, false) } catch (_: Exception) { }
                }
            }
        }
        // Live subtitle: speed changes refresh the shade text.
        serviceScope.launch {
            try {
                AudioEngine.playbackSpeed.collect { refreshNotificationIfLiveChanged() }
            } catch (_: Exception) { }
        }
        // Live subtitle: sleep countdown ticks refresh the shade text.
        serviceScope.launch {
            try {
                SleepTimer.state.collect { refreshNotificationIfLiveChanged() }
            } catch (_: Exception) { }
        }
        // Heart button follows in-app favorite changes.
        serviceScope.launch {
            try {
                FavoritesStore.favoriteTracks.collect {
                    try { refreshShadeButtons() } catch (_: Exception) { }
                    try { refreshNotification() } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }
        // Repeat button follows in-app repeat changes.
        serviceScope.launch {
            try {
                AudioEngine.playbackState.collect {
                    try { refreshShadeButtons() } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }
        // Ambient video frame art: apply when frames arrive (safe path only —
        // never replaces the item mid-playback) and refresh the shade.
        serviceScope.launch {
            try {
                AmbientVideoArt.artworkUri.collect { uri ->
                    try { applyArtworkToSession(uri) } catch (_: Exception) { }
                    try { refreshNotification() } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }
    }

    /** Tap-to-open: shade tap launches MainActivity. */
    private fun sessionActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshNotification() {
        try {
            mediaSession?.let { session ->
                try { onUpdateNotification(session, false) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    /** Reposts the shade only when the live subtitle actually changed. */
    private fun refreshNotificationIfLiveChanged() {
        try {
            val suffix = liveSubtitleSuffix()
            if (suffix == lastLiveSubtitle) return
            lastLiveSubtitle = suffix
            refreshNotification()
        } catch (_: Exception) { }
    }

    /** Rebuilds the favorite/repeat shade buttons; no-op when unchanged. */
    private fun refreshShadeButtons() {
        try {
            val session = mediaSession ?: return
            val track = try { AudioEngine.currentTrack.value } catch (_: Exception) { null }
            val isFav = try {
                track != null && FavoritesStore.isFavorite(track)
            } catch (_: Exception) { false }
            val repeat = try {
                AudioEngine.playbackState.value.settings.repeatMode
            } catch (_: Exception) { RepeatMode.OFF }
            val key = "${track?.audioUrl}:$isFav:$repeat"
            if (key == lastShadeButtonsKey) return
            lastShadeButtonsKey = key
            session.setCustomLayout(buildShadeButtons(isFav, repeat))
        } catch (_: Exception) { }
    }

    private fun buildShadeButtons(isFav: Boolean, repeat: RepeatMode): List<CommandButton> {
        val buttons = mutableListOf<CommandButton>()
        try {
            val favIcon = CommandButton.getIconResIdForIconConstant(
                if (isFav) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED,
            )
            buttons.add(
                CommandButton.Builder()
                    .setSessionCommand(SessionCommand(CUSTOM_FAVORITE_TOGGLE, Bundle.EMPTY))
                    .setDisplayName(if (isFav) "Remove favorite" else "Add favorite")
                    .setIconResId(favIcon)
                    .setEnabled(true)
                    .build(),
            )
        } catch (_: Exception) { }
        try {
            val repeatIconConstant = when (repeat) {
                RepeatMode.SURAH -> CommandButton.ICON_REPEAT_ONE
                RepeatMode.QUEUE -> CommandButton.ICON_REPEAT_ALL
                else -> CommandButton.ICON_REPEAT_OFF
            }
            buttons.add(
                CommandButton.Builder()
                    .setSessionCommand(SessionCommand(CUSTOM_REPEAT_CYCLE, Bundle.EMPTY))
                    .setDisplayName("Repeat: $repeat")
                    .setIconResId(CommandButton.getIconResIdForIconConstant(repeatIconConstant))
                    .setEnabled(true)
                    .build(),
            )
        } catch (_: Exception) { }
        return buttons
    }

    /**
     * Points the current item artwork at the ambient frame (or the ghais_logo
     * fallback). Mirrors PlayerBridge.updateMetadata's safe rule: never
     * replaces the item while playing — position/timeline is preserved, and
     * art lands on the next safe window instead.
     */
    private fun applyArtworkToSession(artUri: Uri?) {
        try {
            val p = player ?: return
            if (p.mediaItemCount == 0) return
            if (p.isPlaying) return
            val index = p.currentMediaItemIndex.coerceIn(0, p.mediaItemCount - 1)
            val current = p.getMediaItemAt(index)
            val target = artUri ?: fallbackArtworkUri()
            val currentArtwork = try { current.mediaMetadata.artworkUri?.toString() } catch (_: Exception) { null }
            if (currentArtwork == target.toString()) return
            val resumePos = try { p.currentPosition.coerceAtLeast(0L) } catch (_: Exception) { 0L }
            val meta = current.mediaMetadata.buildUpon()
                .setArtworkUri(target)
                .build()
            p.replaceMediaItem(index, current.buildUpon().setMediaMetadata(meta).build())
            if (resumePos > 1_000L) {
                try { p.seekTo(resumePos) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    private fun fallbackArtworkUri(): Uri {
        return try {
            Uri.parse("android.resource://$packageName/${R.drawable.ghais_logo}")
        } catch (_: Exception) {
            Uri.EMPTY
        }
    }

    private fun startForegroundWithPlaceholder() {
        try {
            val track = AudioEngine.currentTrack.value
            val initialTitle = track?.let { displayTitle(it.surahNameEn, it.ayahNo, it.isFullSurah) } ?: "Ghais"
            val initialSubtitle = track?.reciterName?.takeIf { it.isNotBlank() } ?: "Preparing recitation…"
            val placeholder = android.app.Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(initialTitle)
                .setContentText(initialSubtitle)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ghais_logo))
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

/**
 * Notification provider that appends the live subtitle
 * ("Sleeps in 12:00 • 1.25×") to the shade content text.
 * Timeline-safe: only overrides the rendered text, never the MediaItem.
 * Same channel/id as the previous Builder setup so the placeholder FGS
 * notification (id 1001) is replaced, not duplicated. The channel is
 * pre-created by the service, so [channelNameResourceId] (0) is never read
 * (DefaultMediaNotificationProvider.ensureNotificationChannel returns early
 * when the channel already exists — verified against media3 1.5.1 bytecode).
 */
private class QuranNotificationProvider(
    appContext: android.content.Context,
    notificationId: Int,
    channelId: String,
) : androidx.media3.session.DefaultMediaNotificationProvider(
    appContext,
    NotificationIdProvider { notificationId },
    channelId,
    0,
) {
    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence {
        return try {
            val base = try {
                super.getNotificationContentText(metadata)?.toString().orEmpty()
            } catch (_: Exception) {
                ""
            }
            val suffix = try {
                QuranPlaybackService.liveSubtitleSuffix()
            } catch (_: Exception) {
                ""
            }
            when {
                suffix.isBlank() && base.isBlank() -> "Ghais"
                suffix.isBlank() -> base
                base.isBlank() -> suffix
                else -> "$base • $suffix"
            }
        } catch (_: Exception) {
            try {
                super.getNotificationContentText(metadata) ?: "Ghais"
            } catch (_: Exception) {
                "Ghais"
            }
        }
    }
}

/**
 * Session BitmapLoader for shade artwork.
 * androidApp has no Coil dependency (coil3 lives in shared androidMain and is
 * not transitively visible), so artwork is decoded via
 * ContentResolver.openInputStream — handles file://, content:// and the
 * android.resource:// ghais_logo fallback alike. Never throws: every failure
 * falls back to the ghais_logo bitmap, then a 1×1 placeholder.
 */
private class ShadeBitmapLoader(
    appContext: android.content.Context,
) : androidx.media3.common.util.BitmapLoader {
    private val resolver = appContext.contentResolver
    private val resources = appContext.resources
    private val packageName = appContext.packageName

    override fun supportsMimeType(mimeType: String?): Boolean {
        return try {
            mimeType?.startsWith("image/") == true
        } catch (_: Exception) {
            false
        }
    }

    override fun decodeBitmap(data: ByteArray?): ListenableFuture<Bitmap> {
        return try {
            Futures.immediateFuture(decodeOrFallback(data))
        } catch (_: Exception) {
            Futures.immediateFuture(fallbackBitmap())
        }
    }

    override fun loadBitmap(uri: Uri?): ListenableFuture<Bitmap> {
        return try {
            var bitmap: Bitmap? = null
            try {
                if (uri != null) {
                    resolver.openInputStream(uri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {
                bitmap = null
            }
            Futures.immediateFuture(bitmap ?: fallbackBitmap())
        } catch (_: Exception) {
            Futures.immediateFuture(fallbackBitmap())
        }
    }

    private fun decodeOrFallback(data: ByteArray?): Bitmap {
        try {
            if (data != null) {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) return bitmap
            }
        } catch (_: Exception) { }
        return fallbackBitmap()
    }

    private fun fallbackBitmap(): Bitmap {
        try {
            val bitmap = BitmapFactory.decodeResource(resources, com.ghais.android.R.drawable.ghais_logo)
            if (bitmap != null) return bitmap
        } catch (_: Exception) { }
        return try {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            BitmapFactory.decodeResource(resources, android.R.drawable.sym_def_app_icon)
        }
    }
}
