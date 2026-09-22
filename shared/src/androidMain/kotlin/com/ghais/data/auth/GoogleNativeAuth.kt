package com.ghais.data.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.lang.ref.WeakReference

/**
 * Holds the foreground [ComponentActivity] so platform auth flows that need an
 * Activity context (Google Credential Manager) can run from shared code.
 * Registered by `MainActivity.onCreate`; public so the app module can call it.
 */
object ActivityHolder {
    private var ref: WeakReference<ComponentActivity>? = null

    fun register(activity: ComponentActivity) {
        ref = WeakReference(activity)
    }

    fun current(): ComponentActivity? = ref?.get()
}

/**
 * Native Google sign-in via Credential Manager: returns a Google ID token for
 * [serverClientId] (must be the Google Cloud **Web** client ID) without
 * leaving the app. Throws [GetCredentialCancellationException] when the user
 * dismisses the sheet — callers should surface "cancelled", not fall back.
 */
internal object GoogleNativeAuth {

    suspend fun getIdToken(
        activity: ComponentActivity,
        serverClientId: String,
    ): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        try {
            val result = CredentialManager.create(activity).getCredential(activity, request)
            return try {
                GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            } catch (e: IllegalArgumentException) {
                throw Exception("Google returned an unexpected credential. Log in with email instead.", e)
            } catch (e: GoogleIdTokenParsingException) {
                throw Exception("Google returned an unexpected credential. Log in with email instead.", e)
            }
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            throw Exception("No Google accounts on this device. Add one in Settings, or log in with email.")
        } catch (e: GetCredentialException) {
            // Backend detail (e.g. error 28444 when the SHA-1 is not
            // registered for the OAuth client) stays in logcat, never in
            // the user-facing box. Console checklist lives in KDoc above.
            android.util.Log.w("GoogleNativeAuth", "getCredential failed", e)
            throw Exception("Google sign-in is unavailable right now. Log in with email instead.")
        }
    }
}
