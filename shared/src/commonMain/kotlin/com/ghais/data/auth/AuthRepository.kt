package com.ghais.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Currently signed-in user, as last confirmed by Appwrite (`account.get()`).
 */
data class AuthSession(
    val userId: String,
    val email: String,
    val name: String,
)

/**
 * Email+password and Google-OAuth authentication against Appwrite Cloud.
 *
 * Platform notes: the official Appwrite Kotlin SDK (`io.appwrite:sdk-for-kotlin`)
 * is JVM-only, so the live implementation lives in `androidMain` ([actual]
 * below) while iOS is a stub until a Native HTTP path lands. UI talks only to
 * this contract. Configure endpoint + project id in [AppwriteConfig]; all calls
 * fail with a clear message while unconfigured.
 */
expect object AuthRepository {
    /** Observable signed-in user; `null` = signed out (or not yet checked). */
    val session: StateFlow<AuthSession?>

    /** Registers a new account and immediately signs the user in. */
    suspend fun signUp(email: String, name: String, password: String): Result<Unit>

    /** Signs in with an existing email+password account. */
    suspend fun signIn(email: String, password: String): Result<Unit>

    /**
     * Starts Google OAuth (opens the provider page in the system browser;
     * completes via the `ghais://auth` redirect). Console setup required:
     * Google provider enabled, native platforms registered, redirect URL
     * allow-listed (see KDoc on the android actual).
     */
    suspend fun signInWithGoogle(): Result<Unit>

    /**
     * Completes Google OAuth after the platform layer intercepts the
     * `ghais://auth?userId=...&secret=...` success redirect.
     */
    suspend fun completeGoogleSignIn(userId: String, secret: String): Result<Unit>

    /** Signs out (local state always cleared, even if the network call fails). */
    suspend fun signOut(): Result<Unit>

    /** Re-checks the current user (call on app launch). */
    suspend fun refreshSession()
}
