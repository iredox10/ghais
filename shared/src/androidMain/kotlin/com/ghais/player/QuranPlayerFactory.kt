package com.ghais.player

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor

/**
 * Single place where the app's ExoPlayer network + buffering policy lives.
 *
 * Why this exists: recitation MP3s stream from several third-party hosts
 * (mp3quran CDN, everyayah single-origin, archive.org datanodes). Library
 * defaults (8s/8s timeouts, cross-protocol redirects disallowed, generic
 * user-agent, 50s prebuffer targets) made slow hosts hang in BUFFERING and
 * deprioritised our traffic. The values below fail fast and start fast:
 * - 10s connect/read timeouts so a dead host surfaces an error instead of
 *   an endless spinner (the engine's auto-skip path handles the rest).
 * - Cross-protocol redirects allowed: mp3quran/archive.org edges 301/302
 *   across http/https and hostnames; refusing them breaks playback.
 * - App user-agent: archive.org throttles generic clients.
 * - Streaming-first LoadControl: start after ~1s of audio is buffered even
 *   if the byte target is unmet (prioritize-time-over-size), cap the buffer
 *   at 30s so skipping a 2-hour surah doesn't download half of it first.
 *
 * Public (not internal) because the app module's playback service builds its
 * fallback player with the same policy.
 */
object QuranPlayerFactory {
    private const val USER_AGENT = "Ghaith/1 (ExoPlayer; +https://ghaith.app)"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    fun httpFactory(): DefaultHttpDataSource.Factory =
        DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(USER_AGENT)

    fun mediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val app = context.applicationContext
        // DefaultDataSource routes by scheme (file:// downloads go to the
        // file source, https:// streams to our tuned HTTP factory). Wiring
        // the HTTP factory directly would force LOCAL files through HTTP
        // and crash with ClassCastException (FileURLConnection is not an
        // HttpURLConnection) — breaking all offline playback.
        //
        // FLAG_DISABLE_ID3_METADATA: the recitation files carry promotional
        // ID3 artwork (the mp3quran.net logo) and tag titles baked in by the
        // source host. Stream tags override MediaItem-level metadata for the
        // media session, so the shade intermittently showed that raw logo
        // instead of our branded reciter art. We own all metadata (title,
        // artist, artworkUri) at the MediaItem level — ID3 parsing is dead
        // weight here, so it's disabled (seek tables like Xing are not
        // affected; duration/seeking keep working).
        return DefaultMediaSourceFactory(
            /* dataSourceFactory = */ DefaultDataSource.Factory(app, httpFactory()),
            /* extractorsFactory = */ DefaultExtractorsFactory()
                .setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA),
        )
    }

    fun loadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
}
