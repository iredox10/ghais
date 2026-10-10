package com.ghais.data.repository

import com.ghais.data.auth.AppwriteConfig
import com.ghais.data.sync.DB_ID
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

/**
 * Cloud charts client: top-qari leaderboards + trending reciters.
 *
 * Reads the single aggregated document `global` from the public `charts`
 * collection in database [DB_ID] (`ghais`) — the same database id the
 * editorial catalog pull uses (`SyncEngine.refreshEditorial()` reads
 * `playlists` / `playlist_items` there). Anonymous-safe: the collection is
 * public-read, so the request carries only the `X-Appwrite-Project` header
 * and no owner id / session, exactly like the public `broadcasts` and
 * `reciter_stats` reads.
 *
 * Transport is raw Appwrite REST over Ktor (`GET
 * …/databases/ghais/collections/charts/documents/global`) rather than the
 * Appwrite SDK `Databases` client on purpose: the SDK is JVM-only
 * (androidMain) with an iOS stub, while a plain [HttpClient] works on every
 * platform — and it makes the whole pull testable in commonTest with a fake
 * engine, mirroring [QuranAyahRepository].
 *
 * Caching (mirrors [QuranAyahRepository]'s disk layer + [EditorialRepository]'s
 * last-good rule):
 * - Disk via multiplatform-settings (`charts_`-prefixed keys), 12h TTL.
 * - [refresh] publishes the disk cache to [snapshot] instantly (even when
 *   stale, so collectors paint without waiting), skips the network entirely
 *   while the cache is fresh, and refetches when stale or missing.
 * - ANY failure (offline / 404 / unparsable body / never-fetched) keeps the
 *   previous snapshot — or null when nothing was ever fetched — and never
 *   throws.
 */
object ChartsRepository {

    /** Public collection holding the single aggregated charts document. */
    const val CHARTS_COLLECTION = "charts"

    /** Document id of the global charts rollup inside [CHARTS_COLLECTION]. */
    const val CHARTS_DOC_ID = "global"

    /**
     * Disk-cache TTL: a cached snapshot younger than this is served with no
     * network hit; older than this, [refresh] refetches in the background
     * while the stale snapshot stays on screen.
     */
    const val CHARTS_TTL_MS = 12L * 60L * 60L * 1000L

    private const val KEY_SNAPSHOT = "charts_snapshot_json"
    private const val KEY_FETCHED_AT = "charts_fetched_at_ms"

    @Serializable
    data class ChartsSnapshot(
        val topQaris7d: List<String> = emptyList(),
        val topQaris30d: List<String> = emptyList(),
        val trending: List<String> = emptyList(),
        val generatedAt: Long = 0L,
    )

    private val _snapshot = MutableStateFlow<ChartsSnapshot?>(null)
    val snapshot: StateFlow<ChartsSnapshot?> = _snapshot.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var customHttpClient: HttpClient? = null

    private val defaultHttpClient: HttpClient? by lazy {
        try {
            HttpClient()
        } catch (_: Exception) {
            null
        }
    }

    private val httpClient: HttpClient?
        get() = customHttpClient ?: defaultHttpClient

    /**
     * Optional hook to inject a configured [HttpClient] (e.g. a test fake);
     * pass null to fall back to the lazily built default client. Mirrors
     * [QuranAyahRepository.configureHttpClient].
     */
    fun configureHttpClient(client: HttpClient?) {
        customHttpClient = client
    }

