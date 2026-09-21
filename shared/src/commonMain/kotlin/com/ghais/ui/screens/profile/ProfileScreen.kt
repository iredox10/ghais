package com.ghais.ui.screens.profile

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.window.Dialog
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.sync.SyncEngine
import com.ghais.data.sync.SyncStatus
import com.ghais.player.QuranDownloads
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.settings.AppSettingsScreen
import com.ghais.ui.screens.stats.StatsScreen
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import kotlin.time.Clock

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

        // Auth session (null = guest)
        val session by AuthRepository.session.collectAsState()

        // Cloud-sync status (manual Sync row below)
        val syncStatus by SyncEngine.status.collectAsState()
        val syncLastAt by SyncEngine.lastSyncedAt.collectAsState()
        val syncError by SyncEngine.lastError.collectAsState()
        val syncSubtitle = when {
            session == null -> "Sign in to sync"
            syncStatus == SyncStatus.SYNCING -> "Syncing…"
            syncStatus == SyncStatus.ERROR ->
                syncError?.takeIf { it.isNotBlank() }?.let { "Sync failed • $it" } ?: "Sync failed"
            else -> syncLastAt?.let {
                "Last synced ${formatSyncRelative(Clock.System.now().toEpochMilliseconds(), it)}"
            } ?: "Tap to sync now"
        }

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
                    onEditClick = { showEditDialog = true },
                    sessionName = session?.name,
                    sessionEmail = session?.email,
                    isGuest = session == null,
                    onLogoutClick = { scope.launch { AuthRepository.signOut() } }
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
            // Recitation schedules (multiple daily alarms)
            // -----------------------------------------------------------------
            item {
                RecitationSchedulesSection()
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

                    SubtleDividerLine()

                    ProfileLinkRow(
                        icon = Icons.Filled.Sync,
                        iconTint = Emerald,
                        title = "Sync",
                        subtitle = syncSubtitle,
                        onClick = { scope.launch { SyncEngine.syncNow() } }
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
    onEditClick: () -> Unit,
    sessionName: String? = null,
    sessionEmail: String? = null,
    isGuest: Boolean = true,
    onLogoutClick: () -> Unit = {}
) {
    val displayName = sessionName?.takeIf { it.isNotBlank() } ?: userName
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
            .padding(vertical = 26.dp, horizontal = 20.dp)
    ) {
        // Soft top glow for the glassmorphic feel
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            LinkBlue.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chunky / Oversized Avatar with Glowing Sweep Gradient Rim
            Box(
                modifier = Modifier
                    .size(112.dp)
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
                        text = displayName.firstOrNull()?.toString()?.uppercase() ?: "A",
                        color = LinkBlue,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = displayName,
                color = TextWhitePrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = userBio,
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )

            if (!sessionEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sessionEmail,
                    color = TextMuted.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
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

                if (isGuest) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, CardBorderSubtle, RoundedCornerShape(50.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "GUEST MODE",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile pill (reference-style action button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(LinkBlue)
                    .clickable { onEditClick() }
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Edit Profile",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isGuest) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onLogoutClick) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = null,
                            tint = HeartRed.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Log out",
                            color = HeartRed.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
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

// Relative sync timestamp: "just now" / "Xs ago" / "Xm ago" / "Xh ago" / "Xd ago"
private fun formatSyncRelative(nowMs: Long, atMs: Long): String {
    val seconds = ((nowMs - atMs).coerceAtLeast(0L)) / 1000L
    return when {
        seconds < 10 -> "just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
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

// -----------------------------------------------------------------------------
// Section: Recitation Schedules (multiple daily alarms: time + reciter +
// surah range + optional play-for-minutes)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecitationSchedulesSection() {
    val schedules by SchedulesStore.schedules.collectAsState()
    val reciters = remember { QuranDataRepository.getReciters() }
    val surahs = remember { QuranDataRepository.getSurahs() }

    var showSheet by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var hour by remember { mutableStateOf(5) }
    var minute by remember { mutableStateOf(30) }
    var reciterSlug by remember { mutableStateOf(reciters.firstOrNull()?.slug ?: "") }
    var fromSurah by remember { mutableStateOf(1) }
    var toSurah by remember { mutableStateOf(114) }
    var useMinutes by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf(30) }
    var reciterExpanded by remember { mutableStateOf(false) }

    fun surahName(id: Int): String = surahs.find { it.id == id }?.nameEn ?: "Surah $id"
    fun reciterName(slug: String): String = reciters.find { it.slug == slug }?.nameEn ?: slug

    fun openAdd() {
        editingId = null
        hour = 5
        minute = 30
        reciterSlug = reciters.firstOrNull()?.slug ?: ""
        fromSurah = 1
        toSurah = 114
        useMinutes = false
        minutes = 30
        reciterExpanded = false
        showSheet = true
    }

    fun openEdit(schedule: RecitationSchedule) {
        editingId = schedule.id
        hour = schedule.hour
        minute = schedule.minute
        reciterSlug = schedule.reciterSlug
        fromSurah = schedule.fromSurah
        toSurah = schedule.toSurah
        useMinutes = schedule.durationMin != null
        minutes = schedule.durationMin ?: 30
        reciterExpanded = false
        showSheet = true
    }

    fun saveSchedule() {
        val safeReciter = reciterSlug.ifBlank { reciters.firstOrNull()?.slug ?: "" }
        if (safeReciter.isBlank()) return
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val safeFrom = fromSurah.coerceIn(1, 114)
        val preservedEnabled = editingId?.let { id -> schedules.find { it.id == id }?.enabled } ?: true
        val schedule = if (useMinutes) {
            RecitationSchedule(
                id = editingId ?: ("sch-" + Clock.System.now().toEpochMilliseconds()),
                hour = safeHour,
                minute = safeMinute,
                reciterSlug = safeReciter,
                fromSurah = safeFrom,
                toSurah = 114,
                durationMin = minutes.coerceIn(5, 180),
                enabled = preservedEnabled
            )
        } else {
            RecitationSchedule(
                id = editingId ?: ("sch-" + Clock.System.now().toEpochMilliseconds()),
                hour = safeHour,
                minute = safeMinute,
                reciterSlug = safeReciter,
                fromSurah = safeFrom,
                toSurah = toSurah.coerceIn(safeFrom, 114),
                durationMin = null,
                enabled = preservedEnabled
            )
        }
        if (editingId == null) SchedulesStore.add(schedule) else SchedulesStore.update(schedule)
        showSheet = false
    }

    ProfileSectionHeader(title = "Recitation schedules", icon = Icons.Filled.Schedule)
    Text(
        text = "Play Quran automatically at your times",
        color = TextMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )

    if (schedules.isEmpty()) {
        ProfileCardContainer {
            Text(
                text = "No schedules — add one to wake up to Quran",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            schedules.forEach { schedule ->
                val rangeCore = if (schedule.fromSurah == schedule.toSurah) {
                    surahName(schedule.fromSurah)
                } else {
                    "${surahName(schedule.fromSurah)} → ${surahName(schedule.toSurah)}"
                }
                val rangeText = if (schedule.durationMin != null) {
                    "$rangeCore • ${schedule.durationMin} min"
                } else {
                    "$rangeCore • full range"
                }
                ScheduleCard(
                    schedule = schedule,
                    timeText = formatScheduleTime(schedule.hour, schedule.minute),
                    reciterText = reciterName(schedule.reciterSlug),
                    rangeText = rangeText,
                    onEdit = { openEdit(schedule) },
                    onToggle = { SchedulesStore.setEnabled(schedule.id, it) },
                    onDelete = { SchedulesStore.remove(schedule.id) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Full-width "Add schedule" pill (mirrors the Edit Profile pill)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(LinkBlue)
            .clickable { openAdd() }
            .padding(vertical = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Add schedule",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (showSheet) {
        AlertDialog(
            onDismissRequest = { showSheet = false },
            title = {
                Text(
                    text = if (editingId == null) "Add schedule" else "Edit schedule",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Clock time picker (tap the time to open the clock dial)
                    var showClock by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Starts at",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, LinkBlue.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .clickable { showClock = true }
                                .padding(horizontal = 28.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatScheduleTime(hour, minute),
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to set time",
                            color = LinkBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showClock = true }
                        )
                    }

                    if (showClock) {
                        val clockState = rememberTimePickerState(
                            initialHour = hour,
                            initialMinute = minute,
                            is24Hour = true
                        )
                        Dialog(onDismissRequest = { showClock = false }) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(DarkCard)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TimePicker(state = clockState)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(50.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .clickable { showClock = false }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Cancel", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(50.dp))
                                                .background(LinkBlue)
                                                .clickable {
                                                    hour = clockState.hour
                                                    minute = clockState.minute
                                                    showClock = false
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Set time", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reciter picker (inline expandable list)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reciter",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { reciterExpanded = !reciterExpanded }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = reciterName(reciterSlug).ifBlank { "Choose reciter" },
                                color = TextWhitePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (reciterExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                            ) {
                                reciters.forEach { reciter ->
                                    val selected = reciter.slug == reciterSlug
                                    Text(
                                        text = reciter.nameEn,
                                        color = if (selected) LinkBlue else TextWhitePrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                reciterSlug = reciter.slug
                                                reciterExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mode toggle: surah range vs play-for-minutes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScheduleModePill(
                            text = "Surah range",
                            selected = !useMinutes,
                            onClick = { useMinutes = false },
                            modifier = Modifier.weight(1f)
                        )
                        ScheduleModePill(
                            text = "Play for",
                            selected = useMinutes,
                            onClick = { useMinutes = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!useMinutes) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ScheduleStepper(
                                label = "From: ${surahName(fromSurah)}",
                                valueText = fromSurah.toString(),
                                onMinus = { fromSurah = (fromSurah - 1).coerceAtLeast(1) },
                                onPlus = { fromSurah = (fromSurah + 1).coerceAtMost(114) }
                            )
                            ScheduleStepper(
                                label = "To: ${surahName(toSurah.coerceIn(fromSurah, 114))}",
                                valueText = toSurah.coerceIn(fromSurah, 114).toString(),
                                onMinus = { toSurah = (toSurah - 1).coerceIn(fromSurah, 114) },
                                onPlus = { toSurah = (toSurah + 1).coerceIn(fromSurah, 114) }
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ScheduleStepper(
                                label = "Start: ${surahName(fromSurah)}",
                                valueText = fromSurah.toString(),
                                onMinus = { fromSurah = (fromSurah - 1).coerceAtLeast(1) },
                                onPlus = { fromSurah = (fromSurah + 1).coerceAtMost(114) }
                            )
                            ScheduleStepper(
                                label = "Minutes",
                                valueText = "$minutes min",
                                onMinus = { minutes = (minutes - 5).coerceAtLeast(5) },
                                onPlus = { minutes = (minutes + 5).coerceAtMost(180) }
                            )
                            Text(
                                text = "Plays from ${surahName(fromSurah)} onward",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { saveSchedule() }) {
                    Text(
                        text = "Save",
                        color = LinkBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSheet = false }) {
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

@Composable
private fun ScheduleCard(
    schedule: RecitationSchedule,
    timeText: String,
    reciterText: String,
    rangeText: String,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ProfileCardContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LinkBlue.copy(alpha = 0.14f))
                    .border(0.5.dp, LinkBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeText,
                    color = LinkBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Text(
                    text = reciterText,
                    color = TextWhitePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rangeText,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = schedule.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LinkBlue,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color(0xFF8E989C),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.12f)
                )
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete schedule",
                    tint = HeartRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ScheduleStepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScheduleStepperButton(text = "−", onClick = onMinus)
            Text(
                text = valueText,
                color = TextWhitePrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            ScheduleStepperButton(text = "+", onClick = onPlus)
        }
    }
}

@Composable
private fun ScheduleStepperButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, CardBorderSubtle, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = LinkBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScheduleModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) LinkBlue else Color.White.copy(alpha = 0.08f))
            .border(
                0.5.dp,
                if (selected) LinkBlue else CardBorderSubtle,
                RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// 24h "HH:MM" formatting for schedule times
private fun formatScheduleTime(hour: Int, minute: Int): String {
    return hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
}
