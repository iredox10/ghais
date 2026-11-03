package com.ghais.data.auth

/** Render API settings. No server secret is shipped in the app. */
object BackendConfig {
    const val PRODUCTION_ENDPOINT = "https://ghaith-c77c2426ed4d.herokuapp.com/v1"
    val ENDPOINT: String get() = com.ghais.data.backend.BackendPlatform.apiEndpoint
    const val GOOGLE_WEB_CLIENT_ID = "674819710970-0daleci989c7jfgoppplppm29i8md1of.apps.googleusercontent.com"
    fun isConfigured(): Boolean = ENDPOINT == PRODUCTION_ENDPOINT ||
        ENDPOINT.startsWith("http://127.0.0.1:") || ENDPOINT.startsWith("http://localhost:") ||
        ENDPOINT.startsWith("http://10.0.2.2:") || ENDPOINT.startsWith("https://localhost:")
}
