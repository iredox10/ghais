package com.ghais.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Data model representing commentary / tafseer for a specific Ayah.
 */
@Serializable
data class AyahTafseer(
    val surahId: Int,
    val ayahNo: Int,
    val editionName: String,
    val author: String,
    val text: String
)

/**
 * Repository providing access to Quranic Tafseer (commentary / exegesis).
 *
 * Features:
 * - Bundled commentary for key Surahs (Al-Fatihah, Al-Kahf 1-5, Al-Kawthar, Al-Ikhlas, Al-Falaq, An-Nas).
 * - Live fetcher from AlQuran Cloud (`https://api.alquran.cloud/v1/ayah/{surah}:{ayah}/editions/en.asad`)
 *   for all other Surahs and Ayahs.
 * - Reactive and thread-safe in-memory cache backed by [MutableStateFlow].
 * - In-flight deduplication via [Mutex].
 * - Synchronous immediate access via [getTafseerImmediate] for instantaneous UI rendering.
 */
object QuranTafseerRepository {

    private const val EDITION_IDENTIFIER = "en.asad"
    private const val DEFAULT_AUTHOR = "Muhammad Asad"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var customHttpClient: HttpClient? = null

    private val defaultHttpClient: HttpClient by lazy {
        HttpClient()
    }

    private val client: HttpClient
        get() = customHttpClient ?: defaultHttpClient

    /**
     * Optional hook to inject a configured [HttpClient] (e.g., custom timeout, OkHttp engine, or test mock).
     */
    fun configureHttpClient(client: HttpClient) {
        customHttpClient = client
    }

    /**
     * Cache key format: "{surahId}:{ayahNo}"
     */
    private fun cacheKey(surahId: Int, ayahNo: Int): String = "$surahId:$ayahNo"

    /**
     * Bundled commentary for key Surahs.
     */
    private val BUNDLED_TAFSEER: Map<String, AyahTafseer> = buildMap {
        fun add(surahId: Int, ayahNo: Int, text: String) {
            put(
                cacheKey(surahId, ayahNo),
                AyahTafseer(
                    surahId = surahId,
                    ayahNo = ayahNo,
                    editionName = EDITION_IDENTIFIER,
                    author = DEFAULT_AUTHOR,
                    text = text
                )
            )
        }

        // --- Surah 1: Al-Fatihah (The Opening) ---
        add(1, 1, "In the name of God, The Most Gracious, The Dispenser of Grace:")
        add(1, 2, "All praise is due to God alone, the Sustainer of all the worlds,")
        add(1, 3, "The Most Gracious, the Dispenser of Grace,")
        add(1, 4, "Lord of the Day of Judgment!")
        add(1, 5, "Thee alone do we worship; and unto Thee alone do we turn for aid.")
        add(1, 6, "Guide us the straight way.")
        add(1, 7, "The way of those upon whom Thou hast bestowed Thy blessings, not of those who have been condemned [by Thee], nor of those who go astray!")

        // --- Surah 18: Al-Kahf (The Cave) - Verses 1 to 5 ---
        add(18, 1, "ALL PRAISE is due to God, who has bestowed this divine writ from on high upon His servant, and has not allowed any deviousness to obscure its meaning:")
        add(18, 2, "[a divine writ] unerringly straight, meant to warn [the godless] of a severe punishment from Him, and to give unto the believers who do good works the glad tiding that theirs shall be a goodly reward-")
        add(18, 3, "[a state of bliss] in which they shall dwell beyond the count of time.")
        add(18, 4, "Furthermore, [this divine writ is meant] to warn all those who assert, \"God has taken unto Himself a son.\"")
        add(18, 5, "No knowledge whatever have they of Him, and neither had their forefathers: dreadful is this saying that comes out of their mouths, [and] nothing but falsehood do they utter!")

        // --- Surah 108: Al-Kawthar (Good in Abundance) ---
        add(108, 1, "BEHOLD, We have bestowed upon thee good in abundance:")
        add(108, 2, "hence, pray unto thy Sustainer [alone], and sacrifice [unto Him alone].")
        add(108, 3, "Verily, he that hates thee has indeed been cut off [from all that is good]!")

        // --- Surah 112: Al-Ikhlas (The Declaration of God's Perfection) ---
        add(112, 1, "SAY: \"He is the One God:")
        add(112, 2, "\"God the Eternal, the Uncaused Cause of All Being.")
        add(112, 3, "\"He begets not, and neither is He begotten;")
        add(112, 4, "\"and there is nothing that could be compared with Him.\"")

        // --- Surah 113: Al-Falaq (The Rising Dawn) ---
        add(113, 1, "SAY: \"I seek refuge with the Sustainer of the rising dawn,")
        add(113, 2, "\"from the evil of aught that He has created,")
        add(113, 3, "\"and from the evil of the black darkness whenever it descends,")
        add(113, 4, "\"and from the evil of all human beings bent on occult endeavours,")
        add(113, 5, "\"and from the evil of the envious when he envies.\"")

        // --- Surah 114: An-Nas (Mankind) ---
        add(114, 1, "SAY: \"I seek refuge with the Sustainer of men,")
        add(114, 2, "\"the Sovereign of men,")
        add(114, 3, "\"the God of men,")
        add(114, 4, "\"from the evil of the whispering, elusive tempter")
        add(114, 5, "\"who whispers in the hearts of men")
        add(114, 6, "\"from all [temptation to evil by] invisible forces as well as men,\"")
    }

