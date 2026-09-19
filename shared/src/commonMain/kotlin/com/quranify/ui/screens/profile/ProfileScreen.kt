package com.quranify.ui.screens.profile

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.data.repository.QuranDataRepository
import com.quranify.data.repository.UserUsageRepository
import com.quranify.player.QuranDownloads
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------
// Design System Tokens: iOS Human Interface + Dark Minimalist Glassmorphism
// -----------------------------------------------------------------------------
private val PitchBlackBg = Color(0xFF07080A)                  // Deep OLED Canvas
private val ObsidianGlassCard = Color(0xFF121419)             // Translucent Glass Container
private val ObsidianGlassInner = Color(0xFF181B22)            // Secondary Glass Inset
private val TrendingPurpleAccent = Color(0xFFA855F7)          // Trending Purple Accent
private val ElectricViolet = Color(0xFF8B5CF6)                // Electric Violet
private val NeonLilac = Color(0xFFC084FC)                     // Neon Lilac Highlight
private val EmeraldAccent = Color(0xFF10B981)                 // Islamic Emerald
private val AmberGoldAccent = Color(0xFFF59E0B)               // Streak & Achievement Gold
private val CyanAccent = Color(0xFF06B6D4)                    // Discovered Reciters Cyan
private val CardBorderSubtle = Color.White.copy(alpha = 0.08f)// Apple-style Hairline Border
private val TextWhitePrimary = Color(0xFFFFFFFF)              // Crisp White
private val TextMutedSecondary = Color(0xFF94A3B8)            // iOS Muted Slate
private val SubtleDivider = Color.White.copy(alpha = 0.06f)   // Hairline Separator

// Persistence Keys
private const val PREF_USER_NAME = "quranify_profile_name"
private const val PREF_USER_BIO = "quranify_profile_bio"
private const val PREF_BITRATE = "quranify_profile_bitrate"
private const val PREF_RECITER = "quranify_profile_reciter"
private const val PREF_GAPLESS = "quranify_profile_gapless"
private const val PREF_LOUDNESS = "quranify_profile_loudness"
private const val PREF_WIFI_ONLY = "quranify_profile_wifi_only"
private const val PREF_DHIKR_ALERT = "quranify_profile_dhikr_alert"
private const val PREF_FRIDAY_ALERT = "quranify_profile_friday_alert"
private const val PREF_PRAYER_ALERT = "quranify_profile_prayer_alert"

object ProfileScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 4u,
                title = "Profile",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val settings = remember { Settings() }
        val scope = rememberCoroutineScope()

        // Real user stats from UserUsageRepository
        val stats by UserUsageRepository.stats.collectAsState()

        // User profile editable state
        var userName by remember {
            mutableStateOf(settings.getString(PREF_USER_NAME, "Abdullah • Believer"))
        }
        var userBio by remember {
            mutableStateOf(settings.getString(PREF_USER_BIO, "Seeking peace & closeness to Allah through the Quran"))
        }
        var showEditDialog by remember { mutableStateOf(false) }

        // Settings toggles & selectors
        var selectedBitrate by remember {
            mutableStateOf(settings.getString(PREF_BITRATE, "High (320kbps)"))
        }
        var selectedReciter by remember {
            mutableStateOf(settings.getString(PREF_RECITER, "Mishary Rashid Alafasy"))
        }
        var isReciterPickerOpen by remember { mutableStateOf(false) }

        var darkModeEnabled by remember { mutableStateOf(true) }
        var gaplessPlayback by remember {
            mutableStateOf(settings.getBoolean(PREF_GAPLESS, true))
        }
        var loudnessNormalization by remember {
            mutableStateOf(settings.getBoolean(PREF_LOUDNESS, true))
        }
        var wifiOnlyDownloads by remember {
            mutableStateOf(settings.getBoolean(PREF_WIFI_ONLY, true))
        }
        var dailyDhikrReminder by remember {
            mutableStateOf(settings.getBoolean(PREF_DHIKR_ALERT, true))
        }
        var fridayKahfAlert by remember {
            mutableStateOf(settings.getBoolean(PREF_FRIDAY_ALERT, true))
        }
        var prayerRecitationAlert by remember {
            mutableStateOf(settings.getBoolean(PREF_PRAYER_ALERT, true))
        }

        // Live offline-download storage state
        val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }
        val cachedSurahCount = downloadedKeys.size

        // Calculate Khatmah progress (out of 114 Surahs)
        val khatmahPercent = ((stats.uniqueSurahsCount.toFloat() / 114f) * 100).toInt().coerceIn(1, 100)

        // Calculate formatted total listening time
        val totalHours = stats.totalSecondsListened / 3600
        val totalMinutes = (stats.totalSecondsListened % 3600) / 60
        val formattedTotalTime = if (totalHours > 0) "${totalHours}h ${totalMinutes}m" else "${stats.minutesToday}m"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBg),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. Screen Title & Ambient Subtitle
            // -----------------------------------------------------------------
            item {
                Column(modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Profile",
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
                        text = "Your spiritual journey, stats & preferences",
                        color = TextMutedSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // -----------------------------------------------------------------
            // 2. Modular Profile Identity Card (Chunky / Oversized Avatar)
            // -----------------------------------------------------------------
            item {
                ProfileHeroCard(
                    userName = userName,
                    userBio = userBio,
                    onEditClick = { showEditDialog = true }
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // -----------------------------------------------------------------
            // 3. Real User Stats (Apple-style Modular 2x2 Grid)
            // -----------------------------------------------------------------
            item {
                Text(
                    text = "SPIRITUAL MILESTONES",
                    color = TrendingPurpleAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Streak
                        ModularStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.LocalFireDepartment,
                            iconColor = AmberGoldAccent,
                            iconBg = AmberGoldAccent.copy(alpha = 0.15f),
                            value = "${stats.daysStreak} Days",
                            label = "Daily Streak",
                            subtext = "Spiritual consistency"
                        )

                        // Card 2: Total Time
                        ModularStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Timer,
                            iconColor = TrendingPurpleAccent,
                            iconBg = TrendingPurpleAccent.copy(alpha = 0.15f),
                            value = formattedTotalTime,
                            label = "Quran Time",
                            subtext = "${stats.minutesToday}m listened today"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: Surahs Explored
                        ModularStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            iconColor = EmeraldAccent,
                            iconBg = EmeraldAccent.copy(alpha = 0.15f),
                            value = "${stats.uniqueSurahsCount} / 114",
                            label = "Surahs Explored",
                            subtext = "Tanzil verified"
                        )

                        // Card 4: Reciters Heard
                        ModularStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Mic,
                            iconColor = CyanAccent,
                            iconBg = CyanAccent.copy(alpha = 0.15f),
                            value = "${stats.uniqueRecitersCount} / 242",
                            label = "Reciters Heard",
                            subtext = "38 global nations"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // -----------------------------------------------------------------
            // 4. Khatmah Quran Journey Progress Card
            // -----------------------------------------------------------------
            item {
                KhatmahProgressCard(
                    surahsCompleted = stats.uniqueSurahsCount,
                    totalSurahs = 114,
                    percentage = khatmahPercent
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // -----------------------------------------------------------------
            // 5. My Quran Library & Saved Shortcuts
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "My Quran Library", icon = Icons.Filled.Bookmark)
                ProfileCardContainer {
                    ProfileLinkRow(
                        icon = Icons.Filled.Favorite,
                        iconTint = Color(0xFFF43F5E),
                        title = "Favorite Verses & Surahs",
                        subtitle = "Bookmarked ayahs and cherished recitations",
                        badge = "Saved"
                    )

                    SubtleDividerLine()

                    ProfileLinkRow(
                        icon = Icons.Filled.Storage,
                        iconTint = EmeraldAccent,
                        title = "Offline Recitations",
                        subtitle = if (cachedSurahCount == 1) "1 surah cached • ${formatStorageBytes(storageSizeBytes)}" else "$cachedSurahCount surahs cached • ${formatStorageBytes(storageSizeBytes)}",
                        badge = formatStorageBytes(storageSizeBytes)
                    )

                    SubtleDividerLine()

                    ProfileLinkRow(
                        icon = Icons.Filled.GridView,
                        iconTint = TrendingPurpleAccent,
                        title = "Custom Playlists",
                        subtitle = "Morning Adhkar, Tahajjud & Tranquility mixes",
                        badge = "3 Lists"
                    )
                }
            }

            // -----------------------------------------------------------------
            // 6. Section: Audio Engine & Streaming Bitrate
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "Audio Engine & Bitrate", icon = Icons.Filled.Headphones)
                ProfileCardContainer {
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
                                        .clickable {
                                            selectedBitrate = name
                                            settings.putString(PREF_BITRATE, name)
                                        }
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

                    ProfileValueRow(
                        icon = Icons.Filled.GraphicEq,
                        title = "Audio Engine",
                        subtitle = "Multiplatform low-latency Core Audio",
                        value = "v2.19 Hi-Res"
                    )

                    SubtleDividerLine()

                    ProfileSwitchRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Gapless Recitation",
                        subtitle = "Seamless zero-latency transition between ayahs",
                        checked = gaplessPlayback,
                        onCheckedChange = {
                            gaplessPlayback = it
                            settings.putBoolean(PREF_GAPLESS, it)
                        }
                    )

                    SubtleDividerLine()

                    ProfileSwitchRow(
                        icon = Icons.Filled.Headphones,
                        title = "Loudness Normalization",
                        subtitle = "Harmonize volume levels across different reciters",
                        checked = loudnessNormalization,
                        onCheckedChange = {
                            loudnessNormalization = it
                            settings.putBoolean(PREF_LOUDNESS, it)
                        }
                    )
                }
            }

            // -----------------------------------------------------------------
            // 7. Section: Default Reciter Preference
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "Reciter Preference", icon = Icons.Filled.Mic)
                ProfileCardContainer {
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
                            val popularReciters = listOf(
                                "Mishary Rashid Alafasy",
                                "Abdul Basit Abdul Samad",
                                "Mahmoud Khalil Al-Husary",
                                "Maher Al-Muaiqly",
                                "Ahmad bin Ali Al-Ajmi",
                                "Abu Bakr Al-Shatri",
                                "Saad Al-Ghamdi",
                                "Abdul Rahman Al-Sudais",
                                "Saud Al-Shuraim"
                            )
                            popularReciters.forEach { reciter ->
                                val isSelected = selectedReciter == reciter
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedReciter = reciter
                                            settings.putString(PREF_RECITER, reciter)
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
                }
            }

            // -----------------------------------------------------------------
            // 8. Section: Appearance & Theme
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "Appearance & Theme", icon = Icons.Filled.Palette)
                ProfileCardContainer {
                    ProfileSwitchRow(
                        icon = Icons.Filled.DarkMode,
                        title = "Dark Obsidian Mode",
                        subtitle = "Pitch Black #07080A OLED theme",
                        checked = darkModeEnabled,
                        onCheckedChange = { darkModeEnabled = it }
                    )

                    SubtleDividerLine()

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

                    ProfileValueRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Quranic Typography",
                        subtitle = "Medina Mushaf rendering typography",
                        value = "Uthmani Hafs v2"
                    )
                }
            }

            // -----------------------------------------------------------------
            // 9. Section: Storage & Offline Downloads
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "Storage & Offline Data", icon = Icons.Filled.Storage)
                ProfileCardContainer {
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
                                        text = if (cachedSurahCount == 1) "1 surah cached offline • ${formatStorageBytes(storageSizeBytes)}" else "$cachedSurahCount surahs cached offline • ${formatStorageBytes(storageSizeBytes)}",
                                        color = TextMutedSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = formatStorageBytes(storageSizeBytes),
                                color = TextWhitePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF22262B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.25f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(TrendingPurpleAccent, NeonLilac)
                                        )
                                    )
                            )
                        }
                    }

                    SubtleDividerLine()

                    ProfileSwitchRow(
                        icon = Icons.Filled.Wifi,
                        title = "Download via Wi-Fi Only",
                        subtitle = "Prevent mobile cellular data consumption",
                        checked = wifiOnlyDownloads,
                        onCheckedChange = {
                            wifiOnlyDownloads = it
                            settings.putBoolean(PREF_WIFI_ONLY, it)
                        }
                    )

                    SubtleDividerLine()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    QuranDownloads.clearAll()
                                    storageSizeBytes = QuranDownloads.storageBytes()
                                }
                            }
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
                                text = formatStorageBytes(storageSizeBytes),
                                color = TextMutedSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 10. Section: Notifications & Spiritual Reminders
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "Spiritual Reminders", icon = Icons.Filled.Notifications)
                ProfileCardContainer {
                    ProfileSwitchRow(
                        icon = Icons.Filled.Notifications,
                        title = "Daily Dhikr Reminder",
                        subtitle = "Morning & Evening Adhkar spiritual alerts",
                        checked = dailyDhikrReminder,
                        onCheckedChange = {
                            dailyDhikrReminder = it
                            settings.putBoolean(PREF_DHIKR_ALERT, it)
                        }
                    )

                    SubtleDividerLine()

                    ProfileSwitchRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Friday Surah Al-Kahf Alert",
                        subtitle = "Gentle reminder every Friday morning",
                        checked = fridayKahfAlert,
                        onCheckedChange = {
                            fridayKahfAlert = it
                            settings.putBoolean(PREF_FRIDAY_ALERT, it)
                        }
                    )

                    SubtleDividerLine()

                    ProfileSwitchRow(
                        icon = Icons.Filled.CloudDone,
                        title = "Prayer Recitation Alerts",
                        subtitle = "Curated verses following Salah prayers",
                        checked = prayerRecitationAlert,
                        onCheckedChange = {
                            prayerRecitationAlert = it
                            settings.putBoolean(PREF_PRAYER_ALERT, it)
                        }
                    )
                }
            }

            // -----------------------------------------------------------------
            // 11. Section: About Quranify
            // -----------------------------------------------------------------
            item {
                ProfileSectionHeader(title = "About Quranify", icon = Icons.Filled.Info)
                ProfileCardContainer {
                    ProfileValueRow(
                        icon = Icons.Filled.Info,
                        title = "App Version",
                        subtitle = "Production release channel",
                        value = "v1.0.0 (Build 2026.1)"
                    )

                    SubtleDividerLine()

                    ProfileValueRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Audio Sources",
                        subtitle = "MP3Quran (242 reciters), Tanzil & EveryAyah",
                        value = "Verified"
                    )

                    SubtleDividerLine()

                    ProfileValueRow(
                        icon = Icons.Filled.Favorite,
                        title = "Open Source License",
                        subtitle = "Apache 2.0 • 100% Free & Ad-Free forever",
                        value = "GitHub"
                    )
                }
            }

            // -----------------------------------------------------------------
            // 12. Spiritual Footer
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(28.dp))
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
                        color = TrendingPurpleAccent.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ---------------------------------------------------------------------
        // Edit Profile Dialog
        // ---------------------------------------------------------------------
        if (showEditDialog) {
            var editingName by remember { mutableStateOf(userName) }
            var editingBio by remember { mutableStateOf(userBio) }

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Display Name",
                            color = TextMutedSecondary,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF171A21))
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = editingName,
                                onValueChange = { editingName = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = "Spiritual Bio / Intention",
                            color = TextMutedSecondary,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF171A21))
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = editingBio,
                                onValueChange = { editingBio = it },
                                maxLines = 3,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editingName.isNotBlank()) {
                                userName = editingName.trim()
                                settings.putString(PREF_USER_NAME, userName)
                            }
                            if (editingBio.isNotBlank()) {
                                userBio = editingBio.trim()
                                settings.putString(PREF_USER_BIO, userBio)
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text(
                            text = "Save",
                            color = TrendingPurpleAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text(
                            text = "Cancel",
                            color = TextMutedSecondary
                        )
                    }
                },
                containerColor = Color(0xFF161920),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component Primitives & Modular Cards
