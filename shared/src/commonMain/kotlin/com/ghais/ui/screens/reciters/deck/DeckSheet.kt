package com.ghais.ui.screens.reciters.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.analytics.HookAnalytics
import com.ghais.analytics.HookEvents
import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.reciters.monogramForReciter
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlinx.coroutines.launch

/**
 * Bottom-sheet host for the "Today's voices" deck: carousel + dock edition.
 *
 * Browse language: swipe a horizontal pager of plum carousel cards
 * ([DeckCarouselCard], neighbours peeking), filter by style tabs or a name
 * search, drag the active card DOWN to follow it into "My Deck" (the dock
 * tray pinned at the sheet bottom). Tapping a dock mini-card opens that
 * reciter's profile. The slim Skip text-button under the pager is the
 * accessible equivalent of moving past a card.
 *
 * - Follow (drag-down or Follow pill): [FollowStore.follow] +
 *   [recordDeckOutcome] (followed=true), slug appended to the dock, pager
 *   advances (last page finishes the deck). No auto-open of the profile.
 * - Skip: [recordDeckOutcome] (followed=false), pager advances.
 * - Completion: "You explored N voices today" + dock-derived follow summary
 *   + Done (same copy as before, lightly restyled).
 *
 * Presentation + deck-progress state only. Deck construction is [todayDeck];
 * the 30s preview lifecycle is owned by [DeckCarouselCard] (autoplay gated
 * on the visible page). Same function signature as the swipe-stack edition,
 * so call sites are untouched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckSheet(
    deck: List<Reciter>,
    photoFor: (String) -> String?,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    taste: TasteProfile? = null,
    affinities: Map<String, Float> = emptyMap()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dockSlugs = remember(deck) { mutableStateListOf<String>() }
    var finished by remember(deck) { mutableStateOf(false) }
    var tab by remember(deck) { mutableStateOf("All") }
    var searchOpen by remember(deck) { mutableStateOf(false) }
    var query by remember(deck) { mutableStateOf("") }

    // Tabs derived honestly from this queue: "All" + up to 3 style values
    // actually present. Fewer than 2 distinct styles -> All + search only.
    val styleTabs = remember(deck) {
        deck.map { it.style.trim() }.filter { it.isNotBlank() }.distinct().take(3)
    }
    val showStyleTabs = styleTabs.size >= 2

    val filtered = remember(deck, tab, query, showStyleTabs) {
        var list = if (tab == "All" || !showStyleTabs) deck
        else deck.filter { it.style.trim() == tab }
        val q = query.trim()
        if (q.isNotEmpty()) {
            list = list.filter { it.nameEn.contains(q, ignoreCase = true) }
        }
        list
    }
    val pagerState = rememberPagerState { filtered.size }
    LaunchedEffect(tab, query, deck) {
        if (filtered.isNotEmpty()) pagerState.scrollToPage(0)
    }
    val currentPage = if (filtered.isEmpty()) {
        0
    } else {
        pagerState.currentPage.coerceIn(0, filtered.size - 1)
    }
    val left = (filtered.size - currentPage).coerceAtLeast(0)

    val followerCounts by FollowStore.followerCounts.collectAsState()

    fun affinityOf(slug: String): Float? =
        affinities[slug] ?: affinities[normalizeDeckSlug(slug)]

    fun followAndAdvance(reciter: Reciter, page: Int) {
        FollowStore.follow(reciter.slug)
        recordDeckOutcome(reciter.slug, followed = true)
        if (!dockSlugs.contains(reciter.slug)) dockSlugs.add(reciter.slug)
        scope.launch {
            if (page >= filtered.size - 1) finished = true
            else pagerState.animateScrollToPage(page + 1)
        }
    }

    fun skipAndAdvance(reciter: Reciter, page: Int) {
        recordDeckOutcome(reciter.slug, followed = false)
        scope.launch {
            if (page >= filtered.size - 1) finished = true
            else pagerState.animateScrollToPage(page + 1)
        }
    }

    val done = finished
    LaunchedEffect(done) {
        if (done && deck.isNotEmpty()) {
            try {
                DeckDoneStore.markDone(todayEpochDay())
            } catch (_: Exception) {
            }
            HookAnalytics.track(
                HookEvents.Event.DECK_COMPLETED,
                mapOf(HookEvents.Prop.SOURCE to "discovery_deck"),
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        shape = GhaisShapes.bottomSheet,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom grabber (dragHandle = null keeps the sheet monochrome).
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill4)
            )
            Spacer(Modifier.height(12.dp))

            if (deck.isEmpty()) {
                DeckEmptyState()
            } else if (done) {
                DeckCompletionState(
                    explored = deck.size,
                    followedNames = dockSlugs.mapNotNull { slug ->
                        deck.find { it.slug == slug }?.nameEn
                    },
                    onDone = onDismiss
                )
            } else {
                DeckTabsRow(
                    styleTabs = styleTabs,
                    showStyleTabs = showStyleTabs,
                    selectedTab = tab,
                    onSelectTab = { tab = it },
                    searchOpen = searchOpen,
                    onToggleSearch = {
                        searchOpen = !searchOpen
                        query = ""
                    }
                )
                if (searchOpen) {
                    Spacer(Modifier.height(8.dp))
                    DeckSearchField(query = query, onQuery = { query = it })
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeckLeftBadge(left = left)
                }
                Spacer(Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Text(
                        text = "No voices match this filter",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 40.dp),
                        pageSpacing = 12.dp
                    ) { page ->
                        val reciter = filtered[page]
                        key(reciter.slug) {
                            DeckDragDown(
                                enabled = page == pagerState.currentPage,
                                onDragDown = { followAndAdvance(reciter, page) }
                            ) {
                                DeckCarouselCard(
                                    reciter = reciter,
                                    photoUrl = photoFor(reciter.slug),
                                    affinity = affinityOf(reciter.slug),
                                    followerCount = followerCounts[reciter.slug],
                                    isInDeck = dockSlugs.contains(reciter.slug),
                                    autoPlayPreview = page == pagerState.currentPage,
                                    onFollow = { followAndAdvance(reciter, page) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Slim accessible skip: same transition as moving past a card.
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .noirClickable(onClick = {
                                filtered.getOrNull(currentPage)?.let {
                                    skipAndAdvance(it, currentPage)
                                }
                            })
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Swipe ← → browse · drag ↓ to deck",
                        color = GhaisNoir.TextDisabled,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    }
                }
                Spacer(Modifier.height(12.dp))
                DeckDock(
                    dockSlugs = dockSlugs.toList(),
                    deck = deck,
                    photoFor = photoFor,
                    onOpenProfile = onOpenProfile
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Tabs row: "All" + style values present in this queue + search toggle. */
@Composable
private fun DeckTabsRow(
    styleTabs: List<String>,
    showStyleTabs: Boolean,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckTabPill(text = "All", selected = selectedTab == "All", onClick = { onSelectTab("All") })
            if (showStyleTabs) {
                for (style in styleTabs) {
                    DeckTabPill(
                        text = style.replaceFirstChar { it.uppercase() },
                        selected = selectedTab == style,
                        onClick = { onSelectTab(style) }
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        NoirNavButton(
            icon = Icons.Filled.Search,
            contentDescription = if (searchOpen) "Close search" else "Search voices",
            onClick = onToggleSearch
        )
    }
}

@Composable
private fun DeckTabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .then(
                if (selected) Modifier.background(GhaisNoir.lavenderFill())
                else Modifier.background(GhaisNoir.Fill2)
            )
            .border(
                1.dp,
                if (selected) GhaisNoir.Lavender.copy(alpha = 0.5f)
                else GhaisNoir.BorderCard,
                GhaisShapes.pill
            )
            .noirClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** Name filter field (case-insensitive match on nameEn). */
@Composable
private fun DeckSearchField(query: String, onQuery: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = TextStyle(color = GhaisNoir.TextPrimary, fontSize = 14.sp),
        cursorBrush = SolidColor(GhaisNoir.Lavender),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Filter by name…",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Small "N left" pill: remaining browsable count in the visible queue. */
@Composable
private fun DeckLeftBadge(left: Int) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$left left",
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * Downward-drag wrapper for the active pager page: vertical drags translate
 * the card (scale 1 → 0.94); past ~120dp on release fires [onDragDown],
 * otherwise the card springs back. Uses [detectVerticalDragGestures], which
 * only claims dy-dominant gestures, so the pager's horizontal scroll is
 * never fought. Disabled for off-screen pages.
 */
@Composable
private fun DeckDragDown(
    enabled: Boolean,
    onDragDown: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 120.dp.toPx() } }
    val offset = remember { Animatable(0f) }
    val progress = (offset.value / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offset.value
                val s = 1f - 0.06f * progress
                scaleX = s
                scaleY = s
                alpha = 1f - 0.15f * progress
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragCancel = {
                        scope.launch {
                            offset.animateTo(
                                0f,
                                spring(dampingRatio = 0.6f, stiffness = 400f)
                            )
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offset.value > thresholdPx) {
                                onDragDown()
                                offset.snapTo(0f)
                            } else {
                                offset.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.6f, stiffness = 400f)
                                )
                            }
                        }
                    },
                    onVerticalDrag = { _, dy ->
                        scope.launch {
                            offset.snapTo((offset.value + dy).coerceAtLeast(0f))
                        }
                    }
                )
            }
    ) {
        content()
    }
}

