package com.ghais.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.UserUsageRepository
import com.ghais.notify.NotifyChannel
import com.ghais.notify.NotifyScheduler
import com.ghais.personal.buildMajlisTracks
import com.ghais.personal.resolveMajlisReciter
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.auth.AuthScreen
import com.ghais.ui.screens.onboarding.OnboardingScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.taste.TasteQuizSheet
import com.ghais.ui.screens.reciters.taste.TasteStore
import com.ghais.ui.theme.GhaisNoir
import com.russhwolf.settings.Settings

/**
 * Activation funnel host (guest-first): Tonight's Majlis + Day-2 auth nudge.
 *
 * Rendered as the first Home item. Two jobs:
 *
 * 1. Majlis payoff — cold users (no history, nothing playing, Majlis never
 *    started) get a 1-tap queue card. The taste quiz auto-opens on top when
 *    the taste profile is incomplete; finishing it (or dismissing it) leaves
 *    the card, and tapping play builds [buildMajlisTracks] from the followed
 *    voice (or the global default) + goal/minutes and starts playback.
 * 2. Guest auth nudge — signed-out users with >= [NUDGE_HISTORY_THRESHOLD]
 *    history entries get a quiet save-your-streak card pushing [AuthScreen].
 *    Auth stays deferred, never blocking. Late-auth new accounts skip the
 *    full onboarding (quiz already captured taste; App marks onboarding done
 *    for signed-in users).
 *
 * [ACTIVATION_DONE_KEY] is device-level on purpose (no owner namespace):
 * guests have no owner yet, and a Majlis started pre-auth counts post-auth.
 */
const val NUDGE_HISTORY_THRESHOLD = 3
private const val ACTIVATION_DONE_KEY = "ghais_activation_majlis_done"

object ActivationStore {
    private val settings: Settings by lazy { Settings() }
    fun isDone(): Boolean = try {
        settings.getBoolean(ACTIVATION_DONE_KEY, false)
    } catch (_: Exception) {
        false
    }
    fun markDone() = try {
        settings.putBoolean(ACTIVATION_DONE_KEY, true)
    } catch (_: Exception) {
    }
}

/** One-shot Day-1 reminder ask (device-level, guest-safe). Never throws. */
object ReminderAskStore {
    private const val KEY = "ghais_activation_reminder_asked"
    private val settings: Settings by lazy { Settings() }
    fun asked(): Boolean = try {
        settings.getBoolean(KEY, false)
    } catch (_: Exception) {
        false
    }
    fun markAsked() = try {
        settings.putBoolean(KEY, true)
    } catch (_: Exception) {
    }
}

@Composable
fun ActivationHost(
    onOpenPlayer: () -> Unit = {},
) {
    val rootNavigator = LocalRootNavigator.current
    val taste by TasteStore.taste.collectAsState()
    val history by UserUsageRepository.history.collectAsState()
    val followed by FollowStore.followedSlugs.collectAsState()
    val session by AuthRepository.session.collectAsState()
    val goal by OnboardingStore.goal.collectAsState()
    val minutes by OnboardingStore.dailyGoalMinutes.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()

    var activationDone by remember { mutableStateOf(ActivationStore.isDone()) }
    var showQuiz by remember(taste.completed, activationDone, history.size) {
        mutableStateOf(!taste.completed && !activationDone && history.isEmpty())
    }
    var nudgeDismissed by remember { mutableStateOf(false) }

    val isCold = history.isEmpty() && currentTrack == null && !activationDone
    val showNudge = session == null && !nudgeDismissed &&
        !isCold && history.size >= NUDGE_HISTORY_THRESHOLD

    fun startMajlis() {
        try {
            val reciter = resolveMajlisReciter(followed)
            val tracks = buildMajlisTracks(reciter, goal, minutes)
            if (tracks.isNotEmpty()) {
                AudioEngine.playQueue(tracks, startIndex = 0)
                ActivationStore.markDone()
                activationDone = true
                onOpenPlayer()
            }
        } catch (_: Exception) {
        }
    }

    if (isCold) {
        val reciterName = remember(followed) {
            try {
                resolveMajlisReciter(followed).nameEn
            } catch (_: Exception) {
                "Mishary Rashid"
            }
        }
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tonight's Majlis",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$reciterName · ${minutes} min · starts with Al-Fatiha",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChromePillButton(
                        text = "Play my mix",
                        onClick = ::startMajlis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    GhostPillButton(
                        text = "Tune it",
                        onClick = { showQuiz = true }
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (showNudge) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Save your streak",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sign in to keep your progress on every device.",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChromePillButton(
                        text = "Sign in",
                        onClick = {
                            rootNavigator?.push(
                                AuthScreen(
                                    onAuthenticated = { isNewAccount ->
                                        nudgeDismissed = true
                                        // Signup-only onboarding: fresh
                                        // accounts get a pushed setup flow.
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
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    GhostPillButton(
                        text = "Later",
                        onClick = { nudgeDismissed = true }
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    var reminderAsked by remember { mutableStateOf(ReminderAskStore.asked()) }
    // Day-1 drip: after the 2nd listen, offer a habitual-hour reminder once.
    // Guest-safe, opt-in, dismissable forever.
    val showReminderAsk = !reminderAsked && !isCold && history.size >= 2
    if (showReminderAsk) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A gentle daily reminder?",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "One quiet nudge at your listening hour. Never more.",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChromePillButton(
                        text = "Remind me",
                        onClick = {
                            try {
                                NotifyScheduler.scheduleDaily(
                                    20, 0, NotifyChannel.DEVOTION_REMINDER
                                )
                            } catch (_: Exception) {
                            }
                            ReminderAskStore.markAsked()
                            reminderAsked = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    GhostPillButton(
                        text = "No thanks",
                        onClick = {
                            ReminderAskStore.markAsked()
                            reminderAsked = true
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (showQuiz) {
        TasteQuizSheet(
            onDismiss = { showQuiz = false },
            onFinished = {
                showQuiz = false
                // Quiz payoff: if still cold, roll straight into the Majlis
                // so 4 taps end in audio, not a dead end.
                if (history.isEmpty() && AudioEngine.currentTrack.value == null) {
                    startMajlis()
                }
            }
        )
    }
}
