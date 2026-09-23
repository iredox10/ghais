package com.ghais.data.auth

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import io.appwrite.enums.OAuthProvider
import io.appwrite.services.Account
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Browser fallback for Google sign-in (Appwrite OAuth2 + Custom Tab).
 *
 * Used when native Credential Manager has nothing usable (no Google accounts
 * on device, or provider misconfiguration such as error 28444): the browser
 * flow lets the user sign into Google on the web, so it needs neither device
 * accounts nor a registered SHA-1. The redirect lands on
 * [AppwriteConfig.OAUTH_SUCCESS_URL] (`appwrite-callback-<project>://auth`),
 * which Appwrite accepts without extra platform registration.
 *
 * Flow: [signIn] opens the provider URL and suspends until [onRedirect]
 * completes the pending handshake with the `userId`/`secret` query params
 * (or a cancellation). Closing the tab without completing resolves via
 * [onReturnedToApp] as a cancellation.
 *
 * Public so the host Activity (app module) can forward OAuth redirects;
 * token exchange itself stays inside AuthRepository.
 */
object GoogleWebAuth {

    data class OAuthTokens(val userId: String, val secret: String)

    private var pending: CompletableDeferred<Result<OAuthTokens>>? = null

    /**
     * Opens the Google OAuth page and suspends until the redirect returns.
     * Returns tokens for `Account.createSession`, never raw user text.
     */
    suspend fun signIn(activity: ComponentActivity, account: Account): Result<OAuthTokens> {
        val deferred = CompletableDeferred<Result<OAuthTokens>>()
        pending?.complete(Result.failure(AuthUserException("Superseded.")))
        pending = deferred
        val url = try {
            account.createOAuth2Token(
                provider = OAuthProvider.GOOGLE,
                success = AppwriteConfig.OAUTH_SUCCESS_URL,
                failure = AppwriteConfig.OAUTH_FAILURE_URL,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            pending = null
            return Result.failure(AuthUserException("Google sign-in failed, please try again."))
        }
        openTab(activity, url)
        return try {
            withTimeout(5 * 60_000L) { deferred.await() }
        } catch (_: TimeoutCancellationException) {
            if (pending === deferred) pending = null
            Result.failure(AuthUserException("Google sign-in timed out, please try again."))
        }
    }

    /**
     * Handles the OAuth redirect. Returns true when the URI belongs to this
     * handshake (success or failure) and completes the pending sign-in.
     * Completes synchronously so a following onResume sees no pending flow.
     */
    fun onRedirect(uri: Uri?): Boolean {
        val deferred = pending ?: return false
        if (uri == null ||
            uri.scheme != AppwriteConfig.OAUTH_REDIRECT_SCHEME ||
            uri.host != "auth"
        ) {
            return false
        }
        pending = null
        val userId = uri.getQueryParameter("userId")
        val secret = uri.getQueryParameter("secret")
        if (!userId.isNullOrBlank() && !secret.isNullOrBlank()) {
            deferred.complete(Result.success(OAuthTokens(userId, secret)))
        } else if (!uri.getQueryParameter("error").isNullOrBlank()) {
            // Provider-side failure (e.g. redirect_uri_mismatch when the
            // Appwrite callback URL isn't registered in Google Cloud) — a
            // real failure, not a user cancellation.
            deferred.complete(Result.failure(AuthUserException("Google sign-in failed, please try again.")))
        } else {
            deferred.complete(Result.failure(AuthUserException("Google sign-in cancelled.")))
        }
        return true
    }

    /**
     * Call from the host Activity's onResume: if a handshake is still pending
     * after returning to the app with no redirect (user closed the tab),
     * resolve it as a cancellation after a short grace period.
     */
    fun onReturnedToApp() {
        val deferred = pending ?: return
        Handler(Looper.getMainLooper()).postDelayed({
            if (pending === deferred) {
                pending = null
                deferred.complete(Result.failure(AuthUserException("Google sign-in cancelled.")))
            }
        }, 1_500L)
    }

    private fun openTab(activity: ComponentActivity, url: String) {
        val uri = Uri.parse(url)
        try {
            CustomTabsIntent.Builder().build().launchUrl(activity, uri)
        } catch (_: Exception) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                // No browser at all: resolve pending so the caller isn't stuck.
                val deferred = pending
                pending = null
                deferred?.complete(Result.failure(AuthUserException("No browser found to continue with Google.")))
            }
        }
    }
}
