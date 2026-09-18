package com.quranify.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.theme.QuranifyColors

// -----------------------------------------------------------------------------
// Design System Tokens: Black & White + Trending Purple Theme
// -----------------------------------------------------------------------------
private val PitchBlackBg = Color(0xFF0B0C0E)              // Pitch Black Canvas
private val ObsidianGlassCard = Color(0xFF121418)         // Obsidian Glass Container
private val TrendingPurpleAccent = Color(0xFFA855F7)      // Trending Purple Accent
private val ElectricViolet = Color(0xFF8B5CF6)            // Electric Violet Gradient
private val NeonLilac = Color(0xFFC084FC)                 // Neon Lilac Highlight
private val PurpleGlowBorder = Color(0x33A855F7)          // 20% alpha purple border
private val CardBorderSubtle = Color.White.copy(alpha = 0.08f) // 8% alpha subtle white border
private val TextWhitePrimary = Color(0xFFFFFFFF)          // Pure White
private val TextMutedSecondary = Color(0xFF8E989C)        // Muted Grey/White Subtitle
private val SubtleDivider = Color.White.copy(alpha = 0.06f) // Divider hairline

object SettingsScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 3u,
                title = "Settings",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        // State holders for interactive preferences
        var selectedBitrate by remember { mutableStateOf("High (320kbps)") }
        var selectedReciter by remember { mutableStateOf("Mishary Rashid Alafasy") }
        var isReciterPickerOpen by remember { mutableStateOf(false) }

        var darkModeEnabled by remember { mutableStateOf(true) }
        var gaplessPlayback by remember { mutableStateOf(true) }
        var loudnessNormalization by remember { mutableStateOf(true) }
        var wifiOnlyDownloads by remember { mutableStateOf(true) }
        var dailyDhikrReminder by remember { mutableStateOf(true) }
        var fridayKahfAlert by remember { mutableStateOf(true) }
        var prayerRecitationAlert by remember { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBg),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp)
        ) {
            // 1. Screen Title & Ambient Subtitle
            item {
                Column(modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Settings",
                            color = TextWhitePrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TrendingPurpleAccent.copy(alpha = 0.15f))
                                .border(0.5.dp, TrendingPurpleAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = TrendingPurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize audio engine, reciters, appearance & storage",
                        color = TextMutedSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // 2. Profile / Account Header with Avatar & Purple Rim
            item {
                ProfileHeaderCard()
            }

            // 3. Section: Audio Engine & Streaming Bitrate
            item {
                SettingsSectionHeader(title = "Audio Engine & Streaming Bitrate", icon = Icons.Filled.Headphones)
                SettingsCardContainer {
                    // Bitrate Selector Chips
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Streaming Bitrate",
                                color = TextWhitePrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = selectedBitrate,
                                color = TrendingPurpleAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val bitrateOptions = listOf(
                                "High (320kbps)" to "Studio AAC",
                                "Lossless" to "24-bit FLAC",
                                "Normal" to "128kbps"
                            )
                            bitrateOptions.forEach { (name, format) ->
                                val isSelected = selectedBitrate == name || (name == "Lossless" && selectedBitrate.contains("Lossless"))
                                val pillBg = if (isSelected) TrendingPurpleAccent.copy(alpha = 0.16f) else Color(0xFF171A1F)
                                val pillBorder = if (isSelected) TrendingPurpleAccent else CardBorderSubtle
                                val textColor = if (isSelected) TextWhitePrimary else TextMutedSecondary

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(pillBg)
                                        .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedBitrate = name }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = name,
                                            color = textColor,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = format,
                                            color = if (isSelected) TrendingPurpleAccent else TextMutedSecondary.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SubtleDividerLine()

                    // Audio Engine info row
                    SettingsValueRow(
                        icon = Icons.Filled.GraphicEq,
                        title = "Audio Engine",
                        subtitle = "Multiplatform low-latency Core Audio",
                        value = "v2.19 Hi-Res"
                    )

                    SubtleDividerLine()

                    // Gapless Playback Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Gapless Ayah Playback",
                        subtitle = "Smooth zero-latency transition between verses",
                        checked = gaplessPlayback,
                        onCheckedChange = { gaplessPlayback = it }
                    )

                    SubtleDividerLine()

                    // Loudness Normalization Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.Headphones,
                        title = "Loudness Normalization",
                        subtitle = "Harmonize volume levels across different reciters",
                        checked = loudnessNormalization,
                        onCheckedChange = { loudnessNormalization = it }
                    )
                }
            }

            // 4. Section: Default Reciter Preference
            item {
                SettingsSectionHeader(title = "Reciter Preference", icon = Icons.Filled.Mic)
                SettingsCardContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isReciterPickerOpen = !isReciterPickerOpen }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TrendingPurpleAccent.copy(alpha = 0.14f))
                                .border(1.dp, TrendingPurpleAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = TrendingPurpleAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Default Reciter",
                                    color = TextWhitePrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TrendingPurpleAccent.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "MURATTAL",
                                        color = TrendingPurpleAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "$selectedReciter • Hafs 'an 'Asim",
                                color = TextMutedSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Expand reciter picker",
                            tint = TextMutedSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reciter Quick Picker Expandable List
                    AnimatedVisibility(visible = isReciterPickerOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1115))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            val availableReciters = listOf(
                                "Mishary Rashid Alafasy",
                                "Abdul Basit Abdul Samad",
                                "Mahmoud Khalil Al-Husary",
                                "Maher Al-Muaiqly",
                                "Abu Bakr Al-Shatri"
                            )
                            availableReciters.forEach { reciter ->
                                val isSelected = selectedReciter == reciter
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedReciter = reciter
                                            isReciterPickerOpen = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = reciter,
                                        color = if (isSelected) TrendingPurpleAccent else TextWhitePrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = TrendingPurpleAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SubtleDividerLine()

                    SettingsValueRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Playback Speed",
                        subtitle = "Default ayah recitation tempo",
                        value = "1.0x (Normal)"
                    )
                }
            }

            // 5. Section: Appearance & Themes
            item {
                SettingsSectionHeader(title = "Appearance & Theme", icon = Icons.Filled.Palette)
                SettingsCardContainer {
                    // Dark Obsidian Mode Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.DarkMode,
                        title = "Dark Obsidian Mode",
                        subtitle = "Pitch Black #0B0C0E OLED background",
                        checked = darkModeEnabled,
                        onCheckedChange = { darkModeEnabled = it }
                    )

                    SubtleDividerLine()

                    // Trending Purple Accent Indicator Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TrendingPurpleAccent.copy(alpha = 0.16f))
                                .border(1.dp, TrendingPurpleAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = TrendingPurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accent Color",
                                color = TextWhitePrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Trending Purple (#A855F7)",
                                color = TextMutedSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Glowing Trending Purple Color Indicator Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(TrendingPurpleAccent.copy(alpha = 0.14f))
                                .border(1.dp, TrendingPurpleAccent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(TrendingPurpleAccent)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                            Text(
                                text = "Active",
                                color = TrendingPurpleAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    SubtleDividerLine()

                    // Palette Preview Swatches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Theme Palette",
                            color = TextMutedSecondary,
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val swatches = listOf(
                                PitchBlackBg,
                                ObsidianGlassCard,
                                Color.White,
                                TrendingPurpleAccent,
                                ElectricViolet,
                                NeonLilac
                            )
                            swatches.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                )
                            }
                        }
                    }

                    SubtleDividerLine()

                    SettingsValueRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Quranic Calligraphy Script",
                        subtitle = "Medina Mushaf rendering typography",
                        value = "Uthmani Hafs v2"
                    )
                }
            }

            // 6. Section: Storage & Offline Downloads
            item {
                SettingsSectionHeader(title = "Storage & Offline Downloads", icon = Icons.Filled.Storage)
                SettingsCardContainer {
                    // Storage Used Info & Visual Bar
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TrendingPurpleAccent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = TrendingPurpleAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Storage Used",
                                        color = TextWhitePrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "14 Surahs cached offline",
                                        color = TextMutedSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = "1.2 GB of 64 GB",
                                color = TextWhitePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar: Obsidian Track + Trending Purple Progress Fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF22262B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.20f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                TrendingPurpleAccent,
                                                NeonLilac
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    SubtleDividerLine()

                    // WiFi Only Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.Wifi,
                        title = "Download via Wi-Fi Only",
                        subtitle = "Prevent mobile cellular data usage for recitations",
                        checked = wifiOnlyDownloads,
                        onCheckedChange = { wifiOnlyDownloads = it }
                    )

                    SubtleDividerLine()

                    // Clear Offline Cache Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Trigger cache cleanup */ }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22171B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear Cache",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Clear Audio Cache",
                                    color = TextWhitePrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Frees temporary streaming buffer",
                                    color = TextMutedSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22262B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "1.2 GB",
                                color = TextMutedSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 7. Section: Notifications & Reminders
            item {
                SettingsSectionHeader(title = "Notifications & Reminders", icon = Icons.Filled.Notifications)
                SettingsCardContainer {
                    // Daily Dhikr Reminder Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.Notifications,
                        title = "Daily Dhikr Reminder",
                        subtitle = "Morning & Evening Adhkar spiritual alerts",
                        checked = dailyDhikrReminder,
                        onCheckedChange = { dailyDhikrReminder = it }
                    )

                    SubtleDividerLine()

                    // Friday Kahf Alert Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Friday Surah Al-Kahf Alert",
                        subtitle = "Reminder every Friday morning at 09:00 AM",
                        checked = fridayKahfAlert,
                        onCheckedChange = { fridayKahfAlert = it }
                    )

                    SubtleDividerLine()

                    // Prayer Time Recitation Prompt Toggle
                    SettingsSwitchRow(
                        icon = Icons.Filled.CloudDone,
                        title = "Prayer Recitation Suggestions",
                        subtitle = "Curated verses following Salah prayers",
                        checked = prayerRecitationAlert,
                        onCheckedChange = { prayerRecitationAlert = it }
                    )
                }
            }

            // 8. Section: About Quranify
            item {
                SettingsSectionHeader(title = "About Quranify", icon = Icons.Filled.Info)
                SettingsCardContainer {
                    SettingsValueRow(
                        icon = Icons.Filled.Info,
                        title = "Version",
                        subtitle = "Production release channel",
                        value = "v1.0.0 (Build 2026.1)"
                    )

                    SubtleDividerLine()

                    SettingsValueRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Attributions & Sources",
                        subtitle = "Quran.com, EveryAyah Audio & Tanzil Project",
                        value = "Verified"
                    )

                    SubtleDividerLine()

                    SettingsValueRow(
                        icon = Icons.Filled.Favorite,
                        title = "Open Source License",
                        subtitle = "Apache 2.0 • 100% Free & Ad-Free forever",
                        value = "GitHub"
                    )
                }
            }

            // 9. Spiritual Footer
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with devotion for the Ummah worldwide ☪",
                        color = TextMutedSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quranify • Modern Audio Experience",
                        color = TrendingPurpleAccent.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component Primitives
