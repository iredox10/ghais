package com.ghais.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.theme.GhaisNoir
import kotlinx.coroutines.launch

private val SpeedSteps = listOf(1f, 1.25f, 1.5f)

object AppSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        // Playback (local except speed, which drives the engine).
        var autoplayNext by remember { mutableStateOf(true) }
        var speedIndex by remember { mutableStateOf(0) }

        // Downloads & storage: Wi-Fi-only is a local preference; counts are live.
        var wifiOnly by remember { mutableStateOf(true) }
        val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }

        // Reminders: local placeholder state only — no scheduler infra wired.
        var dailyReminder by remember { mutableStateOf(false) }
        val reminderTimeLabel = "08:00"

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    SettingsTopBar(onBack = { navigator.pop() })
                }

                // 1. Playback
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "Playback")
                        NoirListRow(
                            icon = Icons.Filled.PlayArrow,
                            title = "Autoplay next",
                            subtitle = "Continue to the next surah automatically",
                            onClick = { autoplayNext = !autoplayNext },
                            trailing = {
                                NoirSwitch(
                                    checked = autoplayNext,
                                    onCheckedChange = { autoplayNext = it }
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
                            onClick = { wifiOnly = !wifiOnly },
                            trailing = {
                                NoirSwitch(
                                    checked = wifiOnly,
                                    onCheckedChange = { wifiOnly = it }
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

                // 3. Reminders & notifications (placeholder — no scheduling infra).
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "Reminders & notifications")
                        NoirListRow(
                            icon = Icons.Filled.Notifications,
                            title = "Daily reminder",
                            subtitle = "Placeholder — $reminderTimeLabel, scheduling not wired yet",
                            onClick = { dailyReminder = !dailyReminder },
                            trailing = {
                                NoirSwitch(
                                    checked = dailyReminder,
                                    onCheckedChange = { dailyReminder = it }
                                )
                            }
                        )
                    }
                }

                // 4. About
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NoirSectionHeader(label = "About")
                        NoirListRow(
                            icon = Icons.Filled.Info,
                            title = "Version",
                            subtitle = "Ghais 1.0",
                            chevron = false
                        )
                        NoirListRow(
                            icon = Icons.Filled.Star,
                            title = "Rate Ghais",
                            subtitle = "Not wired yet",
                            // No-op: store listing / review flow not wired.
                            onClick = { }
                        )
                        NoirListRow(
                            icon = Icons.Filled.Share,
                            title = "Share Ghais",
                            subtitle = "Not wired yet",
                            // No-op: share sheet not wired.
                            onClick = { }
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
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(
                text = "Settings",
                color = GhaisNoir.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Playback, downloads & more",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp
            )
        }
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
