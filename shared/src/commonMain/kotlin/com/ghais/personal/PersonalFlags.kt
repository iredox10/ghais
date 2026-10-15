package com.ghais.personal

/**
 * AGENT P10 — personalization feature flags (glue, no UI).
 *
 * Kept separate from [com.ghais.flags.HookFlags] on purpose: that object is
 * owned by the hooks rollout (A13) and this agent must not edit existing
 * files. Same conventions — plain `var`s on an object, readable anywhere in
 * common code with zero plumbing, writable in tests and (later) from remote
 * config. Defaults are all on; UI gates on these directly, e.g.
 * `if (PersonalFlags.personalShelves) ...`. [reset] restores defaults
 * (tests / sign-out).
 */
object PersonalFlags {
    /** "For You" / personalized reciter shelves. */
    var personalShelves: Boolean = true

    /** V2 taste vector ranking (P2 seam; falls back to signal scores until it lands). */
    var tasteV2: Boolean = true

    /** Weekly listening ritual surface. */
    var weeklyRitual: Boolean = true

    /** Cold-start taste import (adopt pre-auth signals into a new account). */
    var coldImport: Boolean = true

    /** Restores every flag to its ship default. */
    fun reset() {
        personalShelves = true
        tasteV2 = true
        weeklyRitual = true
        coldImport = true
    }
}
