package com.ghais.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Extracts still frames from the active ambient video for lockscreen artwork.
 *
 * Frames are cached as PNGs under `<cacheDir>/video_art/<key>.png` and exposed
 * via [artworkUri]. Driven by [AmbientVideoBridge] hooks; never throws.
 */
object AmbientVideoArt {
    private const val TAG = "AmbientVideoArt"
    private const val FRAME_US = 1_500_000L
    private const val MAX_SIDE = 512
    private const val DEBOUNCE_MS = 3_000L

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var appContext: Context? = null

    private val _artworkUri = MutableStateFlow<Uri?>(null)
    val artworkUri: StateFlow<Uri?> = _artworkUri.asStateFlow()

    private val debounceLock = Any()
    private var lastKey: String? = null
    private var lastAt: Long = 0L

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun refreshFor(assetKey: String?) {
        if (assetKey.isNullOrBlank()) {
            synchronized(debounceLock) {
                lastKey = null
                lastAt = 0L
            }
            scope.launch(Dispatchers.IO) {
                try {
                    artDir()?.listFiles()?.forEach {
                        try {
                            it.delete()
                        } catch (e: Exception) {
                            Log.e(TAG, "cache clear failed for ${it.name}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "refreshFor(null) failed", e)
                }
                _artworkUri.value = null
            }
            return
        }
        val key = assetKey
        synchronized(debounceLock) {
            val now = SystemClock.elapsedRealtime()
            if (key == lastKey && now - lastAt < DEBOUNCE_MS) return
            lastKey = key
            lastAt = now
        }
        scope.launch(Dispatchers.IO) {
            try {
                val ctx = appContext ?: return@launch
                val dir = artDir() ?: return@launch
                val out = File(dir, "${safeName(key)}.png")
                if (out.exists()) {
                    _artworkUri.value = Uri.fromFile(out)
                    return@launch
                }
                val retriever = MediaMetadataRetriever()
                try {
                    ctx.assets.openFd("videos/$key.mp4").use { afd ->
                        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        val frame = retriever.getFrameAtTime(
                            FRAME_US,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        ) ?: return@launch
                        try {
                            val scaled = scaleToMaxSide(frame, MAX_SIDE)
                            try {
                                FileOutputStream(out).use { fos ->
                                    scaled.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                }
                                _artworkUri.value = Uri.fromFile(out)
                            } finally {
                                if (scaled !== frame) scaled.recycle()
                            }
                        } finally {
                            frame.recycle()
                        }
                    }
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "retriever release failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "refreshFor($key) failed", e)
            }
        }
    }

    private fun artDir(): File? {
        val ctx = appContext ?: return null
        return File(ctx.cacheDir, "video_art").apply { mkdirs() }
    }

    private fun safeName(key: String): String =
        key.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun scaleToMaxSide(frame: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(frame.width, frame.height)
        if (longest <= maxSide || longest <= 0) return frame
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            frame,
            (frame.width * scale).toInt().coerceAtLeast(1),
            (frame.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}
