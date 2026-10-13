package com.ghais.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ghais.domain.model.isFullSurah
import com.ghais.player.AudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Home-screen recitation widget (classic RemoteViews, min 2x2 resizable).
 *
 * No new dependencies: reads the same [AudioEngine.currentTrack] /
 * [AudioEngine.isPlaying] StateFlows the FGS mirrors into the notification
 * shade, and routes buttons to [QuranPlaybackService] via its exact
 * ACTION_* strings. Tap on the body opens [MainActivity].
 */
class QuranPlayerWidget : AppWidgetProvider() {

    companion object {
        /** In-process refresh ping (sent by [requestRefresh]). */
        const val ACTION_WIDGET_REFRESH = "com.ghais.android.action.WIDGET_REFRESH"

        private const val REQ_BODY = 100
        private const val REQ_TOGGLE = 101
        private const val REQ_NEXT = 102

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var collectJob: Job? = null

        /**
         * Entry point for service-side callbacks: mirrors how the FGS
         * re-posts the shade from observeEngine(). The service owner calls
         * this when track/playing state changes; safe to call from any
         * thread, never throws.
         */
        fun requestRefresh(context: Context) {
            try {
                val app = context.applicationContext ?: context
                val intent = Intent(app, QuranPlayerWidget::class.java)
                    .setAction(ACTION_WIDGET_REFRESH)
                app.sendBroadcast(intent)
            } catch (_: Exception) {
                try { refreshAll(context) } catch (_: Exception) { }
            }
        }

        fun refreshAll(context: Context) {
            try {
                val app = context.applicationContext ?: context
                val manager = AppWidgetManager.getInstance(app) ?: return
                val ids = try {
                    manager.getAppWidgetIds(
                        ComponentName(app, QuranPlayerWidget::class.java),
                    )
                } catch (_: Exception) {
                    return
                }
                if (ids.isEmpty()) return
                val views = buildViews(app)
                for (id in ids) {
                    try { manager.updateAppWidget(id, views) } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }

        internal fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_quran_player)
            try {
                val track = try { AudioEngine.currentTrack.value } catch (_: Exception) { null }
                val playing = try { AudioEngine.isPlaying.value } catch (_: Exception) { false }

                val title = if (track == null) {
                    "Ghaith"
                } else {
                    try {
                        QuranPlaybackService.displayTitle(
                            track.surahNameEn,
                            track.ayahNo,
                            track.isFullSurah,
                        )
                    } catch (_: Exception) {
                        "Ghaith"
                    }
                }
                val subtitle = try {
                    track?.reciterName?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.widget_empty_subtitle)
                } catch (_: Exception) {
                    track?.reciterName?.takeIf { it.isNotBlank() } ?: "Ghaith"
                }
                val monogram = try {
                    track?.reciterName?.firstOrNull { it.isLetterOrDigit() }
                        ?.uppercaseChar()?.toString() ?: "G"
                } catch (_: Exception) {
                    "G"
                }

                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_subtitle, subtitle)
                views.setTextViewText(R.id.widget_monogram, monogram)
                views.setImageViewResource(
                    R.id.widget_toggle,
                    if (playing) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                )

                // Tap body -> MainActivity (same flags as the shade's session activity).
                try {
                    val open = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    val openPi = PendingIntent.getActivity(
                        context,
                        REQ_BODY,
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openPi)
                } catch (_: Exception) { }

                // Buttons -> FGS explicit actions (exact strings, never invented).
                try {
                    views.setOnClickPendingIntent(
                        R.id.widget_toggle,
                        servicePendingIntent(context, QuranPlaybackService.ACTION_TOGGLE, REQ_TOGGLE),
                    )
                } catch (_: Exception) { }
                try {
                    views.setOnClickPendingIntent(
                        R.id.widget_next,
                        servicePendingIntent(context, QuranPlaybackService.ACTION_NEXT, REQ_NEXT),
                    )
                } catch (_: Exception) { }
            } catch (_: Exception) { }
            return views
        }

        private fun servicePendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, QuranPlaybackService::class.java).setAction(action)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return try {
                // API 26+ (minSdk 26): foreground-service launch, matching the
                // FGS start path; onStartCommand re-asserts foreground anyway.
                PendingIntent.getForegroundService(context, requestCode, intent, flags)
            } catch (_: Exception) {
                PendingIntent.getService(context, requestCode, intent, flags)
            }
        }

        /** Mirrors the FGS observeEngine() pattern: collect shared flows, re-render. */
        internal fun ensureCollecting(context: Context) {
            try {
                if (collectJob?.isActive == true) return
                val app = context.applicationContext ?: context
                collectJob = widgetScope.launch {
                    try {
                        combine(
                            AudioEngine.currentTrack,
                            AudioEngine.isPlaying,
                        ) { _, _ -> Unit }.collect {
                            try { refreshAll(app) } catch (_: Exception) { }
                        }
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }

        internal fun stopCollecting() {
            try { collectJob?.cancel() } catch (_: Exception) { }
            collectJob = null
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        try { ensureCollecting(context) } catch (_: Exception) { }
        try {
            val views = buildViews(context)
            for (id in appWidgetIds) {
                try { appWidgetManager.updateAppWidget(id, views) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            if (intent?.action == ACTION_WIDGET_REFRESH) {
                try { refreshAll(context) } catch (_: Exception) { }
                return
            }
        } catch (_: Exception) { }
        try {
            super.onReceive(context, intent)
        } catch (_: Exception) { }
        // System APPWIDGET_UPDATE lands here via super.onUpdate, but re-render
        // anyway so a stale snapshot never sticks when the flow collector has
        // not started yet (e.g. right after process death).
        try {
            if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                try { ensureCollecting(context) } catch (_: Exception) { }
                try { refreshAll(context) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    override fun onEnabled(context: Context) {
        try { ensureCollecting(context) } catch (_: Exception) { }
    }

    override fun onDisabled(context: Context) {
        try { stopCollecting() } catch (_: Exception) { }
    }
}
