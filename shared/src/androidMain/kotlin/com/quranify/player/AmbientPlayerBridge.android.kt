package com.quranify.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android actual: a second ExoPlayer that loops the ambient file
 * (REPEAT_MODE_ALL) underneath the main Quran player. Both use
 * USAGE_MEDIA so the framework mixes them; ambient volume is kept
 * below the recitation via [setAmbientVolume].
 *
 * Resource bytes come from shared composeResources and are cached to
 * `<cacheDir>/ambient/<key>.mp3` on first use (offline afterwards).
 */
actual object AmbientPlayerBridge {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mutex = Mutex()

    private var appContext: android.content.Context? = null
    private var ambientPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var preparedKey: String? = null
    private var pendingVolume: Float = 0.4f

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun player(): androidx.media3.exoplayer.ExoPlayer? {
        val ctx = appContext ?: return null
        var p = ambientPlayer
        if (p == null) {
            p = androidx.media3.exoplayer.ExoPlayer.Builder(ctx)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .build(),
                    false,
                )
                .setHandleAudioBecomingNoisy(false)
                .build()
            p.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            p.volume = pendingVolume
            ambientPlayer = p
        }
        return p
    }

    actual fun playAmbient(assetKey: String) {
        scope.launch {
            mutex.withLock {
                val p = player() ?: run {
                    android.util.Log.e("AmbientPlayer", "playAmbient($assetKey): no app context (init missing?)")
                    return@withLock
                }
                if (preparedKey == assetKey) {
                    // Same loop already loaded — just resume, never restart.
                    p.volume = pendingVolume
                    p.play()
                    return@withLock
                }
                val file = ensureCachedFile(assetKey) ?: run {
                    android.util.Log.e("AmbientPlayer", "playAmbient($assetKey): cache file unavailable")
                    return@withLock
                }
                try {
                    val item = androidx.media3.common.MediaItem.fromUri(
                        android.net.Uri.fromFile(file)
                    )
                    p.setMediaItem(item)
                    p.prepare()
                    p.volume = pendingVolume
                    p.play()
                    preparedKey = assetKey
                    android.util.Log.d("AmbientPlayer", "looping $assetKey (${file.length()} bytes)")
                } catch (e: Exception) {
                    android.util.Log.e("AmbientPlayer", "prepare/play($assetKey) failed file=$file", e)
                }
            }
        }
    }

    actual fun pauseAmbient() {
        try { ambientPlayer?.pause() } catch (_: Exception) { }
    }

    actual fun resumeAmbient() {
        try {
            // Resume only when something is loaded; never auto-load here
            // (loading is driven by explicit selection via playAmbient).
            if (preparedKey != null) ambientPlayer?.play()
        } catch (_: Exception) { }
    }

    actual fun stopAmbient() {
        scope.launch {
            mutex.withLock {
                try {
                    ambientPlayer?.stop()
                    ambientPlayer?.clearMediaItems()
                } catch (_: Exception) { }
                preparedKey = null
            }
        }
    }

    actual fun setAmbientVolume(volume: Float) {
        pendingVolume = volume.coerceIn(0f, 1f)
        try { ambientPlayer?.volume = pendingVolume } catch (_: Exception) { }
    }

    private suspend fun ensureCachedFile(assetKey: String): java.io.File? {
        val ctx = appContext ?: run {
            android.util.Log.e("AmbientPlayer", "ensureCachedFile($assetKey): appContext null — init() not called")
            return null
        }
        return try {
            val dir = java.io.File(ctx.cacheDir, "ambient").apply { mkdirs() }
            val out = java.io.File(dir, "$assetKey.mp3")
            if (!out.exists() || out.length() == 0L) {
                val bytes = quranify.shared.generated.resources.Res
                    .readBytes("files/ambient/$assetKey.mp3")
                out.writeBytes(bytes)
            }
            out.takeIf { it.exists() && it.length() > 0L }
                ?: run {
                    android.util.Log.e("AmbientPlayer", "ensureCachedFile($assetKey): wrote 0 bytes")
                    null
                }
        } catch (e: Exception) {
            android.util.Log.e("AmbientPlayer", "ensureCachedFile($assetKey) failed", e)
            null
        }
    }
}
