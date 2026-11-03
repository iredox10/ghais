package com.ghais.data.backend

import com.ghais.data.auth.AuthUserException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.ghais.data.auth.ActivityHolder
import com.ghais.data.auth.BackendConfig
import com.ghais.data.auth.GoogleNativeAuth

actual object BackendPlatform {
    actual val apiEndpoint: String get() = com.ghais.shared.BuildConfig.BACKEND_ENDPOINT.ifBlank { BackendConfig.PRODUCTION_ENDPOINT }
    actual val name = "android"
    actual fun appVersion(): String = try {
        val activity = ActivityHolder.current()
        @Suppress("DEPRECATION")
        activity?.packageManager?.getPackageInfo(activity.packageName, 0)?.versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }
    actual suspend fun googleIdToken(): String {
        val activity = ActivityHolder.current() ?: error("Google sign-in is not ready.")
        return try {
            GoogleNativeAuth.getIdToken(activity, BackendConfig.GOOGLE_WEB_CLIENT_ID)
        } catch (_: GetCredentialCancellationException) {
            throw AuthUserException("Google sign-in cancelled.")
        }
    }
}
