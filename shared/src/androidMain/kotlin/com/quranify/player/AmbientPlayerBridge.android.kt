package com.quranify.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var appContext: android.content.Context? = null
    private var ambientPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var preparedKey: String? = null
    private var pendingVolume: Float = 0.4f

    private sealed interface TargetState {
        data object Stopped : TargetState
        data class Paused(val assetKey: String?) : TargetState
        data class Playing(val assetKey: String) : TargetState
    }

    private var targetState: TargetState = TargetState.Stopped
    private var loadJob: kotlinx.coroutines.Job? = null

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
            targetState = TargetState.Playing(assetKey)

            val p = player() ?: run {
                android.util.Log.e("AmbientPlayer", "playAmbient($assetKey): no app context (init missing?)")
                return@launch
            }

            if (preparedKey == assetKey) {
                // Same loop already loaded — just resume, never restart.
                p.volume = pendingVolume
                p.play()
                return@launch
            }

            // Cancel any in-flight load for a previous asset
            loadJob?.cancel()
            loadJob = scope.launch {
                val file = ensureCachedFile(assetKey) ?: run {
                    android.util.Log.e("AmbientPlayer", "playAmbient($assetKey): cache file unavailable")
                    return@launch
                }

                if (targetState is TargetState.Stopped) return@launch

                try {
                    val item = androidx.media3.common.MediaItem.fromUri(
                        android.net.Uri.fromFile(file)
                    )
                    p.setMediaItem(item)
                    p.prepare()
                    p.volume = pendingVolume
                    preparedKey = assetKey

                    // Guard: only play if targetState is still Playing this asset
                    if (targetState == TargetState.Playing(assetKey)) {
                        p.play()
                        android.util.Log.d("AmbientPlayer", "looping $assetKey (${file.length()} bytes)")
                    } else {
                        android.util.Log.d("AmbientPlayer", "prepared $assetKey but targetState=$targetState; suppressing play()")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AmbientPlayer", "prepare/play($assetKey) failed file=$file", e)
                }
            }
        }
    }

    actual fun pauseAmbient() {
        scope.launch {
            targetState = TargetState.Paused(preparedKey)
            try { ambientPlayer?.pause() } catch (_: Exception) { }
        }
    }

    actual fun resumeAmbient() {
        scope.launch {
            val key = preparedKey
            if (key != null) {
                targetState = TargetState.Playing(key)
                try { ambientPlayer?.play() } catch (_: Exception) { }
            }
        }
    }

    actual fun stopAmbient() {
        scope.launch {
            targetState = TargetState.Stopped
            loadJob?.cancel()
            loadJob = null
            preparedKey = null
            try {
                ambientPlayer?.stop()
                ambientPlayer?.clearMediaItems()
            } catch (_: Exception) { }
        }
    }

    actual fun setAmbientVolume(volume: Float) {
        pendingVolume = volume.coerceIn(0f, 1f)
        scope.launch {
            try { ambientPlayer?.volume = pendingVolume } catch (_: Exception) { }
        }
    }

    private suspend fun ensureCachedFile(assetKey: String): java.io.File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val ctx = appContext ?: run {
                android.util.Log.e("AmbientPlayer", "ensureCachedFile($assetKey): appContext null — init() not called")
                return@withContext null
            }
            try {
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
