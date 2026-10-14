package com.ghais.flags

/**
 * A13 hook feature flags for A/B gating of hook surfaces.
 *
 * Plain `var`s on an object on purpose: readable from anywhere in common
 * code with zero plumbing, writable in tests and (later) from a remote-config
 * fetch at startup. Defaults encode the rollout plan — everything stable is
 * on, social surfaces ([leaguesEnabled], [friendshipsEnabled]) and the
 * server-dependent chart ([globalChartsEnabled]) stay off until their backend
 * (`BackendSchema`) is provisioned and their UI agents land.
 *
 * UI gates on these directly, e.g. `if (HookFlags.leaguesEnabled) ...`.
 * [reset] restores defaults (tests / sign-out).
 */
object HookFlags {
    /** Daily listening deck (swipeable ayah cards). */
    var deckEnabled: Boolean = true

    /** Reciter taste quiz → personalised reciter match. */
    var tasteQuizEnabled: Boolean = true

    /** Devotion streaks + grace days. */
    var streakEnabled: Boolean = true

    /** Daily quests + quest-done celebrations. */
    var questsEnabled: Boolean = true

    /** Khatma pacing view. */
    var khatmaPaceEnabled: Boolean = true

    /** Resurrection (returning-listener catch-up) flow. */
    var resurrectionEnabled: Boolean = true

    /** Achievements + unlock toasts. */
    var achievementsEnabled: Boolean = true

    /** Sharing ceremonies (share sheets, recap cards). */
    var ceremoniesEnabled: Boolean = true

    /** Explore-intent shortcuts + moment/recap players. */
    var exploreEnabled: Boolean = true

    /** Hook analytics emission (master switch for `HookAnalytics`). */
    var analyticsEnabled: Boolean = true

    /** XP leagues. OFF: needs `leagues` collection + pooling UI (later agent). */
    var leaguesEnabled: Boolean = false

    /** Friend shared-streaks. OFF: needs `friendships` collection + pair UI (later agent). */
    var friendshipsEnabled: Boolean = false

    /** Server-driven global charts. OFF: needs `global_charts` collection (later agent). */
    var globalChartsEnabled: Boolean = false

    /** Restores every flag to its ship default. */
    fun reset() {
        deckEnabled = true
        tasteQuizEnabled = true
        streakEnabled = true
        questsEnabled = true
        khatmaPaceEnabled = true
        resurrectionEnabled = true
        achievementsEnabled = true
        ceremoniesEnabled = true
        exploreEnabled = true
        analyticsEnabled = true
        leaguesEnabled = false
        friendshipsEnabled = false
        globalChartsEnabled = false
    }
}
