package com.ghais.ui.screens.reciters.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.screens.reciters.taste.TasteStore

/**
 * AGENT P9 — cold-start gate: returning users skip the taste quiz via history import.
 *
 * Contract:
 * - quiz already completed → `content(stored)` (import never runs);
 * - quiz open BUT [hasUsableHistory] is true → imports via
 *   [importTasteFromHistory], persists once through `TasteStore.save`, and
 *   calls `content(imported)`. The save flips `completed`, so the next
 *   recomposition takes the stored branch — the import converges to stored
 *   state after one frame and never re-runs while the inputs are stable
 *   (`remember(merged, catalog)`).
 * - otherwise (true cold start) → `content(null)`, signalling the caller to
 *   keep showing the quiz.
 *
 * Side-effect discipline: the persist runs in [LaunchedEffect], never during
 * composition. [TasteStore] never throws, so no failure path can break the
 * gate — worst case the imported profile is recomputed next launch.
 *
 * ## Mount snippet (coordinator: `RecitersScreen.Content`, around the A1 quiz gate)
 * ```kotlin
 * val taste by TasteStore.taste.collectAsState()
 * var showTasteQuiz by remember { mutableStateOf(!taste.completed) }
 * val history by UserUsageRepository.history.collectAsState()
 * val followed by FollowStore.followedSlugs.collectAsState()
 * val recents by ReciterSearchHistoryStore.recentSlugs.collectAsState()
 * val browseReciters = rememberBrowseReciters()
 *
 * ColdStartGate(
 *     historySlugs = history.map { it.reciterSlug },
 *     followed = followed,
 *     recents = recents,
 *     catalog = browseReciters
 * ) { coldStartTaste ->
 *     // coldStartTaste == null → true cold start: keep today's quiz gate as-is.
 *     if (HookFlags.tasteQuizEnabled && showTasteQuiz && coldStartTaste == null) {
 *         TasteQuizSheet(onDismiss = { showTasteQuiz = false })
 *     }
 *     // coldStartTaste != null → stored quiz profile OR history import; feed it
 *     // to scoreReciter / todayDeck instead of prompting.
 * }
 * ```
 * Imports needed at the call site: `UserUsageRepository`, `FollowStore`,
 * `ReciterSearchHistoryStore`, `TasteStore`, `rememberBrowseReciters`,
 * `HookFlags`, `TasteQuizSheet` — all already imported in `RecitersScreen.kt`
 * except `ReciterSearchHistoryStore` and this gate's package.
 */
@Composable
fun ColdStartGate(
    historySlugs: List<String>,
    followed: Set<String>,
    searchRecents: List<String>,
    catalog: List<Reciter>,
    content: @Composable (TasteProfile?) -> Unit
) {
    val stored by TasteStore.taste.collectAsState()
    if (stored.completed) {
        content(stored)
        return
    }
    val warm = remember(historySlugs, followed, searchRecents) {
        hasUsableHistory(historySlugs, followed, searchRecents)
    }
    if (!warm) {
        content(null)
        return
    }
    val merged = remember(historySlugs, followed, searchRecents) {
        historySlugs + followed.toList() + searchRecents
    }
    val imported = remember(merged, catalog) {
        importTasteFromHistory(merged, catalog)
    }
    LaunchedEffect(imported) {
        TasteStore.save(imported)
    }
    content(imported)
}
