package com.ghais.player

/**
 * In-memory LRU of resolved preview URLs (`cacheKey -> url`, max 50) so the
 * reciter browser does not re-resolve (or re-hit any future network check)
 * when the user flings through voices.
 *
 * Pure commonMain, no platform APIs — safe to use from commonTest.
 * Access-ordered [LinkedHashMap]; eldest entry evicted on overflow.
 * NOT thread-safe by design (single-threaded UI resolution path); callers
 * that resolve off the main thread must serialize externally.
 */
object PreviewCache {
    const val MAX_SIZE: Int = 50

    private val map = LinkedHashMap<String, String>(64, 0.75f, true)

    fun get(key: String): String? = map[key]

    fun put(key: String, url: String) {
        map[key] = url
        while (map.size > MAX_SIZE) {
            val eldest = map.keys.firstOrNull() ?: break
            map.remove(eldest)
        }
    }

    fun contains(key: String): Boolean = map.containsKey(key)

    fun clear() {
        map.clear()
    }

    val size: Int
        get() = map.size

    /** Oldest-first snapshot, for tests/debug only. */
    fun keysSnapshot(): List<String> = map.keys.toList()
}
