package com.ghais.personal

import com.ghais.analytics.HookAnalytics
import com.ghais.analytics.HookEvents
import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.screens.reciters.taste.TasteStore
import kotlin.time.Clock

/**
 * AGENT P10 — personalization facade (glue, no UI).
 *
 * Thin read/write seam over the P1 signal log ([SignalStore] /
 * [scoreSignals]) for "For You" ranking. Every entry point is pure common
 * code and NEVER throws: failures fall back to empty results / silent no-ops
 * so callers (shelves, rails) can render cold-state UI instead of crashing.
 *
 * ## Pipeline
 * `SignalStore` log → [scoreSignals] (exponential time-decay scoring) →
 * per-reciter affinity, cached for [CACHE_TTL_MS]. [topPicks] sorts a catalog
 * by that affinity; [isPersonalized] reports whether there is enough signal
 * to justify a personalized rail; `record*` fans playback intent out to both
 * the signal log and hook analytics.
 *
 * ## Write fan-out (only existing [HookEvents.Event] constants are used)
 * - [recordPreview] → `SignalStore.recordPreview` + `preview_played`
 * - [recordPlay] → `recordPlayStarted`/`recordPlayCompleted` + `moment_played`
 * - [recordSearchOpen] → `SignalStore.recordSearchOpen` + `explore_intent_used`
 */
object ForYou {
    /** Affinity cache TTL: recompute at most once a minute. */
    const val CACHE_TTL_MS: Long = 60_000L

    /** Personalized rail gate: more than this many signals counts as signal. */
    const val PERSONALIZED_SIGNAL_THRESHOLD: Int = 3

    // Plain vars (no @Volatile — no precedent for it in commonMain here, and a
    // benign recompute race is cheaper than a new expect/actual seam).
    private var cachedKey: String? = null

    private var cachedMap: Map<String, Float> = emptyMap()

    private var cachedAtMs: Long = 0L

    /**
     * Per-reciter affinity for [catalog], keyed by trimmed reciter slug.
     *
     * Sparse: only slugs with scored signal weight appear (a catalog slug
     * with no signal reads as 0f via `getOrDefault`). Cached for
     * [CACHE_TTL_MS] per catalog identity; empty catalog short-circuits to
     * empty without touching any store. Never throws — any failure returns
     * the last cached map, else empty.
     *
     * When [PersonalFlags.tasteV2] is on (default), signal scores are
     * lifted into attribute space ([buildTasteVector] + [affinityFor]) so
     * untried voices with the right attributes rank above zero; [quiz]
     * adds completed-quiz priors (null reads the live [TasteStore]).
     * Flag off = pure V1 signal scores.
     */
    fun affinities(catalog: List<Reciter>): Map<String, Float> =
        affinities(catalog, null)

