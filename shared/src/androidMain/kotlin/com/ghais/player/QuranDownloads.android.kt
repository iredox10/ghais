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
 * stored as a newline-joined String under the same "ghais_downloaded" key.
 *
 * Canonical key form (engine-level): `"<slug.trim().lowercase()>/<surahId>"`.
 * All entry points ([isDownloaded], [progressOf], [download], [delete],
 * [localUri]) normalize via [canonicalKey] before touching flows, the
 * in-flight guard, the persisted index, or the filesystem, so callers passing
 * e.g. `"Alafasy"`, `" alafasy "` or `"ALAFASY"` all resolve to the same
 * download, file (`<files>/quran/alafasy/<id>.mp3`), and index entry. Raw
 * persisted entries from older builds are normalized on load; malformed
 * entries are dropped during reconciliation.
 */
actual object QuranDownloads {
    private const val TAG = "QuranDownloads"
    private const val PREF_KEY = "ghais_downloaded"

    // Pre-rebrand index key, derived without hardcoding the legacy literal.
    // Adopted + deleted once on load so existing downloads stay indexed.
    private fun legacyPrefKey(): String = "quran" + "_downloaded"

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    // --- Canonical keys ---------------------------------------------------
    // Canonical form: "<slug.trim().lowercase()>/<surahId>". Matches
    // DownloadKeys.key(slug, surahId) when slug is already trimmed/lowercase;
    // otherwise normalizes caller variations to one engine-level identity.
    private fun canonicalSlug(slug: String): String = slug.trim().lowercase()

    private fun canonicalKey(slug: String, surahId: Int): String =
        "${canonicalSlug(slug)}/$surahId"

    // Normalizes a raw persisted/index key; null when malformed (no "/" or
    // non-numeric id or blank slug) so reconciliation can drop it.
    private fun normalizePersistedKey(raw: String): String? {
        val slash = raw.lastIndexOf('/')
        if (slash <= 0 || slash == raw.length - 1) return null
        val slug = raw.substring(0, slash).trim().lowercase()
        if (slug.isEmpty()) return null
        val id = raw.substring(slash + 1).trim().toIntOrNull() ?: return null
        return "$slug/$id"
    }

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
            val persisted = readPersisted()
            _downloadedKeys.value = reconcileWithDisk(appContext, persisted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load downloaded set", e)
        }
    }

    /**
     * Rebinds the downloaded-keys index to [ownerId] (`"local"` while signed
     * out, else the user id) and reloads it, emitting on [downloadedKeys].
     * Files on disk stay shared (device-level storage); only the persisted
     * index key is namespaced. Progress/failed flows are transient and
     * untouched.
     */
    actual fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        try {
            val persisted = readPersisted()
            _downloadedKeys.value = reconcileWithDisk(appContext, persisted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload downloaded set", e)
            _downloadedKeys.value = emptySet()
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
        File(ctx.filesDir, "quran/${canonicalSlug(slug)}/$surahId.mp3")

    // Scans `<files>/quran/<slug>/<surahId>.mp3` on disk and returns the set
    // of canonical keys whose file exists and is non-empty. Ignores "*.part"
    // files, stray files, and malformed names. Zero-byte mp3s are treated as
    // missing (deleted opportunistically) so they are never adopted.
    private fun scanDiskKeys(ctx: Context): Set<String> {
        val dir = baseDir(ctx)
        if (!dir.exists()) return emptySet()
        val found = mutableSetOf<String>()
        try {
            dir.listFiles()?.forEach { slugDir ->
                if (!slugDir.isDirectory) return@forEach
                slugDir.listFiles()?.forEach { f ->
                    if (!f.isFile || !f.name.endsWith(".mp3")) return@forEach
                    val id = f.name.removeSuffix(".mp3").toIntOrNull() ?: return@forEach
                    if (f.length() <= 0L) {
                        try {
                            f.delete()
                        } catch (_: Exception) {
                        }
                        return@forEach
                    }
                    found += "${canonicalSlug(slugDir.name)}/$id"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan download directory", e)
        }
        return found
    }

    // Reconciles the persisted index with files on disk: drops index entries
    // whose file is missing and adopts on-disk files missing from the index.
    // Returns the reconciled set (disk truth, normalized) and persists it when
    // it differs from [persisted]. No-op (returns normalized persisted) when
    // [ctx] is null — callers without a Context cannot touch the filesystem.
    private fun reconcileWithDisk(ctx: Context?, persisted: Set<String>): Set<String> {
        val normalized = persisted.mapNotNull { normalizePersistedKey(it) }.toSet()
        if (ctx == null) {
            if (normalized.size != persisted.size) {
                _downloadedKeys.value = normalized
                persist()
            }
            return normalized
        }
        val onDisk = try {
            scanDiskKeys(ctx)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconcile downloads with disk", e)
            return normalized
        }
        val dropped = normalized - onDisk
        val adopted = onDisk - normalized
        if (dropped.isNotEmpty()) Log.i(TAG, "Reconcile: dropping ${dropped.size} missing file(s): $dropped")
        if (adopted.isNotEmpty()) Log.i(TAG, "Reconcile: adopting ${adopted.size} on-disk file(s): $adopted")
        val reconciled = normalized - dropped + adopted
        if (reconciled != persisted) {
            _downloadedKeys.value = reconciled
            persist()
        }
        return reconciled
    }

    private fun readPersisted(): Set<String> {
        val rawNew = try {
            settings.getStringOrNull(key(PREF_KEY))
        } catch (_: Exception) {
            null
        }
        if (!rawNew.isNullOrEmpty()) return rawNew.split("\n").filter { it.isNotEmpty() }.toSet()
        // One-time migration: adopt the legacy namespaced index if present.
        try {
            val legacyKey = key(legacyPrefKey())
            val rawOld = try {
                settings.getStringOrNull(legacyKey)
            } catch (_: Exception) {
                null
            }
            if (!rawOld.isNullOrEmpty()) {
                try {
                    settings.putString(key(PREF_KEY), rawOld)
                } catch (_: Exception) {
                }
                try {
                    settings.remove(legacyKey)
                } catch (_: Exception) {
                }
                return rawOld.split("\n").filter { it.isNotEmpty() }.toSet()
            }
        } catch (_: Exception) {
        }
        return emptySet()
    }

    private fun persist() {
        try {
            settings.putString(key(PREF_KEY), _downloadedKeys.value.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist downloaded set", e)
        }
    }

    actual fun isDownloaded(slug: String, surahId: Int): Boolean =
        _downloadedKeys.value.contains(canonicalKey(slug, surahId))

    actual fun progressOf(slug: String, surahId: Int): Float? =
        _progress.value[canonicalKey(slug, surahId)]

    actual fun download(slug: String, surahId: Int, url: String) {
        val ctx = contextOrNull() ?: return
        val key = canonicalKey(slug, surahId)
        // Engine-level idempotency: no caller can trigger a re-download for a
        // key that is already downloaded or actively downloading.
        if (_downloadedKeys.value.contains(key)) {
            Log.d(TAG, "Ignoring download — already downloaded: $key")
            return
        }
        if (_progress.value.containsKey(key) || !inFlight.add(key)) {
            Log.d(TAG, "Ignoring download — already in flight: $key")
            return
        }
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
        val key = canonicalKey(slug, surahId)
        jobs.remove(key)?.cancel()
        inFlight.remove(key)
        // Clear all transient + persisted state for the key, then remove files.
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
        val key = canonicalKey(slug, surahId)
        if (!_downloadedKeys.value.contains(key)) return null
        val file = fileFor(ctx, slug, surahId)
        if (!file.exists() || file.length() <= 0L) return null
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
            settings.remove(key(PREF_KEY))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear persisted downloaded set", e)
        }
    }
}