    // Thread-safe in-memory cache pre-populated with bundled commentary.
    private val _cache = MutableStateFlow<Map<String, AyahTafseer>>(BUNDLED_TAFSEER)

    // Mutex to serialize remote fetches and prevent duplicate concurrent requests.
    private val fetchMutex = Mutex()

    /**
     * Synchronously returns the tafseer if it is bundled or already in the in-memory cache.
     * Returns `null` if not yet loaded in memory.
     */
    fun getTafseerImmediate(surahId: Int, ayahNo: Int): AyahTafseer? {
        val key = cacheKey(surahId, ayahNo)
        return _cache.value[key]
    }

    /**
     * Returns the tafseer for the given surah and ayah.
     *
     * 1. If present in memory (bundled or previously fetched), returns immediately.
     * 2. Otherwise, fetches online from AlQuran Cloud (`en.asad`), parses the response,
     *    stores it into the in-memory cache, and returns it.
     */
    suspend fun getTafseer(surahId: Int, ayahNo: Int): AyahTafseer {
        val key = cacheKey(surahId, ayahNo)

        // Fast path: lock-free cache read
        _cache.value[key]?.let { return it }

        // Slow path: synchronize to ensure only 1 network fetch per ayah
        return fetchMutex.withLock {
            // Re-check after obtaining lock
            _cache.value[key]?.let { return it }

            val fetched = fetchRemoteTafseer(surahId, ayahNo)
            _cache.update { current -> current + (key to fetched) }
            fetched
        }
    }

    /**
     * Fetches commentary text from `https://api.alquran.cloud/v1/ayah/{surah}:{ayah}/editions/en.asad`.
     */
    private suspend fun fetchRemoteTafseer(surahId: Int, ayahNo: Int): AyahTafseer {
        val url = "https://api.alquran.cloud/v1/ayah/$surahId:$ayahNo/editions/$EDITION_IDENTIFIER"

        val response = try {
            client.get(url)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to connect to Tafseer API for Ayah $surahId:$ayahNo: ${e.message}", e)
        }

        if (response.status.value !in 200..299) {
            throw IllegalStateException("Tafseer API returned HTTP ${response.status.value} for Ayah $surahId:$ayahNo")
        }

        val bodyText = response.bodyAsText()
        return parseCloudResponse(surahId, ayahNo, bodyText)
    }

    /**
     * Robust parser handling AlQuran Cloud response formats.
     */
    private fun parseCloudResponse(surahId: Int, ayahNo: Int, jsonString: String): AyahTafseer {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val code = root["code"]?.jsonPrimitive?.intOrNull ?: 200
        if (code != 200) {
            val status = root["status"]?.jsonPrimitive?.contentOrNull ?: "Error"
            throw IllegalStateException("Tafseer API returned error $code ($status) for Ayah $surahId:$ayahNo")
        }

        val dataElement = root["data"]
            ?: throw IllegalStateException("Missing 'data' element in Tafseer API response for Ayah $surahId:$ayahNo")

        val ayahObject = when (dataElement) {
            is JsonArray -> {
                if (dataElement.isEmpty()) {
                    throw NoSuchElementException("Empty data list in Tafseer response for Ayah $surahId:$ayahNo")
                }
                dataElement[0].jsonObject
            }
            is JsonObject -> dataElement
            else -> throw IllegalStateException("Unexpected 'data' format in Tafseer response for Ayah $surahId:$ayahNo")
        }

        val text = ayahObject["text"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Missing 'text' in Tafseer response for Ayah $surahId:$ayahNo")

        val editionObj = ayahObject["edition"]?.jsonObject
        val editionName = editionObj?.get("identifier")?.jsonPrimitive?.content
            ?: editionObj?.get("name")?.jsonPrimitive?.content
            ?: EDITION_IDENTIFIER
        val author = editionObj?.get("englishName")?.jsonPrimitive?.content
            ?: DEFAULT_AUTHOR

        return AyahTafseer(
            surahId = surahId,
            ayahNo = ayahNo,
            editionName = editionName,
            author = author,
            text = text
        )
    }

    /**
     * Checks if the tafseer for the specified Ayah is currently present in memory.
     */
    fun isCached(surahId: Int, ayahNo: Int): Boolean {
        return _cache.value.containsKey(cacheKey(surahId, ayahNo))
    }

    /**
     * Checks if the specified Ayah is part of the pre-bundled offline commentary.
     */
    fun isBundled(surahId: Int, ayahNo: Int): Boolean {
        return BUNDLED_TAFSEER.containsKey(cacheKey(surahId, ayahNo))
    }

    /**
     * Manually injects or overrides a tafseer entry in memory.
     */
    fun putInCache(tafseer: AyahTafseer) {
        _cache.update { it + (cacheKey(tafseer.surahId, tafseer.ayahNo) to tafseer) }
    }

    /**
     * Clears cached online entries, optionally preserving the bundled commentary.
     */
    fun clearCache(keepBundled: Boolean = true) {
        _cache.value = if (keepBundled) BUNDLED_TAFSEER else emptyMap()
    }
}
