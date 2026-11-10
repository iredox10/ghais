package com.ghais.player

/** Each track owns its metadata; an absent artist clears the previous track's. */
internal data class PlaybackMetadataState(val title: String? = null, val artist: String? = null) {
    fun replace(title: String?, artist: String?) = PlaybackMetadataState(
        title?.takeIf { it.isNotBlank() }, artist?.takeIf { it.isNotBlank() },
    )
}