    // Disk cache so charts survive restarts and render offline. Same
    // construction fallback as [QuranAyahRepository.textSettings]: the no-arg
    // `Settings()` factory throws on the JVM unit-test classpath (no platform
    // context), where a process-local map keeps every read/write path
    // executing and testable instead of silently no-oping.
    private val cacheSettings: Settings by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            InMemorySettings()
        }
    }

    private val mutex = Mutex()

    /**
     * Test-only clock: TTL tests advance time without sleeping 12h.
     * Same-module internal so production code always uses the real clock.
     */
    internal var nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }

    private val documentUrl =
        "${AppwriteConfig.ENDPOINT}/databases/$DB_ID/collections/$CHARTS_COLLECTION/documents/$CHARTS_DOC_ID"

    init {
        // Serve disk cache instantly so the first collector paints without
        // waiting for a refresh round-trip.
        try {
            loadCached()?.let { _snapshot.value = it.snapshot }
        } catch (_: Exception) {
        }
    }

    /**
     * Refreshes [snapshot] from disk cache + cloud. Publishes the cached
     * snapshot instantly (even stale), returns without a network hit while it
     * is fresh, and refetches when stale or missing. Never throws: on ANY
     * failure the last-good snapshot is kept (null when never fetched).
     */
    suspend fun refresh() {
        try {
            mutex.withLock {
                val cached = loadCached()
                if (cached != null) _snapshot.value = cached.snapshot
                if (cached != null && !isStale(cached.fetchedAt)) return
                val fresh = fetchRemote() ?: return
                persist(fresh, nowMs())
                _snapshot.value = fresh
            }
        } catch (_: Exception) {
            // Belt-and-braces: refresh never throws.
        }
    }

    /**
     * Resets the singleton for tests: clears the in-memory snapshot, drops
     * the injected client, restores the real clock, and wipes the `charts_`
     * disk keys. Same-module internal so production cannot call it.
     */
    internal fun resetForTest() {
        _snapshot.value = null
        customHttpClient = null
        nowMs = { Clock.System.now().toEpochMilliseconds() }
        try {
            cacheSettings.remove(KEY_SNAPSHOT)
            cacheSettings.remove(KEY_FETCHED_AT)
        } catch (_: Exception) {
        }
    }

    private data class CachedCharts(val snapshot: ChartsSnapshot, val fetchedAt: Long)

    private fun loadCached(): CachedCharts? = try {
        val raw = cacheSettings.getStringOrNull(KEY_SNAPSHOT) ?: return null
        val fetchedAt = cacheSettings.getLongOrNull(KEY_FETCHED_AT) ?: return null
        CachedCharts(json.decodeFromString(ChartsSnapshot.serializer(), raw), fetchedAt)
    } catch (_: Exception) {
        null
    }

    private fun isStale(fetchedAt: Long): Boolean {
        if (fetchedAt <= 0L) return true
        return nowMs() - fetchedAt >= CHARTS_TTL_MS
    }

    private fun persist(snapshot: ChartsSnapshot, fetchedAt: Long) {
        try {
            cacheSettings.putString(KEY_SNAPSHOT, json.encodeToString(snapshot))
            cacheSettings.putLong(KEY_FETCHED_AT, fetchedAt)
        } catch (_: Exception) {
        }
    }

    private suspend fun fetchRemote(): ChartsSnapshot? = try {
        val client = httpClient ?: return null
        val response = client.get(documentUrl) {
            header("X-Appwrite-Project", AppwriteConfig.PROJECT_ID)
        }
        if (!response.status.isSuccess()) return null
        parseChartsPayload(response.bodyAsText())
    } catch (_: Exception) {
        null
    }

    /**
     * Parses one Appwrite get-document body for `charts/global`.
     *
     * Field names are camelCase (the live schema); snake_case is accepted as
     * a fallback. Tolerant by design: missing arrays decode to empty lists,
     * non-string entries are skipped, and a missing/unparseable `generatedAt`
     * decodes to 0 — so a half-shaped admin doc still yields a usable
     * snapshot. `generatedAt` may be epoch millis (number or numeric string)
     * or an ISO-8601 datetime string (Appwrite datetime attributes).
     * Returns null only when the body is not a charts document at all (error
     * envelope, `"{}"`, truncated text): an Appwrite get-document answer
     * always carries `$id`, so its absence marks the failure path, which
     * keeps the cached-or-null snapshot instead of adopting garbage. Visible
     * for tests.
     */
    internal fun parseChartsPayload(body: String): ChartsSnapshot? = try {
        val obj = json.parseToJsonElement(body).jsonObject
        obj["\$id"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null
        ChartsSnapshot(
            topQaris7d = slugList(obj, "topQaris7d", "top_qaris_7d"),
            topQaris30d = slugList(obj, "topQaris30d", "top_qaris_30d"),
            trending = slugList(obj, "trending"),
            generatedAt = parseGeneratedAt(obj),
        )
    } catch (_: Exception) {
        null
    }

    private fun slugList(obj: JsonObject, vararg keys: String): List<String> = try {
        val key = keys.firstOrNull { obj.containsKey(it) } ?: return emptyList()
        obj[key]?.jsonArray?.mapNotNull { element ->
            (element as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.orEmpty()
    } catch (_: Exception) {
        emptyList()
    }

    private fun parseGeneratedAt(obj: JsonObject): Long {
        val prim = (obj["generatedAt"] ?: obj["generated_at"]) as? JsonPrimitive
            ?: return 0L
        return prim.asLongOrNull() ?: parseIso8601(prim.content) ?: 0L
    }

    /**
     * Minimal ISO-8601 parser (no kotlinx-datetime dependency):
     * `yyyy-MM-ddTHH:mm:ss[.SSS][Z|±HH:mm|±HHMM|±HH]`.
     */
    private fun parseIso8601(text: String): Long? = try {
        val m = ISO_8601.matchEntire(text.trim()) ?: return null
        val year = m.groupValues[1].toInt()
        val month = m.groupValues[2].toInt()
        val day = m.groupValues[3].toInt()
        if (month !in 1..12 || day !in 1..31) return null
        val hour = m.groupValues[4].toInt()
        val minute = m.groupValues[5].toInt()
        val second = m.groupValues[6].toInt()
        val frac = m.groupValues[7]
        val millis = when {
            frac.isEmpty() -> 0
            else -> frac.take(3).padEnd(3, '0').toInt()
        }
        // Days-from-civil (Howard Hinnant algorithm).
        var y = year
        val mp = (month + 9) % 12
        y -= mp / 10
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val days = era * 146097 + doe - 719468
        val zone = m.groupValues[8]
        val offsetMinutes = when {
            zone.isEmpty() || zone == "Z" -> 0
            else -> {
                val sign = if (zone.startsWith("-")) -1 else 1
                val digits = zone.drop(1).filter { it.isDigit() }
                val hh = digits.take(2).toInt()
                val mm = digits.drop(2).take(2).toIntOrNull() ?: 0
                sign * (hh * 60 + mm)
            }
        }
        ((days * 24L + hour) * 60 + minute) * 60_000L + second * 1000L + millis -
            offsetMinutes * 60_000L
    } catch (_: Exception) {
        null
    }

    private fun JsonPrimitive.asLongOrNull(): Long? {
        val text = content.trim()
        return longOrNull ?: text.toLongOrNull() ?: text.toDoubleOrNull()?.toLong()
    }

    private val ISO_8601 =
        Regex("""^(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?\s*(Z|[+-]\d{2}:?\d{2})?$""")

    /**
     * Process-local [Settings] used by [cacheSettings] when the platform's
     * no-arg `Settings()` factory cannot build a store (no Context on the JVM
     * unit-test classpath). Same shape as the fallback in
     * [QuranAyahRepository]: data dies with the process, but the persistence
     * logic runs unmodified on every platform instead of silently no-oping,
     * which is what makes it testable.
     */
    private class InMemorySettings : Settings {
        private val values = mutableMapOf<String, Any?>()

        override val keys: Set<String> get() = values.keys
        override val size: Int get() = values.size

        override fun clear() {
            values.clear()
        }

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun hasKey(key: String): Boolean = values.containsKey(key)

        override fun putInt(key: String, value: Int) {
            values[key] = value
        }

        override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = values[key] as? Int

        override fun putLong(key: String, value: Long) {
            values[key] = value
        }

        override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = values[key] as? Long

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun getString(key: String, defaultValue: String): String =
            values[key] as? String ?: defaultValue

        override fun getStringOrNull(key: String): String? = values[key] as? String

        override fun putFloat(key: String, value: Float) {
            values[key] = value
        }

        override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = values[key] as? Float

        override fun putDouble(key: String, value: Double) {
            values[key] = value
        }

        override fun getDouble(key: String, defaultValue: Double): Double =
            values[key] as? Double ?: defaultValue

        override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            values[key] as? Boolean ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
    }
}
