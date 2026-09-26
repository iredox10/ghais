package com.ghais.data.sync

import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.KhatmaStore
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.PreAuthUsage
import com.ghais.data.repository.ReciterCloudCache
import com.ghais.data.repository.ReciterSearchHistoryStore
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.SearchHistoryStore
import com.ghais.data.repository.UserUsageRepository
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Wires automatic cloud-sync triggers around [SyncEngine].
 *
 * Owner binding: every session change rebinds all user-namespaced local
 * stores (`"local"` while signed out, else the user id) so each account sees
 * only its own favorites / follows / routines / schedules / khatma plans /
 * stats / onboarding / download index.
 *
 * ## Triggers
 * All of them are no-ops while signed out or offline.
 *
 * - **Login pull (retried).** When a session appears, wait for the owner
 *   rebind, settle, then run one pull-then-push pass. The pass *waits* for a
 *   real online state (it does not sample [NetworkMonitor.isOnline] once), and
 *   a failed pass is retried with bounded backoff — a device that boots
 *   offline, or whose network settles late, still restores its account.
 * - **Pre-auth adoption.** Choices accumulated while signed out (favorites,
 *   follows, routines, schedules, khatma, goal) are copied into the account
 *   that is signed into next. Listening history and stats are snapshotted the
 *   same way but adopted *after* the pull — see [adoptPendingPreAuthUsage] for
 *   why that ordering is mandatory rather than cosmetic.
 * - **Offline→online catch-up:** when connectivity returns with an active
 *   session, run one pass (covers listening done while offline).
 * - **Debounced push:** any local change to favorites / follows / routines /
 *   schedules / khatma plans / playback state schedules a pass 8s out; rapid
 *   successive edits reset the timer so bursts of toggles collapse into a
 *   single pass.
 * - **Listening cadence:** [UserUsageRepository.stats] ticks about once per
 *   second while audio plays and is deliberately kept *out* of the debounce
 *   (it would hold the window open for the whole session). Instead it drives
 *   a bounded, rate-limited push so history and stats still reach the cloud
 *   during a long listen — see [LISTENING_PUSH_INTERVAL_MS].
 * - **Reciter catalog heartbeat:** an independent, session-free pull of the
 *   public reciter catalog every 15 minutes (plus once at startup), so a
 *   reciter published in the admin panel reaches users who are not otherwise
 *   triggering a sync.
 *
 * Deliberately excludes `AudioEngine.currentPositionMs` (too chatty — the
 * position ticker would keep every trigger's window pinned open).
 *
 * Call once from the app entry point with a caller-owned scope; idempotent.
 */
@OptIn(FlowPreview::class)
object SyncTriggers {
    private var started = false

    /** How often the public reciter catalog is re-pulled while the app runs. */
    private const val RECITER_CATALOG_REFRESH_MS = 15 * 60 * 1000L

    /**
     * Settle time between a session appearing and the first pull attempt.
     * Long enough for the rebind to finish loading every store from disk,
     * short enough to be invisible on a new device.
     */
    private const val LOGIN_PULL_SETTLE_MS = 2_000L

    /** Bounded attempts per login pull: the first try plus three retries. */
    private const val LOGIN_PULL_MAX_ATTEMPTS = 4

    /** Backoff before retry N (index = attempt - 1): 4s, 12s, 30s. */
    private val LOGIN_PULL_BACKOFF_MS = longArrayOf(4_000L, 12_000L, 30_000L)

    /**
     * How long a single attempt waits for connectivity before it gives up,
     * backs off and tries again. The wait is on the flow, so a network that
     * settles late (or flaps) resumes the same pull instead of losing it.
     */
    private const val LOGIN_PULL_ONLINE_TIMEOUT_MS = 90_000L

    /**
     * How long one attempt waits for a pass triggered elsewhere to leave
     * [SyncStatus.SYNCING] before giving up on it, so a wedged pass is never
     * stacked on top of.
     */
    private const val LOGIN_PULL_SETTLE_TIMEOUT_MS = 120_000L

    /** Quiet time a local edit must be followed by before it is pushed. */
    private const val PUSH_QUIET_MS = 8_000L

    /**
     * Bounded push cadence for listening progress: at most one pass per this
     * window while the stats snapshot is actually moving.
     *
     * Without it a listening session never has a quiet moment and nothing is
     * pushed until the user pauses; with it, a 30-minute listen pushes 6
     * times instead of 0, and a hard process kill loses at most one window of
     * listening. A pass is a full pull-then-push over every collection
     * (unconditional upserts, no per-doc change detection), so this is
     * deliberately minutes rather than seconds — retune here, nowhere else.
     */
    private const val LISTENING_PUSH_INTERVAL_MS = 5 * 60 * 1000L

    /**
     * The namespace the user-namespaced stores are bound to right now, plus
     * the pre-auth usage captured from the previous namespace when this bind
     * crossed from signed-out into a real account.
     *
     * Published by the rebind collector only after EVERY `setOwner` call has
     * returned, which makes it the ordering primitive between the two halves
     * of the login flow: the pull suspends on the first non-null value, so it
     * can never read or write a namespace the stores are not bound to yet.
     */
    private class OwnerBinding(val owner: String, val preAuthUsage: PreAuthUsage?)

    private val ownerBinding = MutableStateFlow<OwnerBinding?>(null)

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        // Owner binding + pre-auth adoption.
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
                // Listening history + stats live in the same "local" namespace
                // and are exactly as real as the choices above, so they are
                // captured here as one opaque snapshot. They are NOT adopted
                // in this block: usage is restored by the pull's
                // pull-if-empty guards, which an early adoption would defeat.
                // See [adoptPendingPreAuthUsage].
                val localUsage =
                    if (comingFromSignedOut) UserUsageRepository.snapshotPreAuthUsage() else null
                UserUsageRepository.setOwner(owner)
                FavoritesStore.setOwner(owner)
                FollowStore.setOwner(owner)
                CustomRoutinesStore.setOwner(owner)
                SchedulesStore.setOwner(owner)
                KhatmaStore.setOwner(owner)
                OnboardingStore.setOwner(owner)
                ReciterSearchHistoryStore.setOwner(owner)
                HifzMasteryStore.setOwner(owner)
                SearchHistoryStore.setOwner(owner)
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
                // Published LAST, after every store above is rebound: a reader
                // that observes this value is guaranteed to see the stores
                // already loaded from this owner's namespace.
                ownerBinding.value = OwnerBinding(owner, localUsage)
            }
        }

        // Login pull. Split out of the rebind collector above so that a slow
        // or failing pass can never delay an owner rebind, and so the pull can
        // wait for the rebind instead of merely following it.
        scope.launch {
            // Ordering gate: suspend until the rebind collector has published
            // its first binding, so the pass reads and writes the signed-in
            // namespace rather than "local".
            ownerBinding.filter { it != null }.first()
            AuthRepository.session
                .map { it?.userId }
                .distinctUntilChanged()
                // collectLatest: signing out or switching accounts cancels an
                // in-flight pull instead of letting it land on the new owner.
                .collectLatest { userId ->
                    if (userId == null) return@collectLatest
                    delay(LOGIN_PULL_SETTLE_MS)
                    // Adoption runs only behind a pass that actually
                    // completed, so it can never outrun the cloud restore.
                    if (pullLoginWithRetry(userId)) {
                        adoptPendingPreAuthUsage(userId)
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
        //
        // Every source here is user-driven and none of them moves while audio
        // simply plays, which is what makes the quiet window reachable at all.
        // (All of them are StateFlows, so each already emits only on a real
        // value change — an explicit distinctUntilChanged would be a no-op.)
        // UserUsageRepository.stats is deliberately NOT here: it counts
        // seconds, so it changes about once per second during playback and
        // would hold this debounce open for the entire listening session. It
        // drives the rate-limited push below instead.
        scope.launch {
            combine(
                combine(
                    AuthRepository.session.map { it?.userId }.distinctUntilChanged(),
                    FavoritesStore.favoriteTracks,
                    FollowStore.followedSlugs,
                ) { _, _, _ -> },
                combine(
                    CustomRoutinesStore.routines,
                    SchedulesStore.schedules,
                    KhatmaStore.plans,
                    AudioEngine.currentTrack,
                    AudioEngine.isPlaying,
                ) { _, _, _, _, _ -> },
            ) { _, _ -> }
                .debounce(PUSH_QUIET_MS)
                .collect { pushIfEligible() }
        }

        // Bounded listening cadence: replaces the debounce for the one flow
        // that is never quiet. Rate-limits push STARTS to at most one per
        // LISTENING_PUSH_INTERVAL_MS, and only for emissions that follow a
        // real stats change (stats is a StateFlow, so that is already true).
        // The timestamp is seeded at subscription, so the value every
        // StateFlow replays on subscribe can never start a pass, and an app
        // nobody listens in never pushes at all.
        scope.launch {
            var lastPushStartedAtMs = currentTimeMs()
            UserUsageRepository.stats.collect {
                val now = currentTimeMs()
                if (now - lastPushStartedAtMs < LISTENING_PUSH_INTERVAL_MS) return@collect
                lastPushStartedAtMs = now
                pushIfEligible()
            }
        }

        // Reciter catalog heartbeat: the admin panel publishes reciters to
        // Appwrite, and the catalog is the only thing that carries them to the
        // app. A full user sync is not guaranteed to fire while someone is
        // just listening, so pull the (public-read) catalog on a timer as well
        // as at startup. Cheap enough at 15 minutes, and the cache is only
        // replaced on a non-empty result, so offline runs are no-ops.
        scope.launch {
            while (true) {
                if (NetworkMonitor.isOnline.value) {
                    ReciterCloudCache.refresh()
                }
                delay(RECITER_CATALOG_REFRESH_MS)
            }
        }
    }

    /**
     * One login pull, retried.
     *
     * Each attempt waits for a real online state, then runs a full pass and
     * reads its verdict: [SyncStatus.IDLE] means it completed, [SyncStatus.ERROR]
     * earns another attempt after a backoff, and [SyncStatus.DISABLED] (Appwrite
     * unconfigured, or signed out mid-pass) is not worth retrying.
     *
     * Returns true only when a pass actually completed — the single condition
     * under which it is safe to hand pre-auth usage to this account.
     */
    private suspend fun pullLoginWithRetry(userId: String): Boolean {
        for (attempt in 0 until LOGIN_PULL_MAX_ATTEMPTS) {
            if (attempt > 0) delay(LOGIN_PULL_BACKOFF_MS[attempt - 1])
            // Gate on REACHING an online state rather than sampling the flag
            // once: a device that boots offline, or whose network settles
            // after the settle delay, waits here instead of silently skipping
            // the pull and leaving the account empty.
            if (!awaitOnline()) continue
            // The session or the binding may have moved while we waited.
            if (AuthRepository.session.value?.userId != userId) return false
            if (ownerBinding.value?.owner != userId) return false
            SyncEngine.syncNow()
            // A pass already in flight (another trigger got there first) makes
            // syncNow return immediately, leaving the status SYNCING. Wait for
            // that pass to settle so the verdict read below is its result.
            if (!awaitSyncSettled()) return false
            // IDLE = the pass completed; ERROR = one more try; DISABLED =
            // retrying cannot help.
            if (SyncEngine.status.value == SyncStatus.ERROR) continue
            return SyncEngine.status.value == SyncStatus.IDLE
        }
        return false
    }

    /** Suspends until the device is actually online, or the window expires. */
    private suspend fun awaitOnline(): Boolean {
        val online = withTimeoutOrNull(LOGIN_PULL_ONLINE_TIMEOUT_MS) {
            NetworkMonitor.isOnline.first { it }
        }
        return online == true
    }

    /**
     * Waits for a pass started by anyone to leave [SyncStatus.SYNCING]. False
     * when one is still running after the timeout.
     */
    private suspend fun awaitSyncSettled(): Boolean {
        val settled = withTimeoutOrNull(LOGIN_PULL_SETTLE_TIMEOUT_MS) {
            SyncEngine.status.first { it != SyncStatus.SYNCING }
            true
        }
        return settled == true
    }

    /**
     * Hands the pre-auth listening history and stats to the account that was
     * just pulled — but only when the cloud had nothing to say.
     *
     * The ordering here is load-bearing, not cosmetic. The sync pull restores
     * usage ONLY into an empty namespace (`SyncEngine.pullHistoryIfEmpty` /
     * `SyncEngine.pullStatsIfEmpty`), and `SyncEngine.pushHistory` deletes
     * cloud entries beyond the local set. Adopting before the pull would
     * therefore not merely race it: it would make the namespace non-empty,
     * permanently suppress the cloud restore, and let the very next push
     * prune the account's real history. Hence pull first, then fill only what
     * is still empty — which is also the order that gets a new device
     * everything from Appwrite while still handing a signed-out user's
     * own listening to the account when the cloud has none of it.
     *
     * Contract expected from `UserUsageRepository` (sibling agent):
     * - `fun snapshotPreAuthUsage(): PreAuthUsage` — capture the CURRENT
     *   namespace's history / stats / daily buckets / surah plays / raw unique
     *   id sets. Called BEFORE `setOwner`, from a suspend context, so it may
     *   be either `fun` or `suspend fun`.
     * - `fun adoptPreAuthUsage(preAuth: PreAuthUsage)` — merge the snapshot
     *   into the CURRENT (already rebound) namespace and persist it. Called
     *   from a suspend context, so it may be either `fun` or `suspend fun`.
     *   It must be idempotent per owner (the repository owns a
     *   `KEY_PREAUTH_ADOPTED` latch) and must UNION rather than replace, so
     *   the emptiness guard above stays the authority on what may be adopted.
     */
    private suspend fun adoptPendingPreAuthUsage(userId: String) {
        val binding = ownerBinding.value ?: return
        val preAuth = binding.preAuthUsage ?: return
        if (binding.owner != userId) return
        if (!usageNamespaceIsEmpty()) return
        UserUsageRepository.adoptPreAuthUsage(preAuth)
    }

    /**
     * True while this owner's usage namespace is still at fresh-install
     * defaults — exactly the condition under which the sync pull is willing to
     * restore usage from the cloud. Mirrors the guards in
     * `SyncEngine.pullHistoryIfEmpty` and `SyncEngine.isLocalStatsEmpty` (same
     * `<= 1` thresholds) so a pre-auth snapshot can never sneak in ahead of a
     * cloud restore.
     */
    private fun usageNamespaceIsEmpty(): Boolean {
        val s = UserUsageRepository.stats.value
        val statsEmpty = s.totalSecondsListened <= 0L &&
            s.minutesToday <= 0 &&
            s.daysStreak <= 1 &&
            s.uniqueRecitersCount <= 1 &&
            s.uniqueSurahsCount <= 1
        return statsEmpty && UserUsageRepository.history.value.isEmpty()
    }

    /** The one push gate for every trigger: a session, a network, one pass. */
    private suspend fun pushIfEligible() {
        if (AuthRepository.session.value != null && NetworkMonitor.isOnline.value) {
            SyncEngine.syncNow()
        }
    }

    private fun currentTimeMs(): Long = Clock.System.now().toEpochMilliseconds()
}
