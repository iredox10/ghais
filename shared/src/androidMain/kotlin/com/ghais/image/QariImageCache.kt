package com.ghais.image

import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.sync.NetworkMonitor
import com.ghais.ui.screens.reciters.ALL_VERIFIED_RECITERS
import com.ghais.ui.screens.reciters.resolveReciterPhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Offline image cache for qari photos — Telegram-style: pre-warm while
 * online, serve from cache after.
 *
 * Coil 3's default singleton has no disk cache at all, so reciter photos
 * re-downloaded every session and collapsed into monograms when offline.
 * [install] replaces it with a loader that keeps a persistent LRU disk cache
 * under `cacheDir/qari_photos`; [QariPhotoPreheater] then proactively fills
 * that cache while the device is online, so avatars and heroes keep
 * rendering even with no connection.
 */
object QariImageCache {

    private const val TAG = "QariImageCache"

    // 128 MB is generous: ~100 reciter photos are only a few MB together,
    // and the same cache also warms every other app image. ENABLED is the
    // read-and-write policy, so offline loads hit this cache directly.
    private const val DISK_CACHE_BYTES = 128L * 1024 * 1024

    /**
     * Installs the singleton image loader. Must run before the first Coil
     * use (first `AsyncImage` compose) — it is the first guardInit in
     * [MainActivity.onCreate]. `setSafe` is a no-op if a loader was already
     * set (same-process recreation), and throws inside guardInit if it is
     * genuinely too late, which surfaces the ordering bug loudly.
     */
    fun install(context: Context) {
        SingletonImageLoader.setSafe { ctx ->
            ImageLoader.Builder(ctx)
                .diskCache {
                    DiskCache.Builder()
                        .directory(ctx.cacheDir.resolve("qari_photos"))
                        .maxSizeBytes(DISK_CACHE_BYTES)
                        .build()
                }
                .build()
        }
        Log.i(TAG, "Qari image cache installed (128 MB LRU)")
    }
}

/**
 * Proactive qari-photo preheater: watches [NetworkMonitor.isOnline] and
 * fires the fetch pass on every online window, so newly added reciters and
 * previously failed photos are retried automatically once connectivity
 * returns. Every image file lands in the disk cache configured by
 * [QariImageCache.install].
 */
object QariPhotoPreheater {

    private const val TAG = "QariPhotoPreheater"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = AtomicBoolean(false)
    private var started = false

    /** Starts the connectivity watcher. Safe to call once per process. */
    fun start(context: Context) {
        if (started) return
        started = true
        val appCtx = context.applicationContext
        scope.launch {
            NetworkMonitor.isOnline.collectLatest { online ->
                if (online) preheat(appCtx)
            }
        }
        Log.i(TAG, "Qari photo preheater started")
    }

    /**
     * Enqueues every resolved qari photo URL into the singleton loader so
     * the network fetcher writes each file into the persistent disk cache.
     * Already-cached URLs short-circuit without a network round trip, so the
     * pass is cheap to re-run; mid-pass disconnects just stop enqueueing and
     * the remaining photos are picked up on the next online window.
     *
     * Requests decode at 1x1 with the memory cache disabled — only the raw
     * disk bytes matter here, which keeps CPU, RAM and the shared decode
     * queue out of the UI's way.
     */
    private suspend fun preheat(appCtx: Context) {
        if (!running.compareAndSet(false, true)) return
        try {
            val loader = SingletonImageLoader.get(appCtx)
            val urls = buildSet {
                fun addFor(slug: String) {
                    resolveReciterPhoto(slug)?.let { add(it) }
                }
                QuranDataRepository.getBrowseReciters().forEach { addFor(it.slug) }
                ALL_VERIFIED_RECITERS.forEach { addFor(it.slug) }
            }
            urls.forEach { url ->
                // Stop enqueueing as soon as the device goes offline; the
                // next online window retriggers the pass for the rest.
                if (!NetworkMonitor.isOnline.value) return
                loader.enqueue(
                    ImageRequest.Builder(appCtx)
                        .data(url)
                        .size(1, 1)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                )
            }
            Log.i(TAG, "Qari photo preheat enqueued ${urls.size} photos")
        } catch (t: Throwable) {
            Log.w(TAG, "Qari photo preheat failed", t)
        } finally {
            running.set(false)
        }
    }
}
