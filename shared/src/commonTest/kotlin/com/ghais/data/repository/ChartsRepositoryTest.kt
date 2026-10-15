package com.ghais.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class ChartsRepositoryTest {

    private lateinit var engine: FakeChartsEngine

    // ------------------------------------------------------------------
    // Singleton normalisation
    //
    // ChartsRepository is an `object`: the in-memory snapshot, the injected
    // HTTP client, the test clock and the `charts_` disk keys all outlive a
    // single test. Reset through the internal test hook first, then inject
    // the fake — the same setup/teardown shape as QuranAyahRepositoryTest.
    // ------------------------------------------------------------------

    @BeforeTest
    fun setUpChartsRepository() {
        ChartsRepository.resetForTest()
        engine = FakeChartsEngine()
        ChartsRepository.configureHttpClient(HttpClient(engine))
    }

    @AfterTest
    fun tearDownChartsRepository() {
        ChartsRepository.resetForTest()
    }

    @Test
    fun testParsesFakeChartsPayload() = runBlocking {
        ChartsRepository.refresh()

        val snap = assertNotNull(
            ChartsRepository.snapshot.value,
            "refresh did not adopt the fake charts/global doc",
        )
        assertEquals(listOf("mishary", "al-sudais"), snap.topQaris7d)
        assertEquals(listOf("husary"), snap.topQaris30d)
        assertEquals(listOf("minshawi", "abdul-basit"), snap.trending)
        assertEquals(1_759_500_000_000L, snap.generatedAt)
    }

    @Test
    fun testYesterdayWindowParsesAndOldPayloadDefaultsEmpty() = runBlocking {
        ChartsRepository.resetForTest()
        ChartsRepository.configureHttpClient(HttpClient(engine))
        engine.payload = """{
          "${'$'}id": "global",
          "topQarisYesterday": ["husary", "mishary"],
          "generated_at": 1759500000000
        }"""
        ChartsRepository.refresh()
        val snap = assertNotNull(ChartsRepository.snapshot.value)
        assertEquals(listOf("husary", "mishary"), snap.topQarisYesterday)

        // Old server payloads without the window decode to empty, not null.
        ChartsRepository.resetForTest()
        ChartsRepository.configureHttpClient(HttpClient(engine))
        engine.payload = CHARTS_PAYLOAD
        ChartsRepository.refresh()
        assertEquals(
            emptyList(),
            assertNotNull(ChartsRepository.snapshot.value).topQarisYesterday,
        )
    }

    @Test
    fun testRefreshHitsChartsGlobalDocument() = runBlocking {
        ChartsRepository.refresh()

        assertEquals(1, engine.urls.size, "expected exactly one charts fetch, got: ${engine.urls}")
        val url = engine.urls.single()
        assertTrue(
            url.contains("databases/ghais/collections/charts/documents/global"),
            "fetch did not target charts/global: $url",
        )
    }

    @Test
    fun testFreshCacheServedWithoutNetworkHit() = runBlocking {
        ChartsRepository.refresh()
        assertEquals(1, engine.urls.size, "setup: expected one fetch, got: ${engine.urls}")

        // A second refresh inside the 12h TTL must serve the disk cache and
        // never touch the network — even though the server doc changed.
        engine.payload = CHARTS_PAYLOAD_V2
        ChartsRepository.refresh()

        assertEquals(1, engine.urls.size, "fresh cache caused a network hit")
        assertEquals(
            listOf("mishary", "al-sudais"),
            assertNotNull(ChartsRepository.snapshot.value).topQaris7d,
            "fresh refresh adopted un-fetched data",
        )
    }

    @Test
    fun testStaleCacheTriggersRefetch() = runBlocking {
        ChartsRepository.refresh()
        assertEquals(1, engine.urls.size, "setup: expected one fetch, got: ${engine.urls}")

        // 12h later the cache is stale: refresh must go back to the network
        // and adopt the new doc.
        val fetchedAt = ChartsRepository.nowMs()
        ChartsRepository.nowMs = { fetchedAt + ChartsRepository.CHARTS_TTL_MS + 1 }
        engine.payload = CHARTS_PAYLOAD_V2
        ChartsRepository.refresh()

        assertEquals(2, engine.urls.size, "stale cache did not trigger a refetch")
        val snap = assertNotNull(ChartsRepository.snapshot.value)
        assertEquals(listOf("al-minshawi"), snap.topQaris7d)
        assertEquals(listOf("abdullah-awad-al-juhany"), snap.trending)
    }

    @Test
    fun testFailureKeepsCachedSnapshotWithoutThrowing() = runBlocking {
        ChartsRepository.refresh()
        val seeded = assertNotNull(
            ChartsRepository.snapshot.value,
            "setup: fake charts/global doc was not adopted",
        )

        // The network dies after the TTL expired: refresh must keep the
        // last-good snapshot instead of throwing or blanking.
        val fetchedAt = ChartsRepository.nowMs()
        ChartsRepository.nowMs = { fetchedAt + ChartsRepository.CHARTS_TTL_MS + 60_000 }
        ChartsRepository.configureHttpClient(HttpClient(FakeChartsEngine(failing = true)))
        ChartsRepository.refresh()

        assertEquals(seeded, ChartsRepository.snapshot.value, "failed refresh dropped the cache")
    }

    @Test
    fun testFailureWithEmptyCacheYieldsNullWithoutThrowing() = runBlocking {
        ChartsRepository.configureHttpClient(HttpClient(FakeChartsEngine(failing = true)))
        ChartsRepository.refresh()

        assertNull(ChartsRepository.snapshot.value, "offline + never-fetched must stay null")
    }

    @Test
    fun testEmptyListsHandled() = runBlocking {
        engine.payload = CHARTS_PAYLOAD_EMPTY
        ChartsRepository.refresh()

        val snap = assertNotNull(
            ChartsRepository.snapshot.value,
            "valid-but-empty charts doc was not adopted",
        )
        assertTrue(snap.topQaris7d.isEmpty(), "topQaris7d not empty: ${snap.topQaris7d}")
        assertTrue(snap.topQaris30d.isEmpty(), "topQaris30d not empty: ${snap.topQaris30d}")
        assertTrue(snap.trending.isEmpty(), "trending not empty: ${snap.trending}")
        assertEquals(0L, snap.generatedAt)
    }

    @Test
    fun testNonStringEntriesAreSkipped() = runBlocking {
        engine.payload = CHARTS_PAYLOAD_MIXED
        ChartsRepository.refresh()

        val snap = assertNotNull(
            ChartsRepository.snapshot.value,
            "mixed-type charts doc was not adopted",
        )
        assertEquals(listOf("mishary", "husary"), snap.topQaris7d)
        assertEquals(1_759_500_000_000L, snap.generatedAt)
    }

    @Test
    fun testLiveServerShapeCamelCaseAndIsoDatetime() = runBlocking {
        engine.payload = CHARTS_PAYLOAD_LIVE_SHAPE
        ChartsRepository.refresh()

        val snap = assertNotNull(
            ChartsRepository.snapshot.value,
            "live-shape charts doc was not adopted",
        )
        assertEquals(listOf("mishary", "husary"), snap.topQaris7d)
        assertEquals(listOf("al-minshawi"), snap.topQaris30d)
        assertEquals(listOf("husary"), snap.trending)
        // 2026-10-08T10:00:00.000+00:00
        assertEquals(1_791_453_600_000L, snap.generatedAt)
    }
}

