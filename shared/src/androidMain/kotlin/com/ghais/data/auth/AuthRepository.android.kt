package com.ghais.data.auth

import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.enums.OAuthProvider
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android live implementation (Appwrite Kotlin SDK, JVM artifact).
 *
 * Google OAuth: opens the provider URL in the system browser; Appwrite
 * redirects to `ghais://auth?userId=..&secret=..`, which lands in
 * `MainActivity` (intent-filter + `singleTask`) and finishes via
 * [completeGoogleSignIn]. Console prerequisites: Google provider enabled with
 * a Cloud OAuth client, Android platform registered (package + SHA-256), and
 * `ghais://auth` allow-listed as success/failure URL.
 */
actual object AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    actual val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private var appContext: android.content.Context? = null
    private var account: Account? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    private fun accountOrThrow(): Account {
        account?.let { return it }
        check(AppwriteConfig.isConfigured()) {
            "Appwrite is not configured: paste ENDPOINT and PROJECT_ID in AppwriteConfig."
        }
        val newAccount = Account(
            Client()
                .setEndpoint(AppwriteConfig.ENDPOINT)
                .setProject(AppwriteConfig.PROJECT_ID),
        )
        account = newAccount
        return newAccount
    }

    actual suspend fun signUp(email: String, name: String, password: String): Result<Unit> {
        return runCatching {
            val account = accountOrThrow()
            try {
                account.create(
                    userId = ID.unique(),
                    email = email,
                    password = password,
                    name = name,
                )
                account.createEmailPasswordSession(email = email, password = password)
                refreshSession()
            } catch (e: AppwriteException) {
                throw Exception(friendlyMessage(e))
            }
        }
    }

    actual suspend fun signIn(email: String, password: String): Result<Unit> {
        return runCatching {
            val account = accountOrThrow()
            try {
                account.createEmailPasswordSession(email = email, password = password)
                refreshSession()
            } catch (e: AppwriteException) {
                throw Exception(friendlyMessage(e))
            }
        }
    }

    actual suspend fun signInWithGoogle(): Result<Unit> {
        return runCatching {
            val account = accountOrThrow()
            val url = try {
                account.createOAuth2Token(
                    provider = OAuthProvider.GOOGLE,
                    success = AppwriteConfig.OAUTH_SUCCESS_URL,
                    failure = AppwriteConfig.OAUTH_FAILURE_URL,
                )
            } catch (e: AppwriteException) {
                throw Exception(friendlyMessage(e))
            }
            val ctx = appContext
                ?: throw Exception("Google sign-in is not ready yet, please retry.")
            try {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (e: Exception) {
                throw Exception("Could not open a browser for Google sign-in.")
            }
        }
    }

    actual suspend fun completeGoogleSignIn(userId: String, secret: String): Result<Unit> {
        return runCatching {
            val account = accountOrThrow()
            try {
                account.createSession(userId = userId, secret = secret)
                refreshSession()
            } catch (e: AppwriteException) {
                throw Exception(friendlyMessage(e))
            }
        }
    }

    actual suspend fun signOut(): Result<Unit> {
        return runCatching {
            try {
                accountOrThrow().deleteSession(sessionId = "current")
            } catch (e: AppwriteException) {
                throw Exception(friendlyMessage(e))
            } finally {
                _session.value = null
            }
        }
    }

    actual suspend fun refreshSession() {
        val user = try {
            accountOrThrow().get()
        } catch (_: Exception) {
            _session.value = null
            return
        }
        _session.value = AuthSession(
            userId = user.id,
            email = user.email,
            name = user.name,
        )
    }

    private fun friendlyMessage(e: AppwriteException): String {
        val raw = e.message?.takeIf { it.isNotBlank() }
        return when (e.code) {
            401 -> "Invalid email or password."
            409 -> "An account with this email already exists."
            429 -> "Too many attempts. Please wait and try again."
            else -> raw ?: "Authentication failed${e.code?.let { " (code $it)" } ?: ""}."
        }
    }
}
