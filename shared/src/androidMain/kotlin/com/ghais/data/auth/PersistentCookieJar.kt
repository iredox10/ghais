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
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) return@forEach
            decode(value)?.let { memory[key.removePrefix(KEY_PREFIX) + "|" + it.name] = it }
        }
        // Drop expired cookies loaded from disk.
        memory.values.removeAll { it.expiresAt < System.currentTimeMillis() }
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
        return memory.values.filter { it.expiresAt >= now && it.matches(url) }
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
