package com.ghais.data.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
        val result = CredentialManager.create(activity).getCredential(activity, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }
}
