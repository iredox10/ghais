package com.ghais.player

import android.content.Context
import android.util.Log
import com.russhwolf.settings.Settings
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android actual: downloads per-surah mp3s with HttpURLConnection into
 * `<filesDir>/quran/<slug>/<surahId>.mp3` and persists the downloaded key
 * set via multiplatform Settings.
 *
 * Note: multiplatform-settings 1.3.0 has no StringSet API, so the set is
 * stored as a newline-joined String under the same "quran_downloaded" key.
 */
actual object QuranDownloads {
    private const val TAG = "QuranDownloads"
    private const val PREF_KEY = "quran_downloaded"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var appContext: Context? = null

    private val settings: Settings by lazy { Settings() }

    private val _downloadedKeys = MutableStateFlow(emptySet<String>())
    actual val downloadedKeys: StateFlow<Set<String>> = _downloadedKeys.asStateFlow()

    private val _progress = MutableStateFlow(emptyMap<String, Float>())
    actual val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    private val _failedKeys = MutableStateFlow(emptySet<String>())
    actual val failedKeys: StateFlow<Set<String>> = _failedKeys.asStateFlow()

    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val jobs = ConcurrentHashMap<String, Job>()

    /**
     * Must be called once from Android (e.g. MainActivity.onCreate) so the
     * singleton can resolve filesDir and restore the persisted set.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        try {
            _downloadedKeys.value = readPersisted()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load downloaded set", e)
        }
    }

    private fun contextOrNull(): Context? {
        val ctx = appContext
        if (ctx == null) {
            Log.e(TAG, "QuranDownloads used before init — call QuranDownloads.init(context) from MainActivity")
        }
        return ctx
    }

    private fun baseDir(ctx: Context): File = File(ctx.filesDir, "quran")

    private fun fileFor(ctx: Context, slug: String, surahId: Int): File =
        File(ctx.filesDir, "quran/$slug/$surahId.mp3")

    private fun readPersisted(): Set<String> {
        val raw = settings.getStringOrNull(PREF_KEY) ?: return emptySet()
        if (raw.isEmpty()) return emptySet()
        return raw.split("\n").filter { it.isNotEmpty() }.toSet()
    }

    private fun persist() {
        try {
            settings.putString(PREF_KEY, _downloadedKeys.value.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist downloaded set", e)
        }
    }

    actual fun isDownloaded(slug: String, surahId: Int): Boolean =
        _downloadedKeys.value.contains(DownloadKeys.key(slug, surahId))

    actual fun progressOf(slug: String, surahId: Int): Float? =
        _progress.value[DownloadKeys.key(slug, surahId)]

    actual fun download(slug: String, surahId: Int, url: String) {
        val ctx = contextOrNull() ?: return
        val key = DownloadKeys.key(slug, surahId)
        if (_downloadedKeys.value.contains(key)) return
        if (!inFlight.add(key)) return
        _failedKeys.value = _failedKeys.value - key
        _progress.value = _progress.value + (key to -1f)
        val job = scope.launch(Dispatchers.IO) {
            val finalFile = fileFor(ctx, slug, surahId)
            val partFile = File(finalFile.parent, "${finalFile.name}.part")
            var success = false
            try {
                finalFile.parentFile?.mkdirs()
                var connection: HttpURLConnection? = null
                try {
                    connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        requestMethod = "GET"
                        connect()
                    }
                    val code = connection.responseCode
                    if (code !in 200..299) throw IOException("HTTP $code for $url")
                    val total = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        FileOutputStream(partFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead = 0L
                            while (true) {
                                ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                bytesRead += n
                                val p = if (total > 0) {
                                    (bytesRead.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    -1f
                                }
                                _progress.value = _progress.value + (key to p)
                            }
                        }
                    }
                } finally {
                    try {
                        connection?.disconnect()
                    } catch (_: Exception) {
                    }
                }
                ensureActive()
                if (!partFile.renameTo(finalFile)) {
                    partFile.copyTo(finalFile, overwrite = true)
                    partFile.delete()
                }
                success = true
                _downloadedKeys.value = _downloadedKeys.value + key
                persist()
                _progress.value = _progress.value - key
                _failedKeys.value = _failedKeys.value - key
            } catch (e: Exception) {
                if (e is CancellationException) {
                    _progress.value = _progress.value - key
                    throw e
                }
                Log.e(TAG, "Download failed: $key", e)
                _failedKeys.value = _failedKeys.value + key
                _progress.value = _progress.value - key
            } finally {
                if (!success) {
                    try {
                        partFile.delete()
                    } catch (_: Exception) {
                    }
                }
                inFlight.remove(key)
                jobs.remove(key)
            }
        }
        jobs[key] = job
        job.invokeOnCompletion { jobs.remove(key, job) }
    }

    actual fun delete(slug: String, surahId: Int) {
        val key = DownloadKeys.key(slug, surahId)
        jobs.remove(key)?.cancel()
        inFlight.remove(key)
        _progress.value = _progress.value - key
        _failedKeys.value = _failedKeys.value - key
        if (_downloadedKeys.value.contains(key)) {
            _downloadedKeys.value = _downloadedKeys.value - key
            persist()
        }
        val ctx = contextOrNull()
        if (ctx != null) {
            try {
                val finalFile = fileFor(ctx, slug, surahId)
                finalFile.delete()
                File(finalFile.parent, "${finalFile.name}.part").delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete file for $key", e)
            }
        }
    }

    actual fun localUri(slug: String, surahId: Int): String? {
        val ctx = contextOrNull() ?: return null
        val key = DownloadKeys.key(slug, surahId)
        if (!_downloadedKeys.value.contains(key)) return null
        val file = fileFor(ctx, slug, surahId)
        if (!file.exists()) return null
        return "file://${file.absolutePath}"
    }

    actual suspend fun storageBytes(): Long {
        val ctx = contextOrNull() ?: return 0L
        return try {
            withContext(Dispatchers.IO) {
                val dir = baseDir(ctx)
                if (!dir.exists()) return@withContext 0L
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute storage bytes", e)
            0L
        }
    }

    actual suspend fun clearAll() {
        jobs.values.toList().forEach {
            try {
                it.cancel()
            } catch (_: Exception) {
            }
        }
        jobs.clear()
        inFlight.clear()
        val ctx = appContext
        if (ctx != null) {
            try {
                withContext(Dispatchers.IO) {
                    baseDir(ctx).listFiles()?.forEach {
                        try {
                            it.deleteRecursively()
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear download directory", e)
            }
        } else {
            Log.e(TAG, "QuranDownloads used before init — call QuranDownloads.init(context) from MainActivity")
        }
        _downloadedKeys.value = emptySet()
        _progress.value = emptyMap()
        _failedKeys.value = emptySet()
        try {
            settings.remove(PREF_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear persisted downloaded set", e)
        }
    }
}
