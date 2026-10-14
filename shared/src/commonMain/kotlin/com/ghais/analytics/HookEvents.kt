package com.ghais.analytics

/**
 * A13 hook analytics: event-name constants + a pluggable tracker.
 *
 * UI/feature code calls [HookAnalytics.track] with one of the [Event] names
 * and a small `Map<String, String>` of properties. The default [tracker] is
 * [NoopTracker] (events are dropped), so features can ship instrumented
 * before any analytics SDK lands; the app entry point (or a later agent)
 * swaps in a real [HookTracker] once. All entry points swallow exceptions —
 * tracking must never break playback or UI.
 */
object HookEvents {
    /** Canonical hook event names (snake_case, stable across client/server). */
    object Event {
        const val PREVIEW_PLAYED = "preview_played"
        const val PREVIEW_FOLLOWED = "preview_followed"
        const val DECK_COMPLETED = "deck_completed"
        const val DECK_CARD_FOLLOWED = "deck_card_followed"
        const val TASTE_COMPLETED = "taste_completed"
        const val STREAK_MILESTONE = "streak_milestone"
        const val QUEST_DONE = "quest_done"
        const val CEREMONY_SHARED = "ceremony_shared"
        const val KHATMA_PACE_VIEWED = "khatma_pace_viewed"
        const val RESURRECTION_PLAYED = "resurrection_played"
        const val ACHIEVEMENT_UNLOCKED = "achievement_unlocked"
        const val EXPLORE_INTENT_USED = "explore_intent_used"
        const val MOMENT_PLAYED = "moment_played"
        const val RECAP_VIEWED = "recap_viewed"
    }

    /** Well-known property keys (keep values short strings). */
    object Prop {
        const val RECITER_SLUG = "reciter_slug"
        const val SURAH_ID = "surah_id"
        const val STREAK_DAYS = "streak_days"
        const val QUEST_ID = "quest_id"
        const val ACHIEVEMENT_ID = "achievement_id"
        const val SOURCE = "source"
    }
}

/** Sink for hook analytics events; implement against any SDK. */
interface HookTracker {
    fun track(event: String, props: Map<String, String>)
}

/** Default sink: drops every event. */
object NoopTracker : HookTracker {
    override fun track(event: String, props: Map<String, String>) = Unit
}

/**
 * Process-wide analytics facade. Features call [track]; platform setup
 * replaces [tracker] once (e.g. in the Application class).
 */
object HookAnalytics {
    var tracker: HookTracker = NoopTracker

    /** Delegates to [tracker], swallowing any exception. Never throws. */
    fun track(event: String, props: Map<String, String> = emptyMap()) {
        try {
            tracker.track(event, props)
        } catch (_: Exception) {
        }
    }
}
