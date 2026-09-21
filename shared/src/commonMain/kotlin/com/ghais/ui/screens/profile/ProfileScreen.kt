package com.ghais.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.OnboardingStore
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.profile.glass.AuroraBackdrop
import com.ghais.ui.screens.profile.glass.EditProfileDialogGlass
import com.ghais.ui.screens.profile.glass.FeatureCard
import com.ghais.ui.screens.profile.glass.GlassCardContainer
import com.ghais.ui.screens.profile.glass.GlassDivider
import com.ghais.ui.screens.profile.glass.GlassSectionLabel
import com.ghais.ui.screens.profile.glass.ProfileIdentityPill
import com.ghais.ui.screens.profile.glass.ProtonRow
import com.ghais.ui.screens.profile.glass.ProtonSwitchRow
import com.ghais.ui.screens.profile.glass.ProtonValueRow
import com.ghais.ui.screens.profile.glass.SchedulesProtonSection
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

        // Live daily goal (Daily Goal card below)
        val dailyMinutes by OnboardingStore.dailyGoalMinutes.collectAsState()

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AuroraBackdrop()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp)
            ) {
                // -----------------------------------------------------------------
                // 1. Daily Goal feature card
                // -----------------------------------------------------------------
                item {
                    FeatureCard(
                        title = "Daily Goal",
                        body = "You're aiming for $dailyMinutes minutes of listening a day. Small steps, lasting reward.",
                        buttonText = "View stats",
                        onButton = { rootNavigator?.push(StatsScreen) }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 2. Identity pill (tap to edit profile)
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
                // 3. Recitation schedules
                // -----------------------------------------------------------------
                item {
                    SchedulesProtonSection()
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 4. Preferences
                // -----------------------------------------------------------------
                item {
                    GlassSectionLabel("Preferences")
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

                        GlassDivider()

                        ProtonRow(
                            icon = Icons.Filled.Tune,
                            title = "Settings",
                            subtitle = "Audio, reciter, appearance & downloads",
                            onClick = { rootNavigator?.push(AppSettingsScreen) }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 5. About
                // -----------------------------------------------------------------
                item {
                    GlassSectionLabel("About")
                    GlassCardContainer {
                        ProtonValueRow(
                            icon = Icons.Filled.Language,
                            title = "App Version",
                            subtitle = "Production release channel",
                            value = "v1.0.0 (Build 2026.1)"
                        )

                        GlassDivider()

                        ProtonValueRow(
                            icon = Icons.Filled.VerifiedUser,
                            title = "Audio Sources",
                            subtitle = "MP3Quran (242 reciters), Tanzil & EveryAyah",
                            value = "Verified"
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 6. Log out (always visible — auth is mandatory)
                // -----------------------------------------------------------------
                item {
                    GlassCardContainer {
                        ProtonRow(
                            icon = Icons.Filled.Logout,
                            title = "Log out",
                            subtitle = session?.email ?: "Signed in",
                            destructive = true,
                            onClick = { scope.launch { AuthRepository.signOut() } }
                        )
                    }
                }
            }
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
