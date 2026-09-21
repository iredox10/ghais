package com.ghais.data.sync

import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.UserUsageRepository
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
 * only its own favorites / follows / routines / schedules / stats /
 * onboarding / download index.
 *
 * Three triggers, all no-ops while signed out or offline:
 * - **Login pull:** shortly after a session appears, run one pull-then-push
 *   pass so a fresh device restores cloud state into its empty local stores.
 * - **Offline→online catch-up:** when connectivity returns with an active
 *   session, run one pass (covers listening done while offline). Fires once
 *   per transition — never on login itself (the login pull owns that).
 * - **Debounced push:** any local change to favorites / follows / schedules /
 *   listening stats schedules a pass 8s out; rapid successive edits reset the
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
        scope.launch {
            AuthRepository.session.collect { session ->
                val owner = session?.userId ?: "local"
                UserUsageRepository.setOwner(owner)
                FavoritesStore.setOwner(owner)
                FollowStore.setOwner(owner)
                CustomRoutinesStore.setOwner(owner)
                SchedulesStore.setOwner(owner)
                OnboardingStore.setOwner(owner)
                QuranDownloads.setOwner(owner)
                if (session != null) {
                    delay(2000)
                    if (AuthRepository.session.value != null && NetworkMonitor.isOnline.value) {
                        SyncEngine.syncNow()
                    }
                }
            }
        }

        // Offline→online catch-up: when connectivity returns with an active
        // session, run one pass. The wasSignedIn/wasOnline guard keeps this to
        // genuine offline→online transitions — login itself is owned by the
        // pull above, and store rebinding stays there too.
        scope.launch {
            var wasSignedIn = false
            var wasOnline = true
            combine(
                AuthRepository.session,
                NetworkMonitor.isOnline,
            ) { session, online -> (session != null) to online }
                .collect { (signedIn, online) ->
                    if (signedIn && online && wasSignedIn && !wasOnline) {
                        SyncEngine.syncNow()
                    }
                    wasSignedIn = signedIn
                    wasOnline = online
                }
        }

        // Debounced push: any local change schedules a sync 8s out
        // (reset on new change via debounce).
        scope.launch {
            combine(
                AuthRepository.session,
                FavoritesStore.favoriteTracks,
                FollowStore.followedSlugs,
                SchedulesStore.schedules,
                UserUsageRepository.stats,
            ) { _, _, _, _, _ -> }
                .debounce(8000)
                .collect {
                    if (AuthRepository.session.value != null && NetworkMonitor.isOnline.value) {
                        SyncEngine.syncNow()
                    }
                }
        }
    }
}
