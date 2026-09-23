package com.ghais.data.sync

import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.KhatmaStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.ReciterSearchHistoryStore
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.UserUsageRepository
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Wires automatic cloud-sync triggers around [SyncEngine].
 *
 * Owner binding: every session change rebinds all user-namespaced local
 * stores (`"local"` while signed out, else the user id) so each account sees
 * only its own favorites / follows / routines / schedules / khatma plans /
 * stats / onboarding / download index.
 *
 * Three triggers, all no-ops while signed out or offline:
 * - **Login pull:** shortly after a session appears, run one pull-then-push
 *   pass so a fresh device restores cloud state into its empty local stores.
 * - **Offline→online catch-up:** when connectivity returns with an active
 *   session, run one pass (covers listening done while offline). Fires once
 *   per transition — never on login itself (the login pull owns that).
 * - **Debounced push:** any local change to favorites / follows / routines /
 *   schedules / khatma plans / listening stats schedules a pass 8s out; rapid successive edits reset the
 *   timer so bursts of toggles collapse into a single pass.
 *
 * Deliberately excludes `AudioEngine.currentTrack` / position (too chatty —
 * position ticks would keep the debounce window pinned open).
 *
 * Call once from the app entry point with a caller-owned scope; idempotent.
 */
@OptIn(FlowPreview::class)
object SyncTriggers {
    private var started = false

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        // Owner binding + pull-restore shortly after login.
        // Offline-open users (session null, cachedSession present) bind to
        // the cached owner id so their own data shows without internet.
        var lastSessionId: String? = null
        scope.launch {
            combine(
                AuthRepository.session,
                AuthRepository.cachedSession,
            ) { session, cached -> session to cached }
                .collect { (session, cached) ->
                val owner = session?.userId ?: cached?.userId ?: "local"
                // Fresh login from signed-out state: snapshot the local ("local"
                // namespace) choices — e.g. qari followed during onboarding —
                // so they can be adopted into the new account below. Never
                // snapshot when switching directly between accounts.
                val comingFromSignedOut = session != null && lastSessionId == null
                val localFavs = if (comingFromSignedOut) FavoritesStore.favoriteTracks.value else emptyList()
                val localFollows = if (comingFromSignedOut) FollowStore.followedSlugs.value else emptySet()
                val localRoutines = if (comingFromSignedOut) CustomRoutinesStore.routines.value else emptyList()
                val localSchedules = if (comingFromSignedOut) SchedulesStore.schedules.value else emptyList()
                val localKhatmaPlans = if (comingFromSignedOut) KhatmaStore.plans.value else emptyList()
                val localGoal = if (comingFromSignedOut) OnboardingStore.goal.value else null
                val localMinutes = if (comingFromSignedOut) OnboardingStore.dailyGoalMinutes.value else 15
                UserUsageRepository.setOwner(owner)
                FavoritesStore.setOwner(owner)
                FollowStore.setOwner(owner)
                CustomRoutinesStore.setOwner(owner)
                SchedulesStore.setOwner(owner)
                KhatmaStore.setOwner(owner)
                OnboardingStore.setOwner(owner)
                ReciterSearchHistoryStore.setOwner(owner)
                QuranDownloads.setOwner(owner)
                if (comingFromSignedOut) {
                    // Adopt pre-auth choices into the new account, but only
                    // where it is still empty — never overwrite existing data.
                    if (FavoritesStore.favoriteTracks.value.isEmpty()) {
                        localFavs.forEach { FavoritesStore.add(it) }
                    }
                    if (FollowStore.followedSlugs.value.isEmpty()) {
                        localFollows.forEach { FollowStore.follow(it) }
                    }
                    if (CustomRoutinesStore.routines.value.isEmpty()) {
                        localRoutines.forEach {
                            CustomRoutinesStore.create(it.title, it.description, it.items, it.isPublic)
                        }
                    }
                    if (SchedulesStore.schedules.value.isEmpty()) {
                        localSchedules.forEach { SchedulesStore.add(it) }
                    }
                    if (KhatmaStore.plans.value.isEmpty()) {
                        localKhatmaPlans.forEach { KhatmaStore.add(it) }
                    }
                    if (localGoal != null && OnboardingStore.goal.value == null) {
                        OnboardingStore.setGoal(localGoal)
                    }
                    if (localMinutes != 15 && OnboardingStore.dailyGoalMinutes.value == 15) {
                        OnboardingStore.setDailyGoalMinutes(localMinutes)
                    }
                }
                lastSessionId = session?.userId
                if (session != null) {
                    delay(2000)
                    if (AuthRepository.session.value != null && NetworkMonitor.isOnline.value) {
                        SyncEngine.syncNow()
                    }
                }
            }
        }

        // Offline→online catch-up: when connectivity returns, retry the
        // session first (an offline-open user has session null but a cached
        // owner), then run one pass. The wasOnline guard keeps this to
        // genuine offline→online transitions — login itself is owned by the
        // pull above, and store rebinding stays there too.
        scope.launch {
            var wasOnline = true
            NetworkMonitor.isOnline.collect { online ->
                    if (online && !wasOnline) {
                        AuthRepository.refreshSession()
                        if (AuthRepository.session.value != null) {
                            SyncEngine.syncNow()
                        }
                    }
                    wasOnline = online
                }
        }

        // Debounced push: any local change schedules a sync 8s out
        // (reset on new change via debounce). currentTrack/isPlaying are
        // included so pause + track changes upload playback_state — the
        // position ticker (currentPositionMs) stays excluded as too chatty.
        // A paused position is frozen, so the delayed read is still exact.
        scope.launch {
            combine(
                combine(
                    AuthRepository.session,
                    FavoritesStore.favoriteTracks,
                    FollowStore.followedSlugs,
                ) { _, _, _ -> },
                combine(
                    CustomRoutinesStore.routines,
                    SchedulesStore.schedules,
                    UserUsageRepository.stats,
                    KhatmaStore.plans,
                ) { _, _, _, _ -> },
                combine(
                    AudioEngine.currentTrack,
                    AudioEngine.isPlaying,
                ) { _, _ -> },
            ) { _, _, _ -> }
                .debounce(8000)
                .collect {
                    if (AuthRepository.session.value != null && NetworkMonitor.isOnline.value) {
                        SyncEngine.syncNow()
                    }
                }
        }
    }
}
