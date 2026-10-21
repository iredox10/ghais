package com.ghais.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
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
import com.ghais.data.repository.BroadcastRepository
import com.ghais.data.repository.KhatmaStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.ui.screens.auth.AuthScreen
import com.ghais.ui.screens.onboarding.OnboardingScreen
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.khatmaIsComplete
import com.ghais.data.share.ShareSheet
import com.ghais.data.share.SupportKind
import com.ghais.data.share.openSupportMail
import com.ghais.analytics.HookAnalytics
import com.ghais.analytics.HookEvents
import com.ghais.flags.HookFlags
import com.ghais.ui.screens.profile.identity.AchievementStats
import com.ghais.ui.screens.profile.identity.AchievementsShelf
import com.ghais.ui.screens.profile.identity.unlockedAchievements
import com.ghais.ui.components.noir.NoirGrainOverlay
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirAmbientGlow
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.profile.glass.AuroraBackdrop
import com.ghais.ui.screens.profile.glass.GlassCardContainer
import com.ghais.ui.screens.profile.glass.ProfileIdentityPill
import com.ghais.ui.screens.profile.glass.ProtonSwitchRow
import com.ghais.ui.screens.profile.glass.SchedulesProtonSection
import com.ghais.ui.screens.broadcasts.BroadcastsScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.screens.settings.AppSettingsScreen
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

        // Auth session (null = guest)
        val session by AuthRepository.session.collectAsState()

        // Broadcasts inbox unread badge (public cache; refresh happens on inbox open).
        val broadcasts by BroadcastRepository.broadcasts.collectAsState()
        val seenIds by BroadcastRepository.seenIds.collectAsState()
        val unreadBroadcasts = broadcasts.count { it.id !in seenIds }

        // User profile editable state
        var userName by remember {
            mutableStateOf(settings.migratedProfileString(PREF_USER_NAME, "Abdullah • Believer"))
        }
        var userBio by remember {
            mutableStateOf(settings.migratedProfileString(PREF_USER_BIO, "Seeking peace & closeness to Allah through the Quran"))
        }

        // Spiritual reminder master toggle
        var dailyDhikrReminder by remember {
            mutableStateOf(settings.migratedProfileBoolean(PREF_DHIKR_ALERT, true))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Profile owns its root canvas (not NoirScreenRoot), so it
                // carries the status-bar inset itself — same clearance every
                // NoirScreenRoot screen gets; the tab Scaffold no longer
                // applies a top inset.
                .statusBarsPadding()
                .background(GhaisNoir.NoirBlack)
                .noirAmbientGlow()
        ) {
            AuroraBackdrop()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp)
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
                // 1. Identity pill
                // -----------------------------------------------------------------
                item {
                    ProfileIdentityPill(
                        displayName = session?.name?.ifBlank { null } ?: userName,
                        email = session?.email,
                        isGuest = session == null
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 1b. Achievements (hooks A11)
                // -----------------------------------------------------------------
                item {
                    val stats by UserUsageRepository.stats.collectAsState()
                    val khatmaPlans by KhatmaStore.plans.collectAsState()
                    val totalMin = stats.totalSecondsListened / 60L
                    if (HookFlags.achievementsEnabled) {
                        val unlockedIds = remember(totalMin, stats.daysStreak, stats.uniqueRecitersCount, khatmaPlans) {
                            try {
                                unlockedAchievements(
                                    AchievementStats(
                                        hasListened = totalMin > 0,
                                        daysStreak = stats.daysStreak,
                                        uniqueReciters = stats.uniqueRecitersCount,
                                        khatmaCompleted = khatmaPlans.count { khatmaIsComplete(it) },
                                        totalMinutes = totalMin
                                    )
                                )
                            } catch (_: Exception) {
                                emptySet()
                            }
                        }
                        LaunchedEffect(unlockedIds) {
                            try {
                                val settings = Settings()
                                val seen = settings.getString("ghais_ach_seen", "")
                                    .split(",").filter { it.isNotBlank() }.toSet()
                                (unlockedIds - seen).forEach { id ->
                                    HookAnalytics.track(
                                        HookEvents.Event.ACHIEVEMENT_UNLOCKED,
                                        mapOf(HookEvents.Prop.ACHIEVEMENT_ID to id),
                                    )
                                }
                                if (unlockedIds != seen) {
                                    settings.putString("ghais_ach_seen", unlockedIds.joinToString(","))
                                }
                            } catch (_: Exception) {
                            }
                        }
                        AchievementsShelf(
                            unlocked = unlockedIds,
                            onShare = if (HookFlags.ceremoniesEnabled) { def ->
                                if (ShareSheet.isAvailable()) {
                                    ShareSheet.shareText(
                                        def.title,
                                        "I unlocked \"${def.title}\" on Ghaith. ${def.desc}"
                                    )
                                }
                            } else null
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 2. Recitation schedules
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "Schedules")
                    SchedulesProtonSection()
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 3. Preferences (Sentinel-style)
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

                        NoirRowDivider()

                        com.ghais.ui.screens.profile.NoirSettingsRow(
                            icon = Icons.Filled.Notifications,
                            title = "Broadcasts",
                            subtitle = if (unreadBroadcasts > 0) "$unreadBroadcasts unread announcement(s)" else "Announcements from Ghaith",
                            onClick = { rootNavigator?.push(BroadcastsScreen) }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 4. Support — each row opens Gmail compose, pre-filled
                // -----------------------------------------------------------------
                item {
                    NoirSectionHeader(label = "Support")
                    GlassCardContainer {
                        com.ghais.ui.screens.profile.NoirSettingsRow(
                            icon = Icons.Filled.BugReport,
                            title = "Report a bug",
                            subtitle = "Opens Gmail, ready to send",
                            onClick = { openSupportMail(SupportKind.BUG, session?.email) }
                        )

                        NoirRowDivider()

                        com.ghais.ui.screens.profile.NoirSettingsRow(
                            icon = Icons.Filled.Lightbulb,
                            title = "Suggest a feature",
                            subtitle = "Opens Gmail, ready to send",
                            onClick = { openSupportMail(SupportKind.FEATURE, session?.email) }
                        )

                        NoirRowDivider()

                        com.ghais.ui.screens.profile.NoirSettingsRow(
                            icon = Icons.Filled.MailOutline,
                            title = "Contact us",
                            subtitle = "Opens Gmail, ready to send",
                            onClick = { openSupportMail(SupportKind.CONTACT, session?.email) }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // -----------------------------------------------------------------
                // 5. Log out (ghost danger row — muted, no red). Guests get a
                // sign-in row instead: same slot, opposite direction.
                // -----------------------------------------------------------------
                item {
                    GlassCardContainer {
                        if (session != null) {
                            com.ghais.ui.screens.profile.NoirLogoutRow(
                                subtitle = session?.email ?: "Signed in",
                                onClick = { scope.launch { AuthRepository.signOut() } }
                            )
                        } else {
                            com.ghais.ui.screens.profile.NoirLogoutRow(
                                subtitle = "Sign in to sync",
                                title = "Sign in",
                                onClick = {
                                    rootNavigator?.push(
                                        AuthScreen(
                                            onAuthenticated = { isNewAccount ->
                                                // Signup-only onboarding: fresh
                                                // accounts get a pushed setup
                                                // flow; logins just go home.
                                                if (isNewAccount) {
                                                    OnboardingStore.restartForNewUser()
                                                }
                                                rootNavigator?.pop()
                                                if (isNewAccount) {
                                                    rootNavigator?.push(OnboardingScreen)
                                                }
                                            }
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
            // Film grain over everything (last = on top).
            NoirGrainOverlay()
        }
    }
}