/**
 * In-process stand-in for the Appwrite `charts/global` document endpoint.
 *
 * Mirrors [FakeAlQuranEngine]: with [failing] it answers with a body that
 * carries no `$id`, which is exactly how an unreachable/unprovisioned charts
 * endpoint presents itself to `parseChartsPayload`, and it never opens a
 * socket. Otherwise it serves [payload]. Every URL is recorded so a test can
 * prove a refresh did (or did not) hit the network.
 */
@OptIn(InternalAPI::class)
private class FakeChartsEngine(
    var payload: String = CHARTS_PAYLOAD,
    private val failing: Boolean = false,
) : HttpClientEngineBase("fake-charts") {

    override val config: HttpClientEngineConfig = HttpClientEngineConfig()

    private val recordedUrls = MutableStateFlow<List<String>>(emptyList())

    /** Requested URLs, in order, exactly as the repository spelled them. */
    val urls: List<String> get() = recordedUrls.value

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val url = data.url.toString()
        recordedUrls.update { it + url }
        return HttpResponseData(
            HttpStatusCode.OK,
            GMTDate(),
            headersOf(HttpHeaders.ContentType, listOf("application/json; charset=utf-8")),
            HttpProtocolVersion.HTTP_1_1,
            ByteReadChannel(if (failing) "{}" else payload),
            callContext(),
        )
    }
}

private const val CHARTS_PAYLOAD = """{
  "${'$'}id": "global",
  "${'$'}collectionId": "charts",
  "${'$'}databaseId": "ghais",
  "top_qaris_7d": ["mishary", "al-sudais"],
  "top_qaris_30d": ["husary"],
  "trending": ["minshawi", "abdul-basit"],
  "generated_at": 1759500000000
}"""

private const val CHARTS_PAYLOAD_V2 = """{
  "${'$'}id": "global",
  "${'$'}collectionId": "charts",
  "${'$'}databaseId": "ghais",
  "top_qaris_7d": ["al-minshawi"],
  "top_qaris_30d": ["husary"],
  "trending": ["abdullah-awad-al-juhany"],
  "generated_at": 1759586400000
}"""

private const val CHARTS_PAYLOAD_EMPTY = """{
  "${'$'}id": "global",
  "${'$'}collectionId": "charts",
  "${'$'}databaseId": "ghais",
  "top_qaris_7d": [],
  "top_qaris_30d": [],
  "trending": [],
  "generated_at": 0
}"""

private const val CHARTS_PAYLOAD_MIXED = """{
  "${'$'}id": "global",
  "top_qaris_7d": ["mishary", 42, null, "   ", true, "husary"],
  "generated_at": "1759500000000"
}"""

/** Exact shape the Appwrite REST API returns: camelCase keys + ISO-8601 datetime. */
private const val CHARTS_PAYLOAD_LIVE_SHAPE = """{
  "${'$'}id": "global",
  "${'$'}collectionId": "charts",
  "${'$'}databaseId": "ghais",
  "topQaris7d": ["mishary", "husary"],
  "topQaris30d": ["al-minshawi"],
  "trending": ["husary"],
  "generatedAt": "2026-10-08T10:00:00.000+00:00"
}"""