/**
 * "My Deck" tray pinned at the sheet bottom. Empty: ghost plate with the
 * drag-down hint. Populated: last 3 docked voices as overlapping mini-cards
 * (tap opens the profile) under a lavender "↑ My Deck • N" pill.
 * Dock taps never touch the pager.
 */
@Composable
private fun DeckDock(
    dockSlugs: List<String>,
    deck: List<Reciter>,
    photoFor: (String) -> String?,
    onOpenProfile: (String) -> Unit
) {
    if (dockSlugs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.cardNoir)
                .background(GhaisNoir.Fill1)
                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.cardNoir)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Swipe a card down to add to your deck",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                dockSlugs.takeLast(3).forEachIndexed { index, slug ->
                    val reciter = deck.find { it.slug == slug }
                    if (reciter != null) {
                        DeckDockMiniCard(
                            reciter = reciter,
                            photoUrl = photoFor(slug),
                            onClick = { onOpenProfile(slug) },
                            modifier = Modifier
                                .offset(x = (-10 * index).dp)
                                .zIndex(-index.toFloat())
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.lavenderFill())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = GhaisNoir.LavenderInk,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "My Deck • ${dockSlugs.size}",
                        color = GhaisNoir.LavenderInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Overlapping dock mini-card: 40dp art + name; tap opens the profile. */
@Composable
private fun DeckDockMiniCard(
    reciter: Reciter,
    photoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill3)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick = onClick)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill4),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = reciter.nameEn,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = monogramForReciter(reciter.nameEn),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Completion state: explored count + follow summary + Done. */
@Composable
private fun DeckCompletionState(
    explored: Int,
    followedNames: List<String>,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(GhaisShapes.well)
                .background(GhaisNoir.plumFill()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "You explored $explored voices today",
            color = GhaisNoir.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (followedNames.isEmpty()) {
                "Nothing followed — new voices arrive tomorrow"
            } else {
                val shown = followedNames.take(3).joinToString(", ")
                val extra = if (followedNames.size > 3) " +${followedNames.size - 3} more" else ""
                "Followed ${followedNames.size}: $shown$extra"
            },
            color = GhaisNoir.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        ChromePillButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Empty-deck state: catalog exhausted or fully explored already. */
@Composable
private fun DeckEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(GhaisShapes.well)
                .background(GhaisNoir.wellFill()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No new voices today",
            color = GhaisNoir.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "You're all caught up — check back tomorrow",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
