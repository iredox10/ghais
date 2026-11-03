package com.ghais.data.backend

import com.ghais.data.auth.BackendConfig
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class TokenBundle(
    val userId: String, val email: String, val name: String,
    val accessToken: String, val refreshToken: String, val expiresIn: Long = 900,
)

class BackendException(val code: Int, message: String) : Exception(message)

/** One transport for auth and documents. Tokens are sent only to our API. */
object BackendHttp {
    private val json = Json { ignoreUnknownKeys = true }
    private val settings by lazy { Settings() }
    private const val TOKEN_KEY = "ghais_render_token_bundle"
    private val refreshMutex = Mutex()
    private val _tokens = MutableStateFlow<TokenBundle?>(null)
    val tokens = _tokens.asStateFlow()
    private var loaded = false
    private val defaultClient by lazy { HttpClient() }
    private var testClient: HttpClient? = null
    private var testPersistence: ((TokenBundle?) -> Unit)? = null
    internal var onSessionExpired: (() -> Unit)? = null
    private val client get() = testClient ?: defaultClient

    fun init() {
        if (loaded) return
        loaded = true
        _tokens.value = try {
            settings.getStringOrNull(TOKEN_KEY)?.let { json.decodeFromString<TokenBundle>(it) }
        } catch (_: Exception) { null }
    }

    fun adopt(pair: TokenBundle?) {
        val persistence = testPersistence
        if (persistence != null) persistence(pair)
        else if (pair == null) settings.remove(TOKEN_KEY)
        else settings.putString(TOKEN_KEY, json.encodeToString(pair))
        _tokens.value = pair
    }

    private suspend fun send(method: HttpMethod, path: String, body: JsonElement?, access: String?, queries: List<String>) =
        client.request(BackendConfig.ENDPOINT + path) {
            this.method = method
            if (access != null) bearerAuth(access)
            queries.forEach { parameter("queries[]", it) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }

    suspend fun request(method: HttpMethod, path: String, body: JsonElement? = null,
                        authenticated: Boolean = true, queries: List<String> = emptyList()): String {
        init()
        val before = if (authenticated) _tokens.value else null
        var response = send(method, path, body, before?.accessToken, queries)
        if (response.status.value == 401 && before != null && refresh(before)) {
            // Do not retry a request under a different signed-in account.
            val current = _tokens.value
            if (current?.userId == before.userId) response = send(method, path, body, current.accessToken, queries)
        }
        val text = response.bodyAsText()
        if (response.status.value !in 200..299) throw BackendException(response.status.value, "Request failed (${response.status.value}).")
        return text
    }

    private suspend fun refresh(failed: TokenBundle): Boolean = refreshMutex.withLock {
        val current = _tokens.value ?: return@withLock false
        if (current.userId != failed.userId) return@withLock false
        if (current.accessToken != failed.accessToken) return@withLock true
        val response = send(HttpMethod.Post, "/auth/refresh", buildJsonObject {
            put("refreshToken", current.refreshToken)
        }, null, emptyList())
        val text = response.bodyAsText()
        // Logout/account switch during the network call must never be undone.
        if (_tokens.value != current) return@withLock false
        if (response.status.value == 401) {
            adopt(null)
            onSessionExpired?.invoke()
            return@withLock false
        }
        if (response.status.value !in 200..299) throw BackendException(response.status.value, "Session refresh is temporarily unavailable.")
        adopt(json.decodeFromString<TokenBundle>(text))
        true
    }

    internal fun configureForTest(client: HttpClient?, pair: TokenBundle? = null) {
        testClient = client
        testPersistence = if (client != null) ({ _: TokenBundle? -> }) else null
        _tokens.value = pair
        loaded = client != null
        onSessionExpired = null
    }

    suspend fun authenticate(path: String, fields: Map<String, String>): TokenBundle =
        json.decodeFromString(request(HttpMethod.Post, path, JsonObject(fields.mapValues { JsonPrimitive(it.value) }), authenticated = false))
}

/** Platform integration required only for native Google UI and app metadata. */
expect object BackendPlatform {
    val apiEndpoint: String
    val name: String
    fun appVersion(): String
    suspend fun googleIdToken(): String
}

internal object SyncLog {
    fun i(tag: String, message: String) { println("$tag: $message") }
    fun w(tag: String, message: String, error: Throwable? = null) { println("$tag: $message ${error?.message.orEmpty()}") }
    fun e(tag: String, message: String, error: Throwable? = null) = w(tag, message, error)
}
