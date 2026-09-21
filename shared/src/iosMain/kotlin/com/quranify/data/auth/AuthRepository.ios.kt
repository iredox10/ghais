package com.quranify.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub: the Appwrite Kotlin SDK is JVM-only, so there is no live auth on
 * iOS yet (options: Ktor-direct REST via `ktor-client-darwin`, or a Swift SDK
 * bridge). All calls fail with a clear message; sign-out/refresh are safe
 * no-ops. The auth gate treats this as signed-out, so guest mode still works.
 */
actual object AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    actual val session: StateFlow<AuthSession?> = _session.asStateFlow()

    actual suspend fun signUp(email: String, name: String, password: String): Result<Unit> =
        Result.failure(Exception("Sign-up is not available on iOS yet — continue as guest."))

    actual suspend fun signIn(email: String, password: String): Result<Unit> =
        Result.failure(Exception("Sign-in is not available on iOS yet — continue as guest."))

    actual suspend fun signInWithGoogle(): Result<Unit> =
        Result.failure(Exception("Google sign-in is not available on iOS yet — continue as guest."))

    actual suspend fun completeGoogleSignIn(userId: String, secret: String): Result<Unit> =
        Result.failure(Exception("Google sign-in is not available on iOS yet."))

    actual suspend fun signOut(): Result<Unit> {
        _session.value = null
        return Result.success(Unit)
    }

    actual suspend fun refreshSession() {
        _session.value = null
    }
}
