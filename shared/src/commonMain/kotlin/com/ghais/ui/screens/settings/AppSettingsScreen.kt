package com.ghais.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.PlaybackSettings
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.ScheduleEngine
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.TranslationCatalog
import com.ghais.data.repository.TranslationPrefs
import com.ghais.data.share.ShareSheet
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.player.TranslationPickerSheet
import kotlinx.coroutines.launch

private val SpeedSteps = listOf(1f, 1.25f, 1.5f)

// Settings-owned daily-reminder schedule id. Fixed (not SchedulesStore.newId())
// so enable/disable/update always address the same row: enable creates it (or
// re-enables with the stored time), disable removes it.
private const val DailyReminderScheduleId = "settings-daily-reminder"

// Play Store listing used for the Share row. Id follows the androidApp
// namespace (`com.ghais.android`); the app is not published yet, so this is
// the future listing URL, shared as text (ShareSheet, Android live / iOS stub).
private const val GhaithStoreUrl =
    "https://play.google.com/store/apps/details?id=com.ghais.android"

object AppSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        // Playback: autoplay persists via PlaybackSettings (TranslationPrefs-style
        // Settings persistence — data/repository/PlaybackSettings.kt); speed
        // stays session-local and drives the engine.
        val autoplayNext by PlaybackSettings.autoplayNext.collectAsState()
        var speedIndex by remember { mutableStateOf(0) }

        // Downloads & storage: Wi-Fi-only persists via PlaybackSettings and is
        // enforced at the network edge in QuranDownloads (android/ios actuals
        // read the same `playback_wifi_only` key); counts are live.
        val wifiOnly by PlaybackSettings.wifiOnly.collectAsState()
        val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }

        // Reminders: backed by a SchedulesStore daily RecitationSchedule under
        // DailyReminderScheduleId — enable creates/updates it, disable removes
        // it. Subtitle shows the stored time.
        val schedules by SchedulesStore.schedules.collectAsState()
        val reminderSchedule = remember(schedules) {
            schedules.firstOrNull { it.id == DailyReminderScheduleId }
        }
        val dailyReminder = reminderSchedule != null && reminderSchedule.enabled
        val reminderTimeLabel = reminderSchedule?.let {
            "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
        } ?: "08:00"
        val followedSlugs by FollowStore.followedSlugs.collectAsState()
        var exactAllowed by remember { mutableStateOf(true) }
        // Re-check on toggle (user may have just come back from the system
        // grant screen) — SchedulesProtonSection banner pattern, minimal form.
        LaunchedEffect(dailyReminder) {
            exactAllowed = ScheduleEngine.canScheduleExactAlarms()
        }

        // Translation: live from TranslationPrefs, catalog-resolved for display.
        // The row is a second entry point to the ONE picker (shared with the
        // player) so users can switch edition without leaving Settings.
        var showTranslationPicker by remember { mutableStateOf(false) }
        val selectedEditionId by TranslationPrefs.selectedEdition.collectAsState()
        val translationEdition = remember(selectedEditionId) {
            TranslationCatalog.byId(selectedEditionId)
        }
        val translationTitle = translationEdition?.let { edition ->
            val name = edition.englishName.trim().ifBlank { edition.languageName.trim() }
            val flag = edition.countryFlag.trim()
            if (name.isEmpty()) "Translation" else if (flag.isEmpty()) name else "$flag $name"
        } ?: "Translation"
        val translationSubtitle = translationEdition
            ?.languageName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { "$it • Tap to change" }
            ?: "Tap to choose the translation shown while listening"

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // Standard pushed header: back well + left-aligned title,
                    // plus a Share well wired to the screen's existing
                    // "Share Ghaith" ShareSheet handler (same copy, one
                    // entry point — the row below stays as the label-able
                    // fallback).
                    NoirNavBar(
                        title = "Settings",
                        backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onBack = { navigator.pop() },
                        actions = {
                            NoirNavButton(
                                icon = Icons.Filled.Share,
                                contentDescription = "Share",
                                onClick = {
                                    if (ShareSheet.isAvailable()) {
                                        ShareSheet.shareText(
                                            title = "Ghaith — Daily Quran",
                                            text = "Listen to the Quran every day with Ghaith: $GhaithStoreUrl"
                                        )
                                    }
                                }
                            )
                        },
                        titleAlignLeft = true
                    )
                }

                // 1. Playback
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "Playback")
                        NoirListRow(
                            icon = Icons.Filled.PlayArrow,
                            title = "Autoplay next",
                            subtitle = "Continue to the next surah automatically",
                            onClick = { PlaybackSettings.setAutoplayNext(!autoplayNext) },
                            trailing = {
                                NoirSwitch(
                                    checked = autoplayNext,
                                    onCheckedChange = { PlaybackSettings.setAutoplayNext(it) }
                                )
                            }
                        )
                        val speed = SpeedSteps[speedIndex]
                        NoirListRow(
                            icon = Icons.Filled.GraphicEq,
                            title = "Default speed",
                            subtitle = "Tap to cycle 1x / 1.25x / 1.5x",
                            onClick = {
                                speedIndex = (speedIndex + 1) % SpeedSteps.size
                                AudioEngine.setSpeed(SpeedSteps[speedIndex])
                            },
                            trailing = { NoirStatChip(text = formatSpeed(speed)) }
                        )
                        // Crossfade omitted: AudioEngine exposes no crossfade/gapless control.
                    }
                }

                // 2. Downloads & storage
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "Downloads & storage")
                        NoirListRow(
                            icon = Icons.Filled.Wifi,
                            title = "Wi-Fi only",
                            subtitle = "Download surahs on Wi-Fi only",
                            onClick = { PlaybackSettings.setWifiOnly(!wifiOnly) },
                            trailing = {
                                NoirSwitch(
                                    checked = wifiOnly,
                                    onCheckedChange = { PlaybackSettings.setWifiOnly(it) }
                                )
                            }
                        )
                        NoirListRow(
                            icon = Icons.Filled.Storage,
                            title = "Downloaded",
                            subtitle = "${downloadedKeys.size} surahs • ${formatStorageBytes(storageSizeBytes)}",
                            chevron = false
                        )
                        NoirListRow(
                            icon = Icons.Filled.DeleteOutline,
                            title = "Clear cache",
                            subtitle = "Remove all downloaded audio",
                            onClick = {
                                scope.launch { QuranDownloads.clearAll() }
                            },
                            trailing = { NoirStatChip(text = formatStorageBytes(storageSizeBytes)) }
                        )
                    }
                }

                // 3. Reminders & notifications.
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "Reminders & notifications")
                        NoirListRow(
                            icon = Icons.Filled.Notifications,
                            title = "Daily reminder",
                            subtitle = if (dailyReminder) "Daily at $reminderTimeLabel"
                            else "Off — tap to enable at 08:00",
                            onClick = { setDailyReminder(!dailyReminder, followedSlugs) },
                            trailing = {
                                NoirSwitch(
                                    checked = dailyReminder,
                                    onCheckedChange = { setDailyReminder(it, followedSlugs) }
                                )
                            }
                        )
                        // Same "Exact alarms are off" guidance path as
                        // SchedulesProtonSection, minus the card chrome: the
                        // toggle keeps working, but without the grant the
                        // reminder may arrive late or not at all.
                        if (dailyReminder && !exactAllowed) {
                            NoirListRow(
                                icon = Icons.Filled.AlarmOn,
                                title = "Exact alarms are off",
                                subtitle = "Reminders may arrive late — tap to allow",
                                onClick = { ScheduleEngine.openExactAlarmSettings() }
                            )
                        }
                    }
                }

                // 4. Reading / translation
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "Translation")
                        NoirListRow(
                            icon = Icons.Filled.Translate,
                            title = translationTitle,
                            subtitle = translationSubtitle,
                            onClick = { showTranslationPicker = true }
                        )
                    }
                }

                // 5. About
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "About")
                        NoirListRow(
                            icon = Icons.Filled.Info,
                            title = "Version",
                            subtitle = "Ghaith 1.0",
                            chevron = false
                        )
                        NoirListRow(
                            icon = Icons.Filled.Star,
                            title = "Rate Ghaith",
                            subtitle = "Not wired yet",
                            // No-op: no common URL-opener expect exists (only
                            // ShareSheet.shareText plus the auth-scoped
                            // CustomTabs fallback in GoogleWebAuth), so the
                            // Play Store listing cannot be opened from
                            // commonMain without a new platform API — left for
                            // a follow-up; see the task report.
                            onClick = { }
                        )
                        NoirListRow(
                            icon = Icons.Filled.Share,
                            title = "Share Ghaith",
                            subtitle = "Tell a friend about Ghaith",
                            // TafseerSheet.kt:293 pattern; isAvailable() guards
                            // the iOS stub (MyRoutinesScreen pattern).
                            onClick = {
                                if (ShareSheet.isAvailable()) {
                                    ShareSheet.shareText(
                                        title = "Ghaith — Daily Quran",
                                        text = "Listen to the Quran every day with Ghaith: $GhaithStoreUrl"
                                    )
                                }
                            }
                        )
                        NoirListRow(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Attribution",
                            subtitle = "Audio: Mixkit • Text: Wikimedia / Tanzil",
                            chevron = false
                        )
                    }
                }
            }

            // Shared picker, mounted here rather than duplicated: identical
            // sheet, search and country grouping the player uses. Selection
            // persists via TranslationPrefs and invalidates the ayah caches so
            // every surface re-resolves in the new edition.
            TranslationPickerSheet(
                visible = showTranslationPicker,
                selectedEditionId = selectedEditionId,
                onSelect = { edition ->
                    TranslationPrefs.setEdition(edition.id)
                    QuranAyahRepository.onTranslationChanged()
                    showTranslationPicker = false
                },
                onDismiss = { showTranslationPicker = false }
            )
        }
    }
}

