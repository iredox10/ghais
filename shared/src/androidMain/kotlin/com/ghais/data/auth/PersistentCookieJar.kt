package com.ghais.data.auth

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * OkHttp [CookieJar] that persists Appwrite session cookies
 * (`a_session_*`) in SharedPreferences so login survives restarts.
 *
 * Without this the SDK sends no session cookie and every `account.get()`
 * returns 401 — signup looks like it "does nothing".
 */
internal class PersistentCookieJar(
    context: android.content.Context,
) : CookieJar {

    private val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
    private val memory = LinkedHashMap<String, Cookie>()

    init {
        val now = System.currentTimeMillis()
        val expiredKeys = mutableListOf<String>()
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) return@forEach
            val cookie = decode(value)
            if (cookie == null || cookie.expiresAt < now) {
                // Undecodable or expired entries can never be sent; drop the
                // expired ones from disk so they don't reload every launch.
                if (cookie != null) expiredKeys += key
                return@forEach
            }
            // Store under the full prefs key so it matches saveFromResponse
            // (KEY_PREFIX + host + "|" + name); the old code rebuilt a
            // different key here (host|name|name), leaving a stale duplicate
            // in memory that loadForRequest would return twice.
            memory[key] = cookie
        }
        if (expiredKeys.isNotEmpty()) {
            prefs.edit().apply { expiredKeys.forEach { remove(it) } }.apply()
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val editor = prefs.edit()
        for (cookie in cookies) {
            val key = KEY_PREFIX + url.host + "|" + cookie.name
            if (cookie.expiresAt < System.currentTimeMillis()) {
                memory.remove(key)
                editor.remove(key)
            } else {
                memory[key] = cookie
                editor.putString(key, encode(cookie))
            }
        }
        editor.apply()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        // Evict expired cookies so memory doesn't grow and disk doesn't
        // re-serve them after a restart (init only cleans once at load).
        val expiredKeys = memory.entries.filter { it.value.expiresAt < now }.map { it.key }
        if (expiredKeys.isNotEmpty()) {
            expiredKeys.forEach { memory.remove(it) }
            prefs.edit().apply { expiredKeys.forEach { remove(it) } }.apply()
        }
        return memory.values.filter { it.matches(url) }
    }

    /** Wipes all stored cookies (call on sign-out). */
    @Synchronized
    fun clear() {
        memory.clear()
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_PREFIX)) editor.remove(key)
        }
        editor.apply()
    }

    private fun encode(cookie: Cookie): String = listOf(
        cookie.name,
        cookie.value,
        cookie.domain,
        cookie.path,
        cookie.expiresAt.toString(),
        cookie.secure.toString(),
        cookie.httpOnly.toString(),
        cookie.hostOnly.toString(),
    ).joinToString("\u001F")

    private fun decode(raw: String): Cookie? {
        return try {
            val parts = raw.split("\u001F")
            if (parts.size != 8) return null
            Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .expiresAt(parts[4].toLong())
                .path(parts[3])
                .apply {
                    if (parts[7].toBoolean()) hostOnlyDomain(parts[2]) else domain(parts[2])
                    if (parts[5].toBoolean()) secure()
                    if (parts[6].toBoolean()) httpOnly()
                }
                .build()
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val PREFS = "ghais_appwrite_cookies"
        const val KEY_PREFIX = "cookie|"
    }
}
