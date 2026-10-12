package com.ghais.ui.navigation

import cafe.adriel.voyager.core.screen.Screen
import com.ghais.ui.screens.memorization.MemorizationScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.search.SearchScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Deep-link / app-link model for Ghaith.
 *
 * Accepted URIs (see [parseAppLink]):
 * - `quranify://surah/2`, `quranify://ayah/2/255`, `quranify://reciter/mishary`,
 *   `quranify://home`, `quranify://hifz`, `quranify://search`
 * - `https://ghaith.app/surah/2`, `https://ghaith.app/ayah/2/255`,
 *   `https://ghaith.app/reciter/mishary` (plus `/home`, `/hifz`, `/search`)
 *
 * App Links note (server-side, NOT in this repo): for the `https://ghaith.app`
 * variants to auto-verify (`android:autoVerify="true"` in the manifest), the
 * site must serve `https://ghaith.app/.well-known/assetlinks.json` containing
 * a `delegate_permission/common.getUriData` statement for package
 * `com.ghais.android` with this app's release SHA-256 cert fingerprint.
 * Without that file the https links still open a chooser / the browser; the
 * `quranify://` scheme links always resolve directly to MainActivity.
 */
const val APP_LINK_HTTPS_HOST = "ghaith.app"

/** Lowest valid surah id (Al-Fatihah). */
const val MIN_SURAH_ID = 1

/** Highest valid surah id (An-Nas). */
const val MAX_SURAH_ID = 114

sealed interface AppLink {
    data class Surah(val surahId: Int) : AppLink
    data class Ayah(val surahId: Int, val ayahNo: Int) : AppLink
    data class Reciter(val slug: String) : AppLink
    data object Home : AppLink
    data object Hifz : AppLink
    data object Search : AppLink
}

/**
 * Parses a deep-link URI into an [AppLink], or `null` when the URI is not a
 * supported app link.
 *
 * Rules: `surahId` must be numeric and is clamped to 1..114; `ayahNo` must be
 * numeric and >= 1 (else `null`); reciter slug must be non-blank; query
 * parameters and fragments are ignored; trailing slashes are tolerated;
 * scheme/route/host matching is case-insensitive. Plain `http` is rejected —
 * only `https` counts as an app link.
 */
fun parseAppLink(uri: String): AppLink? {
    val raw = uri.trim()
    if (raw.isEmpty()) return null
    val schemeEnd = raw.indexOf("://")
    if (schemeEnd <= 0) return null
    val scheme = raw.substring(0, schemeEnd).lowercase()
    val afterScheme = raw.substring(schemeEnd + 3)
    if (afterScheme.isEmpty()) return null
    // Query + fragment never carry routing info.
    val pathPart = afterScheme.substringBefore('#').substringBefore('?')
    return when (scheme) {
        "quranify" -> {
            // quranify://<route>/<args...>: the host slot holds the route name.
            val segments = pathPart.split('/').filter { it.isNotEmpty() }
            if (segments.isEmpty()) return null
            route(segments[0], segments.drop(1))
        }
        "https" -> {
            val slash = pathPart.indexOf('/')
            val host = (if (slash < 0) pathPart else pathPart.substring(0, slash)).lowercase()
            if (host != APP_LINK_HTTPS_HOST) return null
            val segments = if (slash < 0) {
                emptyList()
            } else {
                pathPart.substring(slash + 1).split('/').filter { it.isNotEmpty() }
            }
            if (segments.isEmpty()) return null
            route(segments[0], segments.drop(1))
        }
        else -> null
    }
}

private fun route(first: String, args: List<String>): AppLink? = when (first.lowercase()) {
    "surah" -> {
        val id = args.firstOrNull()?.toIntOrNull() ?: return null
        AppLink.Surah(id.coerceIn(MIN_SURAH_ID, MAX_SURAH_ID))
    }
    "ayah" -> {
        val surahId = args.getOrNull(0)?.toIntOrNull() ?: return null
        val ayahNo = args.getOrNull(1)?.toIntOrNull() ?: return null
        if (ayahNo < 1) return null
        AppLink.Ayah(surahId.coerceIn(MIN_SURAH_ID, MAX_SURAH_ID), ayahNo)
    }
    "reciter" -> {
        val slug = args.firstOrNull()?.trim().orEmpty()
        if (slug.isEmpty()) return null
        AppLink.Reciter(slug)
    }
    "home" -> AppLink.Home
    "hifz" -> AppLink.Hifz
    "search" -> AppLink.Search
    else -> null
}

/**
 * Maps an [AppLink] to the Voyager [Screen] the root navigator should push.
 *
 * - [AppLink.Surah] -> `SurahDetailScreen(surahId)`
 *   (`SurahDetailScreen.kt:67`: `data class SurahDetailScreen(val surahId: Int)`).
 * - [AppLink.Ayah] -> `SurahDetailScreen(surahId)` as well: scroll-to-ayah is
 *   UNAVAILABLE — SurahDetailScreen takes only `surahId` and owns no list
 *   state / initial-ayah parameter, so the link lands at the top of the surah.
 * - [AppLink.Reciter] -> `ReciterProfileScreen(slug)`
 *   (`ReciterProfileScreen.kt:121`: `data class ReciterProfileScreen(val reciterSlug: String)`).
 * - [AppLink.Hifz] -> `MemorizationScreen` (a Voyager `Tab`, which is a
 *   `Screen`, so it can be pushed; renders fullscreen above the tab dock).
 * - [AppLink.Search] -> `SearchScreen()`.
 * - [AppLink.Home] -> `null`: nothing to push, the consumer should pop to root.
 */
fun AppLink.toScreen(): Screen? = when (this) {
    is AppLink.Surah -> SurahDetailScreen(surahId)
    is AppLink.Ayah -> SurahDetailScreen(surahId)
    is AppLink.Reciter -> ReciterProfileScreen(slug)
    AppLink.Hifz -> MemorizationScreen
    AppLink.Search -> SearchScreen()
    AppLink.Home -> null
}

/**
 * Handoff for links parsed on the Android host: MainActivity posts the parsed
 * [AppLink] here from `onCreate` (cold start) and `onNewIntent` (singleTask
 * re-delivery); the Compose side drains it next to the root navigator, e.g.
 * inside `MainScreen.Content` where `rootNavigator` is in scope:
 *
 * ```
 * val pendingLink by DeepLinkBus.pending.collectAsState()
 * LaunchedEffect(pendingLink) {
 *     val link = DeepLinkBus.consume() ?: return@LaunchedEffect
 *     val screen = link.toScreen()
 *     if (screen != null) rootNavigator.push(screen) else rootNavigator.popUntilRoot()
 * }
 * ```
 *
 * StateFlow replays the latest value, so links arriving before first
 * composition (cold start) are not lost. Only place past the auth/onboarding
 * gate (i.e. where MainScreen is shown), otherwise a link could push under the
 * login gate.
 */
object DeepLinkBus {
    private val _pending = MutableStateFlow<AppLink?>(null)
    val pending: StateFlow<AppLink?> = _pending

    fun post(link: AppLink) {
        _pending.value = link
    }

    /** Returns the pending link and clears it; `null` when there is none. */
    fun consume(): AppLink? {
        val link = _pending.value
        if (link != null) _pending.value = null
        return link
    }
}
