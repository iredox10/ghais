package com.ghais.ui.util

import com.ghais.data.seed.GhaisAssets

/**
 * Single policy for "is this URL a stand-in image rather than real artwork?".
 *
 * Every artwork surface in the app is supposed to fall back to a letter
 * monogram when there is no real photo. That fallback used to key purely on
 * `isNullOrBlank()`, so a reciter whose `photoUrl` happened to be a
 * placeholder service URL still loaded and displayed that placeholder image
 * instead of the monogram. Route every artwork decision through
 * [isPlaceholderImageUrl] so a placeholder is treated exactly like a blank.
 */
private val PLACEHOLDER_HOSTS = listOf(
    "placehold.co",
    "via.placeholder.com",
    "dummyimage.com",
    "dummyimage.de",
    "placekitten.com",
    "picsum.photos",
    "source.unsplash.com",
    "loremflickr.com",
    "fakeimg.pl",
    "placeimg.com",
    "placehold.it",
)

private val PLACEHOLDER_PATH_HINTS = listOf(
    "placeholder",
    "default-avatar",
    "no-image",
    "noimage",
    "blank",
)

/** True when [url] carries no real artwork and the caller should draw a monogram. */
fun isPlaceholderImageUrl(url: String?): Boolean {
    val clean = url?.trim().orEmpty()
    if (clean.isEmpty()) return true
    if (clean == GhaisAssets.LogoUrl) return true
    val lower = clean.lowercase()
    if (PLACEHOLDER_HOSTS.any { lower.contains(it) }) return true
    if (PLACEHOLDER_PATH_HINTS.any { lower.contains(it) }) return true
    return false
}

/** [url] when it points at real artwork, otherwise null. */
fun realImageUrlOrNull(url: String?): String? =
    url?.trim()?.takeIf { !isPlaceholderImageUrl(it) }
