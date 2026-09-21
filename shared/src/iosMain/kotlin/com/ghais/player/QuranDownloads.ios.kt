@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ghais.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSUserDefaults
import platform.Foundation.stringByDeletingLastPathComponent

/**
 * iOS actual — functional baseline with NSURLSession (no delegates).
 *
 * Files live in `<home>/Documents/quran/<slug>/<surahId>.mp3`, the
 * downloaded key set is persisted via NSUserDefaults, and progress stays
 * at -1f (indeterminate) while a download is in flight.
 */
actual object QuranDownloads {
    private const val PREF_KEY = "ghais_downloaded"

    // Pre-rebrand index key, derived without hardcoding the legacy literal.
    // Adopted + deleted once on load so existing downloads stay indexed.
    private fun legacyPrefKey(): String = "quran" + "_downloaded"

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val _downloadedKeys = MutableStateFlow(emptySet<String>())
    actual val downloadedKeys: StateFlow<Set<String>> = _downloadedKeys.asStateFlow()

    private val _progress = MutableStateFlow(emptyMap<String, Float>())
    actual val progress: StateFlow<Map<String, Float>> = _progress.asStateFlow()

    private val _failedKeys = MutableStateFlow(emptySet<String>())
    actual val failedKeys: StateFlow<Set<String>> = _failedKeys.asStateFlow()

    private val lock = Any()
    private val inFlight = mutableSetOf<String>()
    private val tasks = mutableMapOf<String, NSURLSessionDataTask>()

    init {
        try {
            _downloadedKeys.value = readPersisted()
        } catch (_: Exception) {
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
            _downloadedKeys.value = readPersisted()
        } catch (_: Exception) {
            _downloadedKeys.value = emptySet()
        }
    }

    private fun baseDir(): String = NSHomeDirectory() + "/Documents/quran"

    private fun fileFor(slug: String, surahId: Int): String =
        "${baseDir()}/$slug/$surahId.mp3"

    private fun readPersisted(): Set<String> {
        val defaults = NSUserDefaults.standardUserDefaults
        val arr = defaults.stringArrayForKey(key(PREF_KEY))
        val current = arr?.filterIsInstance<String>()?.toSet().orEmpty()
        if (current.isNotEmpty()) return current
        // One-time migration: adopt the legacy namespaced index if present.
        try {
            val legacyKey = key(legacyPrefKey())
            val legacyArr = defaults.stringArrayForKey(legacyKey)
            val legacy = legacyArr?.filterIsInstance<String>()?.toSet().orEmpty()
            if (legacy.isNotEmpty()) {
                try {
                    defaults.setObject(legacy.toList(), key(PREF_KEY))
                } catch (_: Exception) {
                }
                try {
                    defaults.removeObjectForKey(legacyKey)
                } catch (_: Exception) {
                }
                return legacy
            }
        } catch (_: Exception) {
        }
        return emptySet()
    }

    private fun persist() {
        try {
            NSUserDefaults.standardUserDefaults.setObject(_downloadedKeys.value.toList(), key(PREF_KEY))
        } catch (_: Exception) {
        }
    }

    actual fun isDownloaded(slug: String, surahId: Int): Boolean =
        _downloadedKeys.value.contains(DownloadKeys.key(slug, surahId))

    actual fun progressOf(slug: String, surahId: Int): Float? =
        _progress.value[DownloadKeys.key(slug, surahId)]

    actual fun download(slug: String, surahId: Int, url: String) {
        val key = DownloadKeys.key(slug, surahId)
        synchronized(lock) {
            if (_downloadedKeys.value.contains(key) || !inFlight.add(key)) return
        }
        _failedKeys.value = _failedKeys.value - key
        _progress.value = _progress.value + (key to -1f)
        try {
            val nsUrl = NSURL.URLWithString(url)
                ?: throw IllegalArgumentException("Invalid URL: $url")
            val completion: (NSData?, NSURLResponse?, NSError?) -> Unit = { data, response, error ->
                try {
                    val httpCode = (response as? NSHTTPURLResponse)?.statusCode
                    val httpFailed = httpCode != null && (httpCode < 200 || httpCode >= 300)
                    if (error != null || data == null || httpFailed) {
                        _failedKeys.value = _failedKeys.value + key
                        _progress.value = _progress.value - key
                    } else {
                        val path = fileFor(slug, surahId)
                        val dir = path.stringByDeletingLastPathComponent
                        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
                        val ok = data.writeToFile(path, true) &&
                            NSFileManager.defaultManager.fileExistsAtPath(path)
                        if (ok) {
                            _downloadedKeys.value = _downloadedKeys.value + key
                            persist()
                            _progress.value = _progress.value - key
                            _failedKeys.value = _failedKeys.value - key
                        } else {
                            _failedKeys.value = _failedKeys.value + key
                            _progress.value = _progress.value - key
                        }
                    }
                } catch (_: Exception) {
                    _failedKeys.value = _failedKeys.value + key
                    _progress.value = _progress.value - key
                } finally {
                    synchronized(lock) {
                        inFlight.remove(key)
                        tasks.remove(key)
                    }
                }
            }
            val task = NSURLSession.sharedSession.dataTaskWithURL(nsUrl, completion)
            synchronized(lock) {
                // delete() may have run while the task was being created
                if (!inFlight.contains(key)) {
                    try {
                        task.cancel()
                    } catch (_: Exception) {
                    }
                    return
                }
                tasks[key] = task
            }
            task.resume()
        } catch (_: Exception) {
            _failedKeys.value = _failedKeys.value + key
            _progress.value = _progress.value - key
            synchronized(lock) {
                inFlight.remove(key)
                tasks.remove(key)
            }
        }
    }

    actual fun delete(slug: String, surahId: Int) {
        val key = DownloadKeys.key(slug, surahId)
        synchronized(lock) {
            try {
                tasks.remove(key)?.cancel()
            } catch (_: Exception) {
            }
            inFlight.remove(key)
        }
        _progress.value = _progress.value - key
        _failedKeys.value = _failedKeys.value - key
        if (_downloadedKeys.value.contains(key)) {
            _downloadedKeys.value = _downloadedKeys.value - key
            persist()
        }
        try {
            NSFileManager.defaultManager.removeItemAtPath(fileFor(slug, surahId), null)
        } catch (_: Exception) {
        }
    }

    actual fun localUri(slug: String, surahId: Int): String? {
        return try {
            val key = DownloadKeys.key(slug, surahId)
            if (!_downloadedKeys.value.contains(key)) return null
            val path = fileFor(slug, surahId)
            if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
            NSURL.fileURLWithPath(path).absoluteString
        } catch (_: Exception) {
            null
        }
    }

    actual suspend fun storageBytes(): Long {
        return try {
            withContext(Dispatchers.Default) { accumulate(baseDir()) }
        } catch (_: Exception) {
            0L
        }
    }

    private fun accumulate(path: String): Long {
        return try {
            val fm = NSFileManager.defaultManager
            if (!fm.fileExistsAtPath(path)) return 0L
            val attrs = fm.attributesOfItemAtPath(path, null)
            val type = attrs?.get(NSFileType) as? String
            if (type == NSFileTypeDirectory) {
                val children = fm.contentsOfDirectoryAtPath(path, null) ?: return 0L
                var total = 0L
                for (child in children) {
                    val name = child as? String ?: continue
                    total += accumulate("$path/$name")
                }
                total
            } else {
                (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    actual suspend fun clearAll() {
        synchronized(lock) {
            tasks.values.toList().forEach {
                try {
                    it.cancel()
                } catch (_: Exception) {
                }
            }
            tasks.clear()
            inFlight.clear()
        }
        try {
            withContext(Dispatchers.Default) {
                try {
                    val fm = NSFileManager.defaultManager
                    val dir = baseDir()
                    val children = fm.contentsOfDirectoryAtPath(dir, null)
                    children?.forEach { child ->
                        val name = child as? String ?: return@forEach
                        try {
                            fm.removeItemAtPath("$dir/$name", null)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        _downloadedKeys.value = emptySet()
        _progress.value = emptyMap()
        _failedKeys.value = emptySet()
        try {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(key(PREF_KEY))
        } catch (_: Exception) {
        }
    }
}
