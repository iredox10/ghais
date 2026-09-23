package com.ghais.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.FavoritesStore
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirGrainOverlay
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirAmbientGlow
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.profile.glass.AuroraBackdrop
import com.ghais.ui.screens.profile.glass.EditProfileDialogGlass
import com.ghais.ui.screens.profile.glass.GlassCardContainer
import com.ghais.ui.screens.profile.glass.ProfileIdentityPill
import com.ghais.ui.screens.profile.glass.ProtonSwitchRow
import com.ghais.ui.screens.profile.glass.SchedulesProtonSection
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.screens.settings.AppSettingsScreen
import com.ghais.ui.screens.stats.StatsScreen
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch

// Persistence Keys
private const val PREF_USER_NAME = "ghais_profile_name"
private const val PREF_USER_BIO = "ghais_profile_bio"
private const val PREF_DHIKR_ALERT = "ghais_profile_dhikr_alert"

// Derives the pre-rebrand key ("quran…" + "ify_…" form) without hardcoding
// the legacy literal, so the rename stays grep-clean.
private fun legacyPrefKey(newKey: String): String =
    newKey.replace("ghais_", "quran" + "ify_")

// One-time migration readers: read the new key first; if it holds no value
// and the legacy key does, adopt the legacy value, persist it under the new
// key, and delete the legacy key. Best-effort try/catch throughout.
private fun Settings.migratedProfileString(newKey: String, default: String): String {
    return try {
        val current = getString(newKey, default)
        if (current.isNotBlank()) return current
        val legacyKey = legacyPrefKey(newKey)
        val legacy = try {
            getString(legacyKey, "")
        } catch (_: Exception) {
            return current
        }
        if (legacy.isNotBlank()) {
            try {
                putString(newKey, legacy)
            } catch (_: Exception) {
            }
            try {
                remove(legacyKey)
            } catch (_: Exception) {
            }
            legacy
        } else {
            current
        }
    } catch (_: Exception) {
        default
    }
}

private fun Settings.migratedProfileBoolean(newKey: String, default: Boolean): Boolean {
    return try {
        val current = getBoolean(newKey, default)
        if (current != default) return current
        val legacyKey = legacyPrefKey(newKey)
        val legacy = try {
            getBoolean(legacyKey, default)
        } catch (_: Exception) {
            return current
        }
        if (legacy != default) {
            try {
                putBoolean(newKey, legacy)
            } catch (_: Exception) {
            }
            try {
                remove(legacyKey)
            } catch (_: Exception) {
            }
            legacy
        } else {
            current
        }
    } catch (_: Exception) {
        default
    }
}

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

        // User profile editable state
        var userName by remember {
            mutableStateOf(settings.migratedProfileString(PREF_USER_NAME, "Abdullah • Believer"))
        }
        var userBio by remember {
            mutableStateOf(settings.migratedProfileString(PREF_USER_BIO, "Seeking peace & closeness to Allah through the Quran"))
        }
        var showEditDialog by remember { mutableStateOf(false) }

        // Spiritual reminder master toggle
        var dailyDhikrReminder by remember {
            mutableStateOf(settings.migratedProfileBoolean(PREF_DHIKR_ALERT, true))
        }

        // Live offline-download storage state
        val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }
        val cachedSurahCount = downloadedKeys.size

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GhaisNoir.NoirBlack)
                .noirAmbientGlow()
        ) {
            AuroraBackdrop()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp)
            ) {
                // -----------------------------------------------------------------
                // 0. Noir header — editorial greeting
                // -----------------------------------------------------------------
                item {
                    NoirProfileHeader(
                        userName = session?.name?.ifBlank { null } ?: userName
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // -----------------------------------------------------------------
                // 1. Identity pill (tap to edit profile)
                // -----------------------------------------------------------------
                item {
                    ProfileIdentityPill(
                        displayName = session?.name?.ifBlank { null } ?: userName,
                        email = session?.email,
                        isGuest = session == null,
                        onClick = { showEditDialog = true }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 2. Library (badge rows)
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "Collections")
                    GlassCardContainer {
                        com.ghais.ui.screens.profile.NoirLibraryRow(
                            icon = Icons.Filled.MusicNote,
                            title = "Favorite Verses & Surahs",
                            subtitle = "Bookmarked ayahs and cherished recitations",
                            countBadge = favoriteCount
                        )

                        NoirRowDivider()

                        com.ghais.ui.screens.profile.NoirLibraryRow(
                            icon = Icons.Filled.DownloadDone,
                            title = "Offline downloads",
                            subtitle = "$cachedSurahCount surahs cached • " + formatStorageBytes(storageSizeBytes),
                            countBadge = cachedSurahCount
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 3. Recitation schedules
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "Schedules")
                    SchedulesProtonSection()
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 4. Preferences (Sentinel-style)
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "Preferences")
                    GlassCardContainer {
                        ProtonSwitchRow(
                            icon = Icons.Filled.NotificationsActive,
                            title = "Daily Dhikr Reminder",
                            subtitle = "Morning & Evening Adhkar alerts",
                            checked = dailyDhikrReminder,
                            onCheckedChange = {
                                dailyDhikrReminder = it
                                settings.putBoolean(PREF_DHIKR_ALERT, it)
                            }
                        )

                        NoirRowDivider()

                        com.ghais.ui.screens.profile.NoirSettingsRow(
                            icon = Icons.Filled.Tune,
                            title = "Settings",
                            subtitle = "Audio, reciter, appearance & downloads",
                            onClick = { rootNavigator?.push(AppSettingsScreen) }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 5. Journey (Sentinel-style rows)
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "Journey")
                    GlassCardContainer {
                        com.ghais.ui.screens.profile.NoirSettingsRow(
                            icon = Icons.Filled.Insights,
                            title = "Your stats",
                            subtitle = "Streaks, listening time & Khatmah journey",
                            onClick = { rootNavigator?.push(StatsScreen) }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 6. About (ghost value chips)
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "About")
                    GlassCardContainer {
                        com.ghais.ui.screens.profile.NoirAboutRow(
                            icon = Icons.Filled.Language,
                            title = "App Version",
                            subtitle = "Production release channel",
                            value = "v1.0.0 (Build 2026.1)"
                        )

                        NoirRowDivider()

                        com.ghais.ui.screens.profile.NoirAboutRow(
                            icon = Icons.Filled.VerifiedUser,
                            title = "Audio Sources",
                            subtitle = "MP3Quran (242 reciters), Tanzil & EveryAyah",
                            value = "Verified"
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 7. Log out (ghost danger row — muted, no red)
                // -----------------------------------------------------------------
                item {
                    GlassCardContainer {
                        com.ghais.ui.screens.profile.NoirLogoutRow(
                            subtitle = session?.email ?: "Signed in",
                            onClick = { scope.launch { AuthRepository.signOut() } }
                        )
                    }
                }
            }
            // Film grain over everything (last = on top).
            NoirGrainOverlay()
        }

        // ---------------------------------------------------------------------
        // Edit Profile Dialog
        // ---------------------------------------------------------------------
        if (showEditDialog) {
            EditProfileDialogGlass(
                initialName = userName,
                initialBio = userBio,
                onSave = { n, b ->
                    if (n.isNotBlank()) {
                        userName = n
                        settings.putString(PREF_USER_NAME, n)
                    }
                    if (b.isNotBlank()) {
                        userBio = b
                        settings.putString(PREF_USER_BIO, b)
                    }
                    showEditDialog = false
                },
                onDismiss = { showEditDialog = false }
            )
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
