package com.ghais.data.auth

import com.ghais.data.backend.BackendException
import com.ghais.data.backend.BackendHttp
import com.ghais.data.backend.BackendPlatform
import com.ghais.data.backend.TokenBundle
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

internal class AuthUserException(message: String) : Exception(message)

data class AuthSession(val userId: String, val email: String, val name: String)

/** Shared Render authentication, including persistent offline identity. */
object AuthRepository {
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session = _session.asStateFlow()
    private val _cachedSession = MutableStateFlow<AuthSession?>(null)
    val cachedSession = _cachedSession.asStateFlow()
    private val _authChecked = MutableStateFlow(false)
    val authChecked = _authChecked.asStateFlow()

    fun init() {
        BackendHttp.onSessionExpired = {
            _session.value = null
            _cachedSession.value = null
        }
        BackendHttp.init()
        _cachedSession.value = BackendHttp.tokens.value?.asSession()
    }

    private fun TokenBundle.asSession() = AuthSession(userId, email, name)
    private suspend fun authenticate(path: String, fields: Map<String, String>): Result<Unit> = safe {
        val pair = BackendHttp.authenticate(path, fields)
        BackendHttp.adopt(pair)
        _cachedSession.value = pair.asSession()
        _session.value = pair.asSession()
        _authChecked.value = true
    }

    suspend fun signUp(email: String, name: String, password: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Enter your name."))
        if (password.length < 8) return Result.failure(Exception("Password must be at least 8 characters."))
        return authenticate("/auth/register", mapOf("email" to email.trim(), "name" to name, "password" to password))
    }
    suspend fun signIn(email: String, password: String) =
        authenticate("/auth/login", mapOf("email" to email.trim(), "password" to password))

    suspend fun signInWithGoogle(): Result<Unit> {
        val token = safe { BackendPlatform.googleIdToken() }
        return token.fold(
            onSuccess = { authenticate("/auth/google", mapOf("idToken" to it)) },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun signOut(): Result<Unit> {
        try {
            BackendHttp.request(HttpMethod.Delete, "/auth/refresh")
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            // Offline logout must still clear local identity.
        } finally {
            BackendHttp.adopt(null)
            _session.value = null
            _cachedSession.value = null
            _authChecked.value = true
        }
        return Result.success(Unit)
    }

    suspend fun refreshSession() {
        init()
        try {
            if (BackendHttp.tokens.value == null) { _session.value = null; return }
            val obj = Json.parseToJsonElement(BackendHttp.request(HttpMethod.Get, "/auth/me")).jsonObject
            val current = BackendHttp.tokens.value ?: return
            val user = AuthSession(obj.getValue("userId").jsonPrimitive.content,
                obj.getValue("email").jsonPrimitive.content, obj.getValue("name").jsonPrimitive.content)
            if (current.userId == user.userId) {
                BackendHttp.adopt(current.copy(email = user.email, name = user.name))
                _session.value = user
                _cachedSession.value = user
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            _session.value = null
            if (e is BackendException && e.code == 401) BackendHttp.adopt(null)
            _cachedSession.value = BackendHttp.tokens.value?.asSession()
        } finally { _authChecked.value = true }
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) {
        val message = if (e is AuthUserException) e.message ?: "Sign-in failed. Please retry." else if (e is BackendException) when (e.code) {
            400 -> "Check your details and try again."
            401 -> "Invalid email or password."
            409 -> "An account with this email already exists. Log in instead."
            429 -> "Too many attempts. Please wait and try again."
            else -> "Service unavailable. Try again later."
        } else "Unable to connect. Check your connection and try again."
        Result.failure(Exception(message))
    }
}
