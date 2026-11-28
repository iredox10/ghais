package com.ghais.data.auth

import com.ghais.data.backend.BackendException
import com.ghais.data.backend.BackendHttp
import com.ghais.data.backend.TokenBundle
import com.ghais.data.repository.UserUsageRepository
import com.russhwolf.settings.Settings
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random

/**
 * Silent, device-scoped guest session used solely to upload telemetry,
 * play events, listening stats, history, and analytics when the user has
 * not signed up or logged in.
 *
 * Completely isolated from [AuthRepository.session] and [BackendHttp.tokens]
 * so the app UI, Profile screen, and pre-auth account adoption continue to
 * treat the user as signed out.
 */
object GuestTelemetrySession {
    private const val STORE_KEY = "ghais_guest_telemetry_auth_v1"
    private const val GUEST_DOMAIN = "guest.ghaith.app"

    @Serializable
    private data class StoredGuestAuth(
        val email: String,
        val password: String,
        val tokens: TokenBundle? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val settings: Settings by lazy { Settings() }
    private val mutex = Mutex()
    private var cached: StoredGuestAuth? = null

    private fun randomPassword(): String =
        Random.nextBytes(16).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

    private fun loadOrCreateAuth(): StoredGuestAuth {
        cached?.let { return it }
        val loaded = try {
            settings.getStringOrNull(STORE_KEY)?.let { json.decodeFromString<StoredGuestAuth>(it) }
        } catch (_: Exception) {
            null
        }
        if (loaded != null && loaded.email.isNotBlank() && loaded.password.isNotBlank()) {
            cached = loaded
            return loaded
        }
        val devId = UserUsageRepository.deviceId()
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_-]"), "")
            .ifBlank { randomPassword().take(12) }
        val fresh = StoredGuestAuth(
            email = "guest-$devId@$GUEST_DOMAIN",
            password = randomPassword(),
        )
        saveAuth(fresh)
        return fresh
    }

    private fun saveAuth(auth: StoredGuestAuth) {
        cached = auth
        try {
            settings.putString(STORE_KEY, json.encodeToString(auth))
        } catch (_: Exception) {
        }
    }

    /**
     * Returns a valid guest [TokenBundle] when no real user is signed in,
     * or `null` if a real account is active or authentication is unavailable.
     */
    suspend fun ensureSession(forceRefresh: Boolean = false): TokenBundle? = mutex.withLock {
        if (AuthRepository.session.value != null) return@withLock null
        if (!BackendConfig.isConfigured()) return@withLock null

        val auth = loadOrCreateAuth()
        val existing = auth.tokens
        if (existing != null && !forceRefresh) return@withLock existing

        if (existing != null && forceRefresh && existing.refreshToken.isNotBlank()) {
            val refreshed = tryRefresh(existing.refreshToken)
            if (refreshed != null) {
                saveAuth(auth.copy(tokens = refreshed))
                return@withLock refreshed
            }
        }

        val bundle = tryRegisterOrLogin(auth.email, auth.password) ?: return@withLock null
        saveAuth(auth.copy(tokens = bundle))
        bundle
    }

    private suspend fun tryRefresh(refreshToken: String): TokenBundle? = try {
        val text = BackendHttp.request(
            method = HttpMethod.Post,
            path = "/auth/refresh",
            body = buildJsonObject { put("refreshToken", refreshToken) },
            authenticated = false,
        )
        json.decodeFromString<TokenBundle>(text)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun tryRegisterOrLogin(email: String, password: String): TokenBundle? = try {
        BackendHttp.authenticate(
            "/auth/register",
            mapOf("email" to email, "name" to "Guest Listener", "password" to password),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: BackendException) {
        if (e.code == 409) {
            try {
                BackendHttp.authenticate("/auth/login", mapOf("email" to email, "password" to password))
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