/**
 * Enables/disables the settings-owned daily reminder.
 *
 * Enable creates (or re-enables, preserving the stored time/reciter) a
 * reminder-only (`autoStart = false`) [RecitationSchedule] under
 * [DailyReminderScheduleId]; disable removes it. [SchedulesStore] persists and
 * reprograms OS alarms on every mutation. Mirrors the SchedulesProtonSection
 * save path: when exact alarms are denied, the toggle still sticks and the
 * user is guided to the grant screen.
 */
private fun setDailyReminder(enabled: Boolean, followedSlugs: Set<String>) {
    if (!enabled) {
        SchedulesStore.remove(DailyReminderScheduleId)
        return
    }
    val existing = SchedulesStore.get(DailyReminderScheduleId)
    SchedulesStore.add(
        RecitationSchedule(
            id = DailyReminderScheduleId,
            hour = existing?.hour ?: 8,
            minute = existing?.minute ?: 0,
            reciterSlug = existing?.reciterSlug
                ?: followedSlugs.firstOrNull() ?: "mishary",
            fromSurah = existing?.fromSurah ?: 1,
            toSurah = existing?.toSurah ?: 5,
            durationMin = existing?.durationMin,
            autoStart = existing?.autoStart ?: false,
            enabled = true
        )
    )
    if (!ScheduleEngine.canScheduleExactAlarms()) {
        ScheduleEngine.openExactAlarmSettings()
    }
}

private fun formatSpeed(speed: Float): String {
    val label = if (speed == speed.toLong().toFloat()) {
        speed.toLong().toString()
    } else {
        speed.toString()
    }
    return "${label}x"
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${oneDecimal(kb)} KB"
    return "${oneDecimal(kb / 1024.0)} MB"
}

private fun oneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}
