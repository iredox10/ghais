package com.quranify.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.player.AudioEngine
import com.quranify.player.QuranDownloads
import kotlinx.coroutines.launch

private val BlackGlassBg = Color(0xFF000000)
private val CardBg = Color.White.copy(alpha = 0.05f)
private val CardBorder = Color.White.copy(alpha = 0.08f)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val Hairline = Color.White.copy(alpha = 0.08f)

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackGlassBg),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SettingsTopBar(onBack = { navigator.pop() })
            }

            // 1. Playback
            item {
                SettingsSectionHeader(title = "Playback")
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Filled.PlayArrow,
                        title = "Autoplay next",
                        subtitle = "Continue to the next surah automatically",
                        checked = autoplayNext,
                        onCheckedChange = { autoplayNext = it }
                    )
                    SettingsDivider()
                    val speed = SpeedSteps[speedIndex]
                    SettingsActionRow(
                        icon = Icons.Filled.GraphicEq,
                        title = "Default speed",
                        subtitle = "Tap to cycle 1x / 1.25x / 1.5x",
                        trailing = formatSpeed(speed),
                        onClick = {
                            speedIndex = (speedIndex + 1) % SpeedSteps.size
                            AudioEngine.setSpeed(SpeedSteps[speedIndex])
                        }
                    )
                    // Crossfade omitted: AudioEngine exposes no crossfade/gapless control.
                }
            }

            // 2. Downloads & storage
            item {
                SettingsSectionHeader(title = "Downloads & storage")
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Filled.Wifi,
                        title = "Wi-Fi only",
                        subtitle = "Download surahs on Wi-Fi only",
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it }
                    )
                    SettingsDivider()
                    SettingsStaticRow(
                        icon = Icons.Filled.Storage,
                        title = "Downloaded",
                        subtitle = "${downloadedKeys.size} surahs • ${formatStorageBytes(storageSizeBytes)}"
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.DeleteOutline,
                        title = "Clear cache",
                        subtitle = "Remove all downloaded audio",
                        trailing = formatStorageBytes(storageSizeBytes),
                        onClick = {
                            scope.launch { QuranDownloads.clearAll() }
                        }
                    )
                }
            }

            // 3. Reminders & notifications (placeholder — no scheduling infra).
            item {
                SettingsSectionHeader(title = "Reminders & notifications")
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Filled.Notifications,
                        title = "Daily reminder",
                        subtitle = "Placeholder — $reminderTimeLabel, scheduling not wired yet",
                        checked = dailyReminder,
                        onCheckedChange = { dailyReminder = it }
                    )
                }
            }

            // 4. About
            item {
                SettingsSectionHeader(title = "About")
                SettingsCard {
                    SettingsStaticRow(
                        icon = Icons.Filled.Info,
                        title = "Version",
                        subtitle = "Ghais 1.0"
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.Star,
                        title = "Rate Ghais",
                        subtitle = "Not wired yet",
                        trailing = null,
                        // No-op: store listing / review flow not wired.
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Filled.Share,
                        title = "Share Ghais",
                        subtitle = "Not wired yet",
                        trailing = null,
                        // No-op: share sheet not wired.
                        onClick = { }
                    )
                    SettingsDivider()
                    SettingsStaticRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Attribution",
                        subtitle = "Audio: Mixkit • Text: Wikimedia / Tanzil"
                    )
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
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, CardBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "Settings",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.White.copy(alpha = 0.35f),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Hairline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = trailing,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsStaticRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsRowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline)
    )
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
