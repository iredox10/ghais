package com.ghais.data.auth

/**
 * Appwrite Cloud connection settings for the "ghais" project.
 *
 * Fill in [ENDPOINT] and [PROJECT_ID] from the Appwrite Console
 * (Console > ghais project > Settings). The endpoint has the form
 * `https://<REGION>.cloud.appwrite.io/v1`, e.g.
 * `https://fra.cloud.appwrite.io/v1`.
 *
 * No secret API key is stored here on purpose: this is a client app, so it
 * authenticates as the end user via account sessions, never via `setKey()`.
 */
object AppwriteConfig {
    const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    const val PROJECT_ID = "tikitakatalk"

    /**
     * Deep-link scheme the app uses for the OAuth2 redirect back from the
     * browser (`successUrl` / `failureUrl`). Uses Appwrite's reserved
     * `appwrite-callback-<project>` scheme, which the server accepts without
     * extra platform registration (a custom scheme like `ghais://auth` gets
     * rejected with "Invalid 'success' param" until registered in Console).
     * Must match the intent-filter / URL-type registered on each platform.
     */
    const val OAUTH_REDIRECT_SCHEME = "appwrite-callback-tikitakatalk"

    /** Success redirect target for OAuth2 (userId + secret come as query params). */
    const val OAUTH_SUCCESS_URL = "$OAUTH_REDIRECT_SCHEME://auth"

    /** Same deep link; the failure case carries `?error=...` query params. */
    const val OAUTH_FAILURE_URL = "$OAUTH_REDIRECT_SCHEME://auth"

    /**
     * True once real console values have been pasted in. Guards SDK usage so
     * the app fails fast with a clear message instead of calling Appwrite
     * with placeholder values.
     */
    fun isConfigured(): Boolean =
        !ENDPOINT.contains("YOUR-") &&
            !ENDPOINT.contains("TODO") &&
            !PROJECT_ID.contains("PASTE") &&
            !PROJECT_ID.contains("TODO")
}
