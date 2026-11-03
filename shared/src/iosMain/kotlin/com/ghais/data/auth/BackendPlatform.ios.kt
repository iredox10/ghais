package com.ghais.data.backend

import platform.Foundation.NSBundle
import com.ghais.data.auth.AuthUserException

/** The native iOS host supplies its Google Sign-In ID-token presentation. */
object IosGoogleSignIn {
    var idTokenProvider: (suspend () -> String)? = null
}

actual object BackendPlatform {
    actual val apiEndpoint: String = com.ghais.data.auth.BackendConfig.PRODUCTION_ENDPOINT
    actual val name = "ios"
    actual fun appVersion(): String = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
    actual suspend fun googleIdToken(): String =
        IosGoogleSignIn.idTokenProvider?.invoke() ?: throw AuthUserException("Google sign-in is not available on iOS yet. Use email to sign in.")
}
