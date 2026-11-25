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
        // Startup readiness means local identity has loaded. Server validation
        // runs independently so a slow connection never gates guest/offline UI.
        _authChecked.value = true
    }

    private fun TokenBundle.asSession() = AuthSession(userId, email, name)
    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        val at = trimmed.indexOf('@')
        val dot = trimmed.lastIndexOf('.')
        return at > 0 && dot > at + 1 && dot < trimmed.length - 1 && !trimmed.contains(' ')
    }

    private suspend fun authenticate(path: String, fields: Map<String, String>): Result<TokenBundle> = safe {
        val pair = BackendHttp.authenticate(path, fields)
        BackendHttp.adopt(pair)
        _cachedSession.value = pair.asSession()
        _session.value = pair.asSession()
        _authChecked.value = true
        pair
    }

    suspend fun signUp(email: String, name: String, password: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Enter your name."))
        if (!isValidEmail(email)) return Result.failure(Exception("Enter a valid email address."))
        if (password.length < 8) return Result.failure(Exception("Password must be at least 8 characters."))
        return authenticate("/auth/register", mapOf("email" to email.trim(), "name" to name.trim(), "password" to password)).map { }
    }
    suspend fun signIn(email: String, password: String): Result<Unit> {
        if (!isValidEmail(email)) return Result.failure(Exception("Enter a valid email address."))
        if (password.isEmpty()) return Result.failure(Exception("Enter your password."))
        return authenticate("/auth/login", mapOf("email" to email.trim(), "password" to password)).map { }
    }

    suspend fun signInWithGoogle(): Result<Boolean> {
        val token = safe { BackendPlatform.googleIdToken() }
        return token.fold(
            onSuccess = { idToken ->
                authenticate("/auth/google", mapOf("idToken" to idToken)).map { it.isNewUser ?: false }
            },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        if (!isValidEmail(email)) return Result.failure(Exception("Enter a valid email address."))
        return safe {
            val body = buildJsonObject { put("email", email.trim()) }
            val text = BackendHttp.request(HttpMethod.Post, "/auth/forgot-password", body, authenticated = false)
            Json.parseToJsonElement(text).jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: "If an account exists for that email, a reset code has been sent."
        }
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): Result<String> {
        if (!isValidEmail(email)) return Result.failure(Exception("Enter a valid email address."))
        if (!code.trim().matches(Regex("^\\d{6}$"))) return Result.failure(Exception("Enter the 6-digit reset code."))
        if (newPassword.length < 8) return Result.failure(Exception("Password must be at least 8 characters."))
        return safe {
            val body = buildJsonObject {
                put("email", email.trim())
                put("code", code.trim())
                put("newPassword", newPassword)
            }
            val text = BackendHttp.request(HttpMethod.Post, "/auth/reset-password", body, authenticated = false)
            Json.parseToJsonElement(text).jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: "Password updated. You can now log in."
        }
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
        val restoringUserId = BackendHttp.tokens.value?.userId
        try {
            if (restoringUserId == null) { _session.value = null; return }
            val obj = Json.parseToJsonElement(BackendHttp.request(HttpMethod.Get, "/auth/me", expectedUserId = restoringUserId)).jsonObject
            val current = BackendHttp.tokens.value ?: return
            if (current.userId != restoringUserId) return
            val user = AuthSession(obj.getValue("userId").jsonPrimitive.content,
                obj.getValue("email").jsonPrimitive.content, obj.getValue("name").jsonPrimitive.content)
            if (current.userId == user.userId) {
                BackendHttp.adopt(current.copy(email = user.email, name = user.name))
                _session.value = user
                _cachedSession.value = user
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            // A response from an old account must never clear a newer login.
            if (BackendHttp.tokens.value?.userId != restoringUserId) return
            if (e is BackendException && e.code == 401) {
                BackendHttp.adopt(null)
                _session.value = null
            }
            _cachedSession.value = BackendHttp.tokens.value?.asSession()
        } finally { _authChecked.value = true }
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) {
        val message = if (e is AuthUserException) e.message ?: "Sign-in failed. Please retry." else if (e is BackendException) when (e.code) {
            400 -> e.message?.takeIf { !it.startsWith("Request failed") } ?: "Check your details and try again."
            401 -> "Invalid email or password."
            409 -> "An account with this email already exists. Log in instead."
            429 -> "Too many attempts. Please wait and try again."
            else -> "Service unavailable. Try again later."
        } else "Unable to connect. Check your connection and try again."
        Result.failure(Exception(message))
    }
}
