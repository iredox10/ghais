package com.ghais.data.sync

import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.UserUsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Wires automatic cloud-sync triggers around [SyncEngine].
 *
 * Two triggers, both no-ops while signed out ([SyncEngine.syncNow] itself is
 * a no-op when sync is disabled):
 * - **Login pull:** shortly after a session appears, run one pull-then-push
 *   pass so a fresh device restores cloud state into its empty local stores.
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

        // Pull-restore shortly after login.
        scope.launch {
            AuthRepository.session.collect { session ->
                if (session != null) {
                    delay(2000)
                    SyncEngine.syncNow()
                }
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
                    if (AuthRepository.session.value != null) {
                        SyncEngine.syncNow()
                    }
                }
        }
    }
}
