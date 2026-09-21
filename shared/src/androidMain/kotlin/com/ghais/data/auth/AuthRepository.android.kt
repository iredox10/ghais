package com.ghais.data.auth

import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Android live implementation (Appwrite Kotlin SDK, JVM artifact).
 *
 * Google sign-in is native-only: Credential Manager returns a Google ID
 * token on-device, exchanged for an Appwrite session via
 * `POST /account/sessions/id-token`. Console prerequisites: Google provider
 * with native sign-in enabled, the Web client ID pasted in
 * [AppwriteConfig.GOOGLE_WEB_CLIENT_ID], and matching native client ID(s)
 * registered in the Console.
 */
actual object AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    actual val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _authChecked = MutableStateFlow(false)
    actual val authChecked: StateFlow<Boolean> = _authChecked.asStateFlow()

    private var appContext: android.content.Context? = null
    private var account: Account? = null
    private var cookieJar: PersistentCookieJar? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    private fun accountOrThrow(): Account {
        account?.let { return it }
        check(AppwriteConfig.isConfigured()) {
            "Appwrite is not configured: paste ENDPOINT and PROJECT_ID in AppwriteConfig."
        }
        val ctx = appContext
            ?: throw IllegalStateException("AuthRepository.init(context) has not been called yet.")
        val newAccount = Account(
            Client()
                .setEndpoint(AppwriteConfig.ENDPOINT)
                .setProject(AppwriteConfig.PROJECT_ID)
                .apply {
                    // Persistent cookies keep the a_session_* session across
                    // restarts — without this every account.get() 401s and
                    // signup looks like it "does nothing".
                    val jar = PersistentCookieJar(ctx)
                    cookieJar = jar
                    http = okhttp3.OkHttpClient.Builder()
                        .cookieJar(jar)
                        .build()
                },
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
                _authChecked.value = true
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
                _authChecked.value = true
            } catch (e: AppwriteException) {
                throw Exception(friendlyMessage(e))
            }
        }
    }

    /**
     * Native-only Google sign-in: Google ID token on-device via Credential
     * Manager, exchanged for an Appwrite session. No browser involved.
     * Requires [AppwriteConfig.GOOGLE_WEB_CLIENT_ID] (Google Cloud **Web**
     * client ID) plus the matching native client ID registered in the
     * Appwrite Console (Auth > Google > Native client IDs).
     */
    actual suspend fun signInWithGoogle(): Result<Unit> {
        return runCatching {
            accountOrThrow()
            val activity = ActivityHolder.current()
                ?: throw Exception("Google sign-in is not ready yet, please retry.")
            val webClientId = AppwriteConfig.GOOGLE_WEB_CLIENT_ID
            if (webClientId.isBlank() || webClientId.contains("PASTE")) {
                throw Exception("Google sign-in is not configured yet on this build.")
            }
            val idToken = try {
                GoogleNativeAuth.getIdToken(activity, webClientId)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                throw Exception("Google sign-in cancelled.")
            }
            exchangeGoogleIdToken(idToken)
            refreshSession()
            _authChecked.value = true
            if (_session.value == null) {
                throw Exception("Google sign-in failed, please try again.")
            }
        }
    }

    /**
     * Exchanges a Google ID token for an Appwrite session via
     * `POST /account/sessions/id-token`, sharing the persistent cookie jar so
     * the resulting session survives restarts. Throws with the server's
     * message on failure.
     */
    private fun exchangeGoogleIdToken(idToken: String) {
        accountOrThrow()
        val jar = cookieJar
            ?: throw IllegalStateException("Auth storage is not ready yet, please retry.")
        val http = okhttp3.OkHttpClient.Builder().cookieJar(jar).build()
        val body = "{\"provider\":\"google\",\"token\":\"$idToken\"}"
        val request = okhttp3.Request.Builder()
            .url("${AppwriteConfig.ENDPOINT}/account/sessions/id-token")
            .addHeader("X-Appwrite-Project", AppwriteConfig.PROJECT_ID)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message = response.body.string()
                    .let { Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
                    ?.takeIf { it.isNotBlank() }
                    ?: "Google sign-in failed (server ${response.code})."
                throw Exception(message)
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
                cookieJar?.clear()
                _session.value = null
                _authChecked.value = true
            }
        }
    }

    actual suspend fun refreshSession() {
        try {
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
        } finally {
            _authChecked.value = true
        }
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
