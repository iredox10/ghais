package com.ghais.data.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.*
import kotlin.test.*

class RenderMigrationTest {
    private var client: HttpClient? = null
    private val old = TokenBundle("alice", "a@example.com", "Alice", "old", "refresh-old")
    private val fresh = """{"userId":"alice","email":"a@example.com","name":"Alice","accessToken":"new","refreshToken":"refresh-new","expiresIn":900}"""
    @AfterTest fun cleanup() { client?.close(); BackendHttp.configureForTest(null) }
    private fun configure(handler: suspend (HttpRequestData) -> Pair<Int, String>) {
        client = HttpClient(FakeBackendEngine(handler))
        BackendHttp.configureForTest(client, old)
    }

    @Test fun concurrentUnauthorizedRequestsRotateOnce() = runBlocking {
        var refreshes = 0
        var oldRequests = 0
        val bothFailed = CompletableDeferred<Unit>()
        configure { req ->
            when {
                req.url.encodedPath.endsWith("/auth/refresh") -> { refreshes++; 200 to fresh }
                req.headers[HttpHeaders.Authorization] == "Bearer old" -> {
                    oldRequests++
                    if (oldRequests == 2) bothFailed.complete(Unit)
                    bothFailed.await()
                    401 to "{}"
                }
                else -> { assertEquals("Bearer new", req.headers[HttpHeaders.Authorization]); 200 to "{}" }
            }
        }
        coroutineScope {
            listOf(async { BackendHttp.request(HttpMethod.Get, "/auth/me") },
                async { BackendHttp.request(HttpMethod.Get, "/auth/me") }).awaitAll()
        }
        assertEquals(1, refreshes)
        assertEquals("refresh-new", BackendHttp.tokens.value?.refreshToken)
    }

    @Test fun logoutDuringRefreshCannotRestoreTheSession() = runBlocking {
        configure { req ->
            if (req.url.encodedPath.endsWith("/auth/refresh")) {
                BackendHttp.adopt(null)
                200 to fresh
            } else 401 to "{}"
        }
        try { BackendHttp.request(HttpMethod.Get, "/auth/me"); fail("Expected unauthorized") }
        catch (e: BackendException) { assertEquals(401, e.code) }
        assertNull(BackendHttp.tokens.value)
    }

    @Test fun rejectedRefreshClearsIdentityButServerFailureKeepsOfflineIdentity() = runBlocking {
        for (status in listOf(401, 503)) {
            configure { req -> if (req.url.encodedPath.endsWith("/auth/refresh")) status to "{}" else 401 to "{}" }
            try { BackendHttp.request(HttpMethod.Get, "/auth/me"); fail("Expected unauthorized") }
            catch (e: BackendException) { assertEquals(status, e.code) }
            if (status == 401) assertNull(BackendHttp.tokens.value) else assertEquals(old, BackendHttp.tokens.value)
            client?.close()
        }
    }

    @Test fun documentsPreserveNumbersAndBroadcastTimestamp() = runBlocking {
        configure { req ->
            assertEquals(Url(com.ghais.data.auth.BackendConfig.ENDPOINT).host, req.url.host)
            assertEquals("Bearer old", req.headers[HttpHeaders.Authorization])
            200 to """{"${'$'}id":"global","${'$'}createdAt":"2026-10-27T00:00:00Z","seconds":9007199254740991,"visible":true,"items":["a","b"]}"""
        }
        val doc = Databases().getDocument("ghais", "broadcasts", "global")
        assertEquals(9007199254740991L, doc.data["seconds"])
        assertEquals(true, doc.data["visible"])
        assertEquals(listOf("a","b"), doc.data["items"])
        assertEquals("2026-10-27T00:00:00Z", doc.createdAt)
    }

    @Test fun documentHashMatchesExistingAndroidIds() {
        fun hex(text: String) = sha256(text.encodeToByteArray()).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex(""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex("abc"))
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0", hex("a".repeat(1_000_000)))
    }
}

@OptIn(InternalAPI::class)
private class FakeBackendEngine(val handler: suspend (HttpRequestData) -> Pair<Int,String>) : HttpClientEngineBase("render-test") {
    override val config = HttpClientEngineConfig()
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val (status, body) = handler(data)
        return HttpResponseData(HttpStatusCode.fromValue(status), GMTDate(),
            headersOf(HttpHeaders.ContentType, "application/json"), HttpProtocolVersion.HTTP_1_1,
            ByteReadChannel(body), callContext())
    }
}
