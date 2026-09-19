package com.quranify.ui.screens.profile

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.data.repository.FavoritesStore
import com.quranify.player.QuranDownloads
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.settings.AppSettingsScreen
import com.quranify.ui.screens.stats.StatsScreen
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------
// Design System Tokens: Black-Glass (Pure Black + Glass Cards + Link Blue)
// -----------------------------------------------------------------------------
private val PureBlackBg = Color(0xFF000000)                    // Pure Black page bg
private val DarkCard = Color(0xFF1C1C1E)                       // Dark card base (dialogs)
private val GlassCard = Color.White.copy(alpha = 0.05f)        // Glass card fill
private val LinkBlue = Color(0xFF4C8DFF)                       // Primary accent (blue)
private val HeroBlue = Color(0xFF2E7CF6)                       // Hero gradient start
private val HeroIndigo = Color(0xFF0A1F44)                     // Hero gradient end
private val Emerald = Color(0xFF30D158)                        // Success / following green
private val HeartRed = Color(0xFFFF5A6E)                       // Favorites / heart red
private val StreakGold = Color(0xFFFFC94D)                     // Warm gold — streak flame ONLY (unused on this screen)
private val CardBorderSubtle = Color.White.copy(alpha = 0.08f)// Glass hairline border
private val TextWhitePrimary = Color(0xFFFFFFFF)              // Crisp White
private val TextMuted = Color(0xFF9A9AA0)                      // Muted grey
private val SubtleDivider = Color.White.copy(alpha = 0.06f)   // Hairline Separator

// Persistence Keys
private const val PREF_USER_NAME = "quranify_profile_name"
private const val PREF_USER_BIO = "quranify_profile_bio"
private const val PREF_DHIKR_ALERT = "quranify_profile_dhikr_alert"

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
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current

        // Live favourites count
        val favoriteTracks by FavoritesStore.favoriteTracks.collectAsState()
        val favoriteCount = favoriteTracks.size

        // User profile editable state
        var userName by remember {
            mutableStateOf(settings.getString(PREF_USER_NAME, "Abdullah • Believer"))
        }
        var userBio by remember {
            mutableStateOf(settings.getString(PREF_USER_BIO, "Seeking peace & closeness to Allah through the Quran"))
        }
        var showEditDialog by remember { mutableStateOf(false) }

        // Spiritual reminder master toggle
        var dailyDhikrReminder by remember {
            mutableStateOf(settings.getBoolean(PREF_DHIKR_ALERT, true))
        }

        // Live offline-download storage state
        val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }
        val cachedSurahCount = downloadedKeys.size

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlackBg),
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
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(LinkBlue.copy(alpha = 0.15f))
                                .border(0.5.dp, LinkBlue.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = LinkBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your spiritual journey, stats & preferences",
                        color = TextMuted,
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
            // Favorites row (live count — kept)
            // -----------------------------------------------------------------
            item {
                ProfileCardContainer {
                    ProfileLinkRow(
                        icon = Icons.Filled.Favorite,
                        iconTint = HeartRed,
                        title = "Favorite Verses & Surahs",
                        subtitle = "Bookmarked ayahs and cherished recitations",
                        badge = "$favoriteCount items"
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
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
                                        .background(LinkBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = LinkBlue,
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
                                        color = TextMuted,
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
                                .background(Color.White.copy(alpha = 0.10f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.25f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(LinkBlue, HeroBlue)
                                        )
                                    )
                            )
                        }
                    }

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
                                    .background(HeartRed.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear Cache",
                                    tint = HeartRed,
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
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatStorageBytes(storageSizeBytes),
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // Spiritual Reminders (single summary row + toggle)
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
                }
            }

            // -----------------------------------------------------------------
            // Settings & Stats link rows
            // -----------------------------------------------------------------
            item {
                ProfileCardContainer {
                    ProfileLinkRow(
                        icon = Icons.Filled.Settings,
                        iconTint = LinkBlue,
                        title = "Settings",
                        subtitle = "Audio, reciter, appearance & downloads",
                        onClick = { rootNavigator?.push(AppSettingsScreen) }
                    )

                    SubtleDividerLine()

                    ProfileLinkRow(
                        icon = Icons.Filled.BarChart,
                        iconTint = Emerald,
                        title = "Your stats",
                        subtitle = "Streaks, listening time & Khatmah journey",
                        onClick = { rootNavigator?.push(StatsScreen) }
                    )
                }
            }

            // -----------------------------------------------------------------
            // About Quranify (trimmed)
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
                        icon = Icons.Filled.Favorite,
                        title = "Audio Sources",
                        subtitle = "MP3Quran (242 reciters), Tanzil & EveryAyah",
                        value = "Verified"
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
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
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
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
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
                            color = LinkBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text(
                            text = "Cancel",
                            color = TextMuted
                        )
                    }
                },
                containerColor = DarkCard,
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
            .background(GlassCard)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            LinkBlue.copy(alpha = 0.25f),
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
                                    LinkBlue,
                                    HeroBlue,
                                    LinkBlue.copy(alpha = 0.6f),
                                    HeroBlue.copy(alpha = 0.7f),
                                    LinkBlue,
                                    LinkBlue
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
                                    HeroBlue.copy(alpha = 0.45f),
                                    HeroIndigo
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.toString()?.uppercase() ?: "A",
                        color = LinkBlue,
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
                        .background(LinkBlue)
                        .border(2.dp, PureBlackBg, CircleShape)
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
                    color = TextMuted,
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
                            .clip(RoundedCornerShape(50.dp))
                            .background(LinkBlue.copy(alpha = 0.16f))
                            .border(0.5.dp, LinkBlue.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO MEMBER",
                            color = LinkBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Emerald.copy(alpha = 0.16f))
                            .border(0.5.dp, Emerald.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CLOUD SYNCED",
                            color = Emerald,
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
private fun ProfileLinkRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String = "",
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (badge.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badge,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.6f),
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
                tint = LinkBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = title.uppercase(),
            color = LinkBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
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
            .background(GlassCard)
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
                    if (checked) LinkBlue.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) LinkBlue else TextMuted,
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
                    color = TextMuted,
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
                checkedTrackColor = LinkBlue,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color(0xFF8E989C),
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
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
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
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
                    color = TextMuted,
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
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(50.dp))
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