// -----------------------------------------------------------------------------

@Composable
private fun ProfileHeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianGlassCard)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            PurpleGlowBorder,
                            CardBorderSubtle
                        )
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Glowing Trending Purple Rim
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    TrendingPurpleAccent,
                                    NeonLilac,
                                    ElectricViolet,
                                    TrendingPurpleAccent
                                )
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF2B1842),
                                    Color(0xFF14111C)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User Avatar",
                        tint = TrendingPurpleAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Believer Account",
                        color = TextWhitePrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Cloud Synced Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TrendingPurpleAccent.copy(alpha = 0.16f))
                            .border(0.5.dp, TrendingPurpleAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            color = TrendingPurpleAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Personalized audio, bookmarks & Khatma",
                    color = TextMutedSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 10.dp, top = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TrendingPurpleAccent,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = title.uppercase(),
            color = TrendingPurpleAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCardContainer(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianGlassCard)
            .border(
                BorderStroke(1.dp, CardBorderSubtle),
                shape = RoundedCornerShape(20.dp)
            )
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
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (checked) TrendingPurpleAccent.copy(alpha = 0.14f) else Color(0xFF191C20)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) TrendingPurpleAccent else TextMutedSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhitePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextMutedSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trending Purple switch: active track Trending Purple, thumb pure white
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrendingPurpleAccent,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color(0xFF8E989C),
                uncheckedTrackColor = Color(0xFF1E2325),
                uncheckedBorderColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF191C20)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMutedSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhitePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextMutedSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1D22))
                    .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = value,
                    color = TextWhitePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SubtleDividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SubtleDivider)
    )
}
