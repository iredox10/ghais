package com.ghais.data.auth

import com.ghais.data.sync.NetworkMonitor
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.coroutines.cancellation.CancellationException
import java.util.concurrent.TimeUnit

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
/**
 * Marker for intentionally user-safe messages. [AuthRepository.userMessage]
 * passes through only these (plus mapped network/Appwrite cases) — every
 * other throwable maps to generic copy so raw SDK/OkHttp/server text (e.g.
 * JSON parse errors, "Expected URL", AppwriteException message) can never
 * reach the error box via `e.message` passthrough.
 *
 * Top-level internal (not private) so [GoogleWebAuth] can fail with the same
 * safe type; otherwise its "cancelled / timed out" copy would be flattened
 * to generic by [AuthRepository.userMessage].
 */
internal class AuthUserException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

actual object AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    actual val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val _authChecked = MutableStateFlow(false)
    actual val authChecked: StateFlow<Boolean> = _authChecked.asStateFlow()

    private val _cachedSession = MutableStateFlow<AuthSession?>(null)

    /**
     * Last successfully refreshed session, persisted to disk for offline
     * launch. Initialized from prefs in [init] (no network), updated on
     * every successful [refreshSession], cleared on [signOut]. Unlike
     * [session] (memory-only, nulled on any refresh failure), this survives
     * offline 401s/failures — the UI layer decides offline mode.
     */
    actual val cachedSession: StateFlow<AuthSession?> = _cachedSession.asStateFlow()

    private var appContext: android.content.Context? = null
    private var account: Account? = null
    private var cookieJar: PersistentCookieJar? = null

    private const val CACHED_PREFS = "ghais_appwrite_cookies"
    private const val KEY_CACHED_USER_ID = "cached_session_userId"
    private const val KEY_CACHED_EMAIL = "cached_session_email"
    private const val KEY_CACHED_NAME = "cached_session_name"

    private fun cachedPrefs(): android.content.SharedPreferences? =
        appContext?.getSharedPreferences(CACHED_PREFS, android.content.Context.MODE_PRIVATE)

    private fun readCachedSession(): AuthSession? {
        val prefs = cachedPrefs() ?: return null
        val userId = prefs.getString(KEY_CACHED_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val email = prefs.getString(KEY_CACHED_EMAIL, null) ?: ""
        val name = prefs.getString(KEY_CACHED_NAME, null) ?: ""
        return AuthSession(userId = userId, email = email, name = name)
    }

    private fun persistCachedSession(session: AuthSession) {
        cachedPrefs()?.edit()
            ?.putString(KEY_CACHED_USER_ID, session.userId)
            ?.putString(KEY_CACHED_EMAIL, session.email)
            ?.putString(KEY_CACHED_NAME, session.name)
            ?.apply()
        _cachedSession.value = session
    }

    private fun clearCachedSession() {
        cachedPrefs()?.edit()
            ?.remove(KEY_CACHED_USER_ID)
            ?.remove(KEY_CACHED_EMAIL)
            ?.remove(KEY_CACHED_NAME)
            ?.apply()
        _cachedSession.value = null
    }

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        // Disk only — no network. Surfaces the last known login instantly so
        // an offline launch doesn't flash the login gate.
        _cachedSession.value = readCachedSession()
    }

    @Synchronized
    private fun accountOrThrow(): Account {
        account?.let { return it }
        check(AppwriteConfig.isConfigured()) {
            "Service unavailable. Try again later."
        }
        val ctx = appContext
            ?: throw IllegalStateException("Service unavailable. Try again later.")
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
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build()
                },
        )
        account = newAccount
        return newAccount
    }

    actual suspend fun signUp(email: String, name: String, password: String): Result<Unit> {
        validateEmail(email)?.let { return Result.failure(Exception(it)) }
        if (name.isBlank()) return Result.failure(Exception("Enter your name."))
        if (password.length < 8) return Result.failure(Exception("Password must be at least 8 characters."))
        if (!NetworkMonitor.isOnline.value) return Result.failure(Exception("No internet connection. Connect and try again."))
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
                if (_session.value == null) {
                    throw AuthUserException("Something went wrong. Please try again.")
                }
            } catch (e: AppwriteException) {
                throw AuthUserException(friendlyMessage(e))
            }
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            throw AuthUserException(userMessage(e))
        }
    }

    actual suspend fun signIn(email: String, password: String): Result<Unit> {
        validateEmail(email)?.let { return Result.failure(Exception(it)) }
        if (password.isEmpty()) return Result.failure(Exception("Enter your password."))
        if (!NetworkMonitor.isOnline.value) return Result.failure(Exception("No internet connection. Connect and try again."))
        return runCatching {
            val account = accountOrThrow()
            try {
                account.createEmailPasswordSession(email = email, password = password)
                refreshSession()
                _authChecked.value = true
                if (_session.value == null) {
                    throw AuthUserException("Something went wrong. Please try again.")
                }
            } catch (e: AppwriteException) {
                throw AuthUserException(friendlyMessage(e))
            }
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            throw AuthUserException(userMessage(e))
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Enter your email address."
        val ok = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)
        return if (ok) null else "Enter a valid email address."
    }

    /**
     * Native-only Google sign-in: Google ID token on-device via Credential
     * Manager, exchanged for an Appwrite session. No browser involved.
     * Requires [AppwriteConfig.GOOGLE_WEB_CLIENT_ID] (Google Cloud **Web**
     * client ID) plus the matching native client ID registered in the
     * Appwrite Console (Auth > Google > Native client IDs).
     */
    actual suspend fun signInWithGoogle(): Result<Unit> {
        if (!NetworkMonitor.isOnline.value) return Result.failure(Exception("No internet connection. Connect and try again."))
        return runCatching {
            val account = accountOrThrow()
            val activity = ActivityHolder.current()
                ?: throw AuthUserException("Google sign-in is not ready yet, please retry.")
            // 1) Native: on-device ID token, no browser. Skipped when the
            // Web client ID isn't configured — the browser fallback below
            // doesn't need it.
            val webClientId = AppwriteConfig.GOOGLE_WEB_CLIENT_ID
            val nativeReady = webClientId.isNotBlank() && !webClientId.contains("PASTE")
            if (nativeReady) {
                val native = runCatching {
                    val idToken = GoogleNativeAuth.getIdToken(activity, webClientId)
                    exchangeGoogleIdToken(idToken)
                }
                if (native.isSuccess) {
                    refreshSession()
                    _authChecked.value = true
                    if (_session.value != null) return@runCatching
                } else if (native.exceptionOrNull() is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    throw AuthUserException("Google sign-in cancelled.")
                }
                // Any other native failure (no device accounts, provider
                // misconfiguration) falls through to the browser flow.
            }
            // 2) Browser fallback: Appwrite OAuth2 in a Custom Tab. Works
            // with zero device accounts and no SHA-1 registration.
            val tokens = GoogleWebAuth.signIn(activity, account).getOrThrow()
            try {
                account.createSession(userId = tokens.userId, secret = tokens.secret)
            } catch (e: AppwriteException) {
                throw AuthUserException(friendlyMessage(e))
            }
            refreshSession()
            _authChecked.value = true
            if (_session.value == null) {
                throw AuthUserException("Google sign-in failed, please try again.")
            }
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            throw AuthUserException(userMessage(e))
        }
    }

    /**
     * Exchanges a Google ID token for an Appwrite session via
     * `POST /account/sessions/id-token`, sharing the persistent cookie jar so
     * the resulting session survives restarts. Throws [AuthUserException]
     * with generic copy on failure — never the server's message.
     */
    private suspend fun exchangeGoogleIdToken(idToken: String) = withContext(Dispatchers.IO) {
        accountOrThrow()
        val jar = cookieJar
            ?: throw AuthUserException("Auth storage is not ready yet, please retry.")
        val http = okhttp3.OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        val body = "{\"provider\":\"google\",\"token\":\"$idToken\"}"
        val request = okhttp3.Request.Builder()
            .url("${AppwriteConfig.ENDPOINT}/account/sessions/id-token")
            .addHeader("X-Appwrite-Project", AppwriteConfig.PROJECT_ID)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        fun check(response: okhttp3.Response) {
            if (!response.isSuccessful) {
                // Deliberately generic: raw server JSON and HTTP codes
                // must never reach the user-facing error box.
                throw AuthUserException("Google sign-in failed, please try again.")
            }
        }
        try {
            http.newCall(request).execute().use { response -> check(response) }
        } catch (_: SocketTimeoutException) {
            // One bounded retry on a stalled id-token exchange only; a
            // fresh Call on the same client (same jar). A second timeout
            // propagates to the native runCatching above and falls through
            // to the browser flow (never surfaced raw).
            http.newCall(request).execute().use { response -> check(response) }
        }
    }

    actual suspend fun signOut(): Result<Unit> {
        return runCatching {
            try {
                accountOrThrow().deleteSession(sessionId = "current")
            } catch (e: AppwriteException) {
                throw AuthUserException(friendlyMessage(e))
            } finally {
                // cookieJar is set by accountOrThrow above, but if it threw
                // (not configured / init missing) the jar would be null and
                // stale cookies on disk would resurrect the session on next
                // launch — fall back to a fresh jar so sign-out always wipes.
                (cookieJar ?: appContext?.let { PersistentCookieJar(it) })?.clear()
                clearCachedSession()
                _session.value = null
                _authChecked.value = true
            }
            // deleteSession returns Any in SDK 22; pin the Result to Unit.
            Unit
        }.recoverCatching { e ->
            // Local state is already wiped by the finally above; only the
            // surfaced message is mapped. Raw network errors (e.g. offline
            // deleteSession IOException) must not reach the UI.
            if (e is CancellationException) throw e
            throw AuthUserException(userMessage(e))
        }
    }

    actual suspend fun refreshSession() {
        try {
            val user = try {
                accountOrThrow().get()
            } catch (_: Exception) {
                // Offline / 401 failure: memory session stays null, but the
                // disk cache (cachedSession) is deliberately left intact so
                // the UI layer can decide offline mode.
                _session.value = null
                return
            }
            val fresh = AuthSession(
                userId = user.id,
                email = user.email,
                name = user.name,
            )
            _session.value = fresh
            persistCachedSession(fresh)
        } finally {
            _authChecked.value = true
        }
    }

    private fun friendlyMessage(e: AppwriteException): String {
        // Never surface raw server text or numeric codes: 400s carry
        // 'Invalid ... param' jargon and catch-alls leak "(code NNN)".
        return when (e.code) {
            400 -> "Check your details and try again."
            401 -> "Invalid email or password."
            404 -> "Service unavailable. Try again later."
            409 -> "An account with this email already exists. Log in instead."
            429 -> "Too many attempts. Please wait and try again."
            503 -> "Service unavailable. Try again later."
            else -> "Something went wrong. Please try again."
        }
    }

    /**
     * Maps non-Appwrite failures (network, init order) to user-safe copy.
     *
     * Only [AuthUserException] messages pass through (safe by construction —
     * every throw site uses a fixed literal). Everything else maps to
     * generic copy: raw `e.message` text (OkHttp errors, JSON parse details,
     * Appwrite server text) must never reach the UI.
     */
    private fun userMessage(e: Throwable): String = when (e) {
        // Never swallow structured cancellation: rethrow so runCatching
        // can't convert it into a failure Result (defense in depth; the
        // recoverCatching sites already rethrow first).
        is CancellationException -> throw e
        is AuthUserException -> e.message?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Please try again."
        // The SDK may wrap transport failures as AppwriteException with a
        // null/zero code — unwrap to the causal IOException first so DNS /
        // timeout / TLS failures still read as offline, never raw.
        is AppwriteException -> {
            val transport = generateSequence<Throwable>(e) { it.cause }
                .firstOrNull { it is UnknownHostException || it is SocketTimeoutException || it is SSLException }
            if (e.code == null || e.code == 0 || transport != null) {
                offlineCopy()
            } else {
                friendlyMessage(e)
            }
        }
        // Explicit network branches (all IOException subclasses, kept
        // explicit so a future copy change can't silently re-leak them):
        is UnknownHostException -> offlineCopy()
        is SocketTimeoutException -> offlineCopy()
        is SSLException -> offlineCopy()
        is java.io.IOException -> offlineCopy()
        is IllegalStateException -> "Service unavailable. Try again later."
        else -> "Something went wrong. Please try again."
    }

    private fun offlineCopy(): String = "No internet connection. Connect and try again."
}