    fun affinities(
        catalog: List<Reciter>,
        quiz: TasteProfile?,
    ): Map<String, Float> {
        if (catalog.isEmpty()) return emptyMap()
        return try {
            val now = nowMs()
            val key = catalogKey(catalog, quiz)
            val hitKey = cachedKey
            val hit = cachedMap
            if (hitKey != null && hitKey == key && now - cachedAtMs < CACHE_TTL_MS) {
                return hit
            }
            val slugs = catalog.map { it.slug.trim() }.filter { it.isNotEmpty() }.toSet()
            if (slugs.isEmpty()) return emptyMap()
            val scores = scoreSignals(SignalStore.exportSnapshot(), SignalStore.currentEpochDay())
            val filtered = if (!PersonalFlags.tasteV2) {
                scores.filterKeys { it in slugs }
            } else {
                // V2 blend that preserves V1 ordering: direct signal scores
                // stay exactly as scored; attribute-similar untried voices
                // (vector bonus) slot in strictly below the weakest signaled
                // voice, so discovery never outranks a direct love signal.
                val base = scores.filterKeys { it in slugs }
                try {
                    val effectiveQuiz = try {
                        quiz ?: TasteStore.taste.value
                    } catch (_: Exception) {
                        null
                    }
                    val vector = buildTasteVector(scores, catalog, effectiveQuiz)
                    val floor = base.values.filter { it > 0f }.minOrNull()?.let { it / 2f } ?: 0.05f
                    val withDiscovery = base.toMutableMap()
                    catalog.forEach { reciter ->
                        val slug = reciter.slug.trim()
                        // Only voices with NO signal entry qualify: negative
                        // skip/unfollow penalties are preserved untouched.
                        if (slug in slugs && !withDiscovery.containsKey(slug)) {
                            val bonus = try {
                                val raw = affinityFor(reciter, vector)
                                if (raw > 0f) (raw / (1f + raw)) * floor else 0f
                            } catch (_: Exception) {
                                0f
                            }
                            if (bonus > 0f) withDiscovery[slug] = bonus
                        }
                    }
                    withDiscovery.filterValues { it > 0f }
                } catch (_: Exception) {
                    base
                }
            }
            cachedKey = key
            cachedMap = filtered
            cachedAtMs = now
            filtered
        } catch (_: Exception) {
            try {
                cachedMap
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * Top-[limit] reciters from [catalog] by [affinities], best first.
     *
     * Ties keep catalog order ([sortedByDescending] is stable). [limit] is
     * coerced into `0..catalog.size`, so an oversized limit returns the whole
     * catalog and `0`/negative returns empty. Never throws — any failure
     * returns empty.
     */
    fun topPicks(catalog: List<Reciter>, limit: Int = 5): List<Reciter> {
        return try {
            if (catalog.isEmpty() || limit <= 0) return emptyList()
            val cap = limit.coerceIn(0, catalog.size)
            if (cap <= 0) return emptyList()
            val scores = affinities(catalog)
            catalog.filter { it.slug.trim().isNotEmpty() }
                .sortedByDescending { scores[it.slug.trim()] ?: 0f }.take(cap)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * True when there is enough personalization confidence to render a
     * personalized rail: more than [PERSONALIZED_SIGNAL_THRESHOLD] recorded
     * signals, or a completed taste quiz. Never throws (false on failure).
     */
    fun isPersonalized(): Boolean {
        return try {
            signalCount() > PERSONALIZED_SIGNAL_THRESHOLD || tasteCompleted()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Records a full-track play intent: `PLAY_COMPLETED` when [completed],
     * else `PLAY_STARTED`. Fans out to analytics (`moment_played`). Never
     * throws; blank slugs are dropped by the store.
     */
    fun recordPlay(slug: String, completed: Boolean) {
        try {
            if (completed) SignalStore.recordPlayCompleted(slug)
            else SignalStore.recordPlayStarted(slug)
        } catch (_: Exception) {
        }
        HookAnalytics.track(
            HookEvents.Event.MOMENT_PLAYED,
            mapOf(
                HookEvents.Prop.RECITER_SLUG to slug.trim(),
                HookEvents.Prop.SOURCE to if (completed) "completed" else "started",
            ),
        )
    }

    /**
     * Records a preview intent (`PREVIEW` + `preview_played`). Never throws.
     */
    fun recordPreview(slug: String) {
        try {
            SignalStore.recordPreview(slug)
        } catch (_: Exception) {
        }
        HookAnalytics.track(
            HookEvents.Event.PREVIEW_PLAYED,
            mapOf(HookEvents.Prop.RECITER_SLUG to slug.trim()),
        )
    }

    /**
     * Records a search-open intent (`SEARCH_OPEN` + `explore_intent_used` —
     * there is no dedicated search event in [HookEvents.Event]). Never
     * throws.
     */
    fun recordSearchOpen(slug: String) {
        try {
            SignalStore.recordSearchOpen(slug)
        } catch (_: Exception) {
        }
        HookAnalytics.track(
            HookEvents.Event.EXPLORE_INTENT_USED,
            mapOf(
                HookEvents.Prop.RECITER_SLUG to slug.trim(),
                HookEvents.Prop.SOURCE to "search",
            ),
        )
    }

    /**
     * Adopts pre-auth (guest) signals into the newly bound owner's log.
     * Capture `SignalStore.exportSnapshot()` BEFORE `setOwner(newId)`, then
     * pass it here. Gated on [PersonalFlags.coldImport]; flag off or empty
     * input adopts nothing. Invalidates the affinity cache on adoption.
     *
     * @return adopted count. Never throws.
     */
    fun adoptPreAuthSignals(preAuth: List<ReciterSignal>): Int = try {
        if (!PersonalFlags.coldImport || preAuth.isEmpty()) return 0
        val adopted = SignalStore.mergeSnapshot(preAuth)
        if (adopted > 0) invalidateCache()
        adopted
    } catch (_: Exception) {
        0
    }

    /**
     * Drops the cached affinity map. Test seam (mirrors the fresh-owner
     * isolation the store tests use); never throws.
     */
    fun invalidateCache() {
        try {
            cachedKey = null
            cachedMap = emptyMap()
            cachedAtMs = 0L
        } catch (_: Exception) {
        }
    }

    private fun signalCount(): Int = try {
        SignalStore.signals.value.size
    } catch (_: Exception) {
        0
    }

    private fun tasteCompleted(): Boolean = try {
        TasteStore.taste.value.completed
    } catch (_: Exception) {
        false
    }

    private fun catalogKey(catalog: List<Reciter>, quiz: TasteProfile?): String = try {
        val slugs = catalog.map { it.slug.trim() }.filter { it.isNotEmpty() }.sorted()
        val quizBits = try {
            "${quiz?.pace}:${quiz?.mood}:${quiz?.completed}"
        } catch (_: Exception) {
            "q?"
        }
        "${catalog.size}:${slugs.hashCode()}|v2=${PersonalFlags.tasteV2}|q=$quizBits"
    } catch (_: Exception) {
        "fallback:${catalog.size}"
    }

    private fun nowMs(): Long = try {
        Clock.System.now().toEpochMilliseconds()
    } catch (_: Exception) {
        0L
    }
}