// -----------------------------------------------------------------------------

@Composable
private fun ProfileHeroCard(
    userName: String,
    userBio: String,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(ObsidianGlassCard)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x33A855F7),
                            CardBorderSubtle
                        )
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chunky / Oversized Avatar with Glowing Sweep Gradient Rim
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    TrendingPurpleAccent,
                                    NeonLilac,
                                    EmeraldAccent,
                                    AmberGoldAccent,
                                    ElectricViolet,
                                    TrendingPurpleAccent
                                )
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF2C1A4A),
                                    Color(0xFF120E1C)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.toString()?.uppercase() ?: "A",
                        color = TrendingPurpleAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Edit badge floating at bottom right of avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(TrendingPurpleAccent)
                        .border(2.dp, PitchBlackBg, CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = userName,
                        color = TextWhitePrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = userBio,
                    color = TextMutedSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Synced Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TrendingPurpleAccent.copy(alpha = 0.16f))
                            .border(0.5.dp, TrendingPurpleAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO MEMBER",
                            color = TrendingPurpleAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldAccent.copy(alpha = 0.16f))
                            .border(0.5.dp, EmeraldAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CLOUD SYNCED",
                            color = EmeraldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModularStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    value: String,
    label: String,
    subtext: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianGlassCard)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                color = TextWhitePrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                color = TextWhitePrimary.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = subtext,
                color = TextMutedSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KhatmahProgressCard(
    surahsCompleted: Int,
    totalSurahs: Int,
    percentage: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianGlassCard)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Current Khatmah",
                            color = TextWhitePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$surahsCompleted of $totalSurahs Surahs listened",
                            color = TextMutedSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldAccent.copy(alpha = 0.15f))
                        .border(0.5.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percentage%",
                        color = EmeraldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF20242B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage.toFloat() / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TrendingPurpleAccent, EmeraldAccent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "« خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ »",
                color = EmeraldAccent.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "\"The best of you are those who learn the Quran and teach it.\"",
                color = TextMutedSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileLinkRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String = ""
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
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMutedSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (badge.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2128))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badge,
                    color = TextMutedSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextMutedSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProfileSectionHeader(
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
private fun ProfileCardContainer(
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
private fun ProfileSwitchRow(
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
private fun ProfileValueRow(
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

// Storage size formatting: B / KB / MB with one decimal
private fun formatStorageBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${oneDecimal(kb)} KB"
    val mb = kb / 1024.0
    return "${oneDecimal(mb)} MB"
}

private fun oneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
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
