package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.reciterCountryKey
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.flags.HookFlags
import com.ghais.personal.ForYou
import com.ghais.personal.PersonalFlags
import com.ghais.personal.ShelfRow
import com.ghais.personal.SignalStore
import com.ghais.personal.buildShelves
import com.ghais.ui.screens.reciters.personal.ColdStartGate
import com.ghais.ui.screens.reciters.personal.SectionId
import com.ghais.ui.screens.reciters.personal.WeeklyRitualRow
import com.ghais.ui.screens.reciters.personal.orderSections
import com.ghais.ui.screens.reciters.personal.weekSeed
import com.ghais.ui.screens.reciters.personal.weeklyPicks
import com.ghais.player.AudioEngine
import com.ghais.ui.screens.reciters.deck.DeckEntryCard
import com.ghais.ui.screens.reciters.deck.DeckDoneStore
import com.ghais.ui.screens.reciters.deck.isDone
import com.ghais.ui.screens.reciters.deck.DeckSheet
import com.ghais.ui.screens.reciters.deck.todayDeck
import com.ghais.ui.screens.reciters.taste.TasteQuizSheet
import com.ghais.ui.screens.reciters.taste.TasteStore
import com.ghais.ui.screens.reciters.today.TopQarisRail
import com.ghais.ui.screens.reciters.today.HeardEntry
import com.ghais.ui.screens.reciters.today.VoiceOfDayCard
import com.ghais.ui.screens.reciters.today.YesterdayMostHeardSection
import com.ghais.ui.screens.reciters.today.yesterdayTopEntries
import com.ghais.ui.screens.reciters.today.currentEpochDay
import com.ghais.ui.screens.reciters.today.voiceOfDay
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Phase 4 — Noir Reciters.
 *
 * Strict Noir Glass monochrome on the shared canvas (glow zone -> #050506 +
 * ambient glow + film grain). Order, grouping, search filter and navigation
 * targets are unchanged; only the surface language changed: zero accent hue,
 * alpha-white glass cards, top-only specular hairlines, chrome pills and
 * grayscale artwork. State reads through fill elevation, weight and opacity.
 */
class RecitersScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Reciters",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        // Push onto the ROOT navigator: the local one here is a TabNavigator,
        // which can only render Tabs (pushing a plain Screen crashes in CurrentTab).
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current

        // Cloud-merged catalog (bundled seeds + Appwrite `reciters`), so a
        // reciter uploaded from the admin panel shows up here on the next
        // catalog sync instead of only in search. Reactive to the cache.
        val browseReciters = rememberBrowseReciters()

        // Engagement hooks: taste quiz (A1), deck (A4). Refinement filters
        // live on the search screen; browse here is unfiltered discovery.
        val taste by TasteStore.taste.collectAsState()
        var showTasteQuiz by remember { mutableStateOf(!taste.completed) }
        var deckOpen by remember { mutableStateOf(false) }

        // Grouped on the flag-insensitive country key so an admin-created
        // "Saudi Arabia" joins the bundled "Saudi Arabia 🇸🇦" section instead of
        // forming a second, near-identical nation. Nations and the reciters
        // inside them are both alphabetical, and the label keeps the richest
        // raw spelling in the group (the one that still carries its flag).
        val groups = remember(browseReciters) {
            browseReciters
                .groupBy { reciterCountryKey(it.country).ifBlank { "other" } }
                .map { (key, reciters) ->
                    val label = reciters
                        .map { it.country.trim() }
                        .filter { it.isNotBlank() }
                        .maxByOrNull { it.count { ch -> !ch.isLetterOrDigit() && ch != ' ' } }
                        ?.takeIf { reciterCountryKey(it) == key }
                        ?: reciters.first().country.ifBlank { "Other" }
                    val sorted = reciters.sortedBy { it.nameEn.trim().lowercase() }
                    label to sorted
                }
                .sortedBy { (label, _) -> reciterCountryKey(label).ifBlank { label.lowercase() } }
        }

        // Browse is unfiltered: collapsed shows top-6 nations by count.
        var nationsExpanded by remember(browseReciters) { mutableStateOf(false) }
        val topNations = remember(groups) {
            groups.sortedByDescending { (_, reciters) -> reciters.size }.take(6)
        }

        // P-series personalization: signals -> affinities feed every
        // curated slot below. One collection point; slots remember on it.
        val signalTick by SignalStore.signals.collectAsState()
        val affinities = remember(signalTick, browseReciters) {
            try {
                ForYou.affinities(browseReciters)
            } catch (_: Exception) {
                emptyMap()
            }
        }
        val followedSlugs by FollowStore.followedSlugs.collectAsState()
        val usageHistory by UserUsageRepository.history.collectAsState()
        val historySlugs = remember(usageHistory) {
            usageHistory.map { it.reciterSlug }.toSet()
        }
        val shelfEpochDay = remember { currentEpochDay() }
        val shelfBySlug = remember(browseReciters) {
            browseReciters.associateBy { it.slug }
        }
        // Hoisted: LazyListScope is not @Composable — remember() calls must
        // live here at Content level; item{} blocks below only render.
        val shelves = remember(
            browseReciters, affinities, followedSlugs,
            historySlugs, shelfEpochDay
        ) {
            try {
                buildShelves(
                    reciters = browseReciters,
                    affinities = affinities,
                    followed = followedSlugs,
                    history = historySlugs,
                    epochDay = shelfEpochDay
                )
            } catch (_: Exception) {
                emptyList()
            }
        }
        val ritualWeek = remember { weekSeed(currentEpochDay()) }
        val ritual = remember(
            browseReciters, ritualWeek, affinities,
            historySlugs, followedSlugs
        ) {
            try {
                weeklyPicks(
                    reciters = browseReciters,
                    affinities = affinities,
                    historySlugs = historySlugs,
                    followedSlugs = followedSlugs,
                    weekSeed = ritualWeek
                )
            } catch (_: Exception) {
                emptyList()
            }
        }

        // Fixed editorial top (Spotify Home rule): SEARCH header, DECK
        // hero, VOICE yesterday-top, CHARTS, then personalized rails,
        // RITUAL standalone, NATIONS last. Deck-done state only changes
        // the entry card copy, never the position.
        val deckDoneDay by DeckDoneStore.doneDay.collectAsState()
        val orderDay = remember { currentEpochDay() }
        val deckDoneToday = remember(deckDoneDay, orderDay) {
            try {
                isDone(deckDoneDay, orderDay)
            } catch (_: Exception) {
                false
            }
        }
        val sectionOrder = remember(
            followedSlugs, historySlugs, taste.completed
        ) {
            try {
                orderSections(
                    hasFollows = followedSlugs.isNotEmpty(),
                    hasHistory = historySlugs.isNotEmpty(),
                    hasTaste = taste.completed
                )
            } catch (_: Exception) {
                listOf(
                    SectionId.SEARCH, SectionId.DECK,
                    SectionId.VOICE_OF_DAY, SectionId.CHARTS,
                    SectionId.RITUAL, SectionId.NATIONS
                )
            }
        }
        val yourVoicesShelves = remember(shelves) {
            shelves.filter { it.id == "your-voices" }
        }
        val forYouShelves = remember(shelves) {
            shelves.filter { it.id != "your-voices" }
        }
        // FOR_YOU renders only on real confidence (>3 signals or completed
        // taste quiz) — history alone isn't enough. orderSections still
        // decides position; this decides visibility.
        val forYouConfident = remember(signalTick, taste.completed, historySlugs) {
            try {
                ForYou.isPersonalized()
            } catch (_: Exception) {
                historySlugs.isNotEmpty() || taste.completed
            }
        }

        // Hoisted deck preview: same recipe as the sheet host below, so
        // the entry card matches the deck that opens. (LazyListScope is
        // not @Composable — remember() calls live here at Content level.)
        val deckPreview = remember(
            browseReciters, followedSlugs, usageHistory, taste, affinities
        ) {
            try {
                todayDeck(
                    reciters = browseReciters,
                    taste = taste,
                    history = historySlugs,
                    followedSlugs = followedSlugs,
                    affinities = affinities
                )
            } catch (_: Exception) {
                emptyList()
            }
        }
        val deckAffinityName = remember(affinities, shelfBySlug, followedSlugs) {
            try {
                followedSlugs.firstNotNullOfOrNull { shelfBySlug[it]?.nameEn }
                    ?: affinities.keys.firstNotNullOfOrNull { shelfBySlug[it]?.nameEn }
            } catch (_: Exception) {
                null
            }
        }

        // Shared play recipe (nation reels + personal shelves + ritual).
        fun playReciterQueue(reciter: Reciter) {
            try {
                SignalStore.recordPlayStarted(reciter.slug)
            } catch (_: Exception) {
            }
            val surahs = QuranDataRepository.getSurahsForReciter(reciter)
                .filter { reciter.isSurahAvailable(it.id) }
            val tracks = surahs.map { surah ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(surah.id),
                    textUthmani = "",
                    durationMs = surah.ayahsCount * 15_000L
                )
            }
            if (tracks.isNotEmpty()) {
                AudioEngine.playQueue(tracks, startIndex = 0)
            }
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp)
            ) {
                // P4-ordered sections: SEARCH header first, NATIONS last,
                // everything in between per orderSections(...).
                sectionOrder.forEach { section ->
                    when (section) {
                        SectionId.SEARCH -> item(key = "search_header") {
                            NoirRecitersHeader(
                                totalReciters = browseReciters.size,
                                nationCount = groups.size
                            )
                            Spacer(Modifier.height(16.dp))
                            NoirRecitersSearch(
                                onClick = { rootNavigator?.push(ReciterSearchScreen()) }
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        SectionId.YOUR_VOICES -> if (PersonalFlags.personalShelves) {
                            yourVoicesShelves.forEach { shelf ->
                                item(key = "noir_shelf_${shelf.id}") {
                                    ShelfRow(
                                        shelf = shelf,
                                        reciterBySlug = shelfBySlug,
                                        photoFor = ::photoForSlug,
                                        followedSlugs = followedSlugs,
                                        onReciter = { slug ->
                                            rootNavigator?.push(ReciterProfileScreen(slug))
                                        },
                                        onPlayReciter = ::playReciterQueue,
                                        onToggleFollow = { slug -> FollowStore.toggle(slug) }
                                    )
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }

                        SectionId.FOR_YOU -> if (PersonalFlags.personalShelves && forYouConfident) {
                            forYouShelves.forEach { shelf ->
                                item(key = "noir_shelf_${shelf.id}") {
                                    ShelfRow(
                                        shelf = shelf,
                                        reciterBySlug = shelfBySlug,
                                        photoFor = ::photoForSlug,
                                        followedSlugs = followedSlugs,
                                        onReciter = { slug ->
                                            rootNavigator?.push(ReciterProfileScreen(slug))
                                        },
                                        onPlayReciter = ::playReciterQueue,
                                        onToggleFollow = { slug -> FollowStore.toggle(slug) }
                                    )
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }

                        // Weekly ritual (P8): standalone section, fresh every
                        // Monday. Self-hides when its picks are empty.
                        SectionId.RITUAL -> if (PersonalFlags.weeklyRitual && ritual.isNotEmpty()) {
                            item(key = "slot_weekly_ritual") {
                                WeeklyRitualRow(
                                    reciters = ritual,
                                    weekSeed = ritualWeek,
                                    photoFor = ::photoForSlug,
                                    onOpenReciter = { slug ->
                                        rootNavigator?.push(ReciterProfileScreen(slug))
                                    },
                                    onPlayReciter = ::playReciterQueue,
                                    isFollowing = { it in followedSlugs },
                                    onFollowClick = { FollowStore.toggle(it) }
                                )
                                Spacer(Modifier.height(20.dp))
                            }
                        }

                        SectionId.DECK -> if (HookFlags.deckEnabled) {
                            item(key = "slot_deck") {
                                LaunchedEffect(Unit) {
                                    try {
                                        com.ghais.analytics.HookAnalytics.track(
                                            com.ghais.analytics.HookEvents.Event.DECK_ENTRY_SEEN,
                                            mapOf(com.ghais.analytics.HookEvents.Prop.SOURCE to "reciters_top"),
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                                DeckEntryCard(
                                    onOpen = { deckOpen = true },
                                    preview = deckPreview.take(3),
                                    totalCount = deckPreview.size,
                                    affinityLabel = deckAffinityName?.substringBefore(" ")?.takeIf { it.isNotBlank() },
                                    doneToday = deckDoneToday
                                )
                                Spacer(Modifier.height(20.dp))
                            }
                        }

                        SectionId.VOICE_OF_DAY -> item(key = "noir_voice_of_day") {
                            val epochDay = remember { currentEpochDay() }
                            val voice = remember(browseReciters, epochDay) {
                                voiceOfDay(browseReciters, epochDay)
                            }
                            // Yesterday's most heard (device history); falls
                            // back to the deterministic voice when empty.
                            val nowMs = remember {
                                try {
                                    kotlin.time.Clock.System.now().toEpochMilliseconds()
                                } catch (_: Exception) {
                                    epochDay * 86_400_000L
                                }
                            }
                            val heardEntries = remember(usageHistory, nowMs, browseReciters) {
                                try {
                                    yesterdayTopEntries(usageHistory, nowMs, 5)
                                        ?.mapNotNull { top ->
                                            val reciter = shelfBySlug[top.slug]
                                                ?: shelfBySlug[top.slug.trim().lowercase()]
                                            if (reciter == null) null
                                            else HeardEntry(
                                                slug = reciter.slug,
                                                nameEn = reciter.nameEn,
                                                plays = top.plays
                                            )
                                        }
                                        ?.takeIf { it.isNotEmpty() }
                                        ?: emptyList()
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                            val playingTrack by AudioEngine.currentTrack.collectAsState()
                            val enginePlaying by AudioEngine.isPlaying.collectAsState()
                            YesterdayMostHeardSection(
                                entries = heardEntries,
                                fallbackReciter = voice,
                                playingSlug = playingTrack?.reciterSlug,
                                isPlaying = enginePlaying,
                                onOpenProfile = { slug ->
                                    rootNavigator?.push(ReciterProfileScreen(slug))
                                },
                                onPlay = { slug ->
                                    val reciter = shelfBySlug[slug] ?: return@YesterdayMostHeardSection
                                    try {
                                        com.ghais.analytics.HookAnalytics.track(
                                            com.ghais.analytics.HookEvents.Event.MOMENT_PLAYED,
                                            mapOf(
                                                com.ghais.analytics.HookEvents.Prop.RECITER_SLUG to slug,
                                                com.ghais.analytics.HookEvents.Prop.SOURCE to "yesterday_top",
                                            ),
                                        )
                                    } catch (_: Exception) {
                                    }
                                    playReciterQueue(reciter)
                                },
                                onOpenPlayer = { deckOpen = true }
                            )
                        }

                        SectionId.CHARTS -> item(key = "noir_top_qaris_rail") {
                            TopQarisRail(
                                onReciter = { slug ->
                                    rootNavigator?.push(ReciterProfileScreen(slug))
                                }
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        SectionId.NATIONS -> if (groups.isEmpty()) {
                            item {
                                NoirRecitersEmpty()
                            }
                        } else {
                            item(key = "browse_nations_header") {
                                NoirSectionHeader(
                                    label = "Browse nations • ${groups.size}",
                                    actionLabel = if (nationsExpanded) "Show less"
                                    else "All ${groups.size} nations",
                                    onAction = { nationsExpanded = !nationsExpanded }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            val visibleGroups = if (nationsExpanded) groups else topNations
                            visibleGroups.forEachIndexed { index, (nation, reciters) ->
                                item(key = "noir_reel_$nation") {
                                    NoirNationReel(
                                        nation = nation,
                                        reciters = reciters,
                                        photoFor = ::photoForSlug,
                                        onSeeAll = { rootNavigator?.push(RegionRecitersScreen(nation)) },
                                        onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) },
                                        onPlayReciter = { reciter ->
                                            playReciterQueue(reciter)
                                        }
                                    )
                                    if (index < visibleGroups.size - 1) {
                                        Spacer(Modifier.height(20.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(GhaisNoir.BorderGhost)
                                        )
                                        Spacer(Modifier.height(20.dp))
                                    }
                                }
                            }
                            if (!nationsExpanded && groups.size > topNations.size) {
                                item(key = "browse_nations_expander") {
                                    Spacer(Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(GhaisShapes.row)
                                            .background(GhaisNoir.Fill2)
                                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.row)
                                            .noirClickable { nationsExpanded = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "All ${groups.size} nations",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // First-visit taste quiz (A1) with cold-start import (P9):
            // returning users with real history skip the quiz entirely.
            if (HookFlags.tasteQuizEnabled && showTasteQuiz) {
                val searchRecents by com.ghais.data.repository.ReciterSearchHistoryStore.recentSlugs.collectAsState()
                ColdStartGate(
                    historySlugs = usageHistory.map { it.reciterSlug },
                    followed = followedSlugs,
                    searchRecents = searchRecents,
                    catalog = browseReciters
                ) { coldStartTaste ->
                    if (coldStartTaste == null) {
                        TasteQuizSheet(onDismiss = { showTasteQuiz = false })
                    } else {
                        showTasteQuiz = false
                    }
                }
            }

            // Swipe deck host (A4, affinity-aware v2 via P7).
            if (deckOpen) {
                val deck = remember(
                    browseReciters, followedSlugs, usageHistory, taste, affinities
                ) {
                    todayDeck(
                        reciters = browseReciters,
                        taste = taste,
                        history = historySlugs,
                        followedSlugs = followedSlugs,
                        affinities = affinities
                    )
                }
                DeckSheet(
                    deck = deck,
                    taste = taste,
                    photoFor = ::photoForSlug,
                    onDismiss = { deckOpen = false },
                    onOpenProfile = { slug ->
                        rootNavigator?.push(ReciterProfileScreen(slug))
                    },
                    affinities = affinities
                )
            }
        }
    }
}

private val NoirReelCardShape = RoundedCornerShape(28.dp)
private val NoirAvatarShape = RoundedCornerShape(24.dp)

/**
 * Editorial two-line headline ("Browse" light / "Reciters" bold) matching the
 * Home "Return to / Your Quran" treatment, with ghost count chips.
 */
@Composable
private fun NoirRecitersHeader(
    totalReciters: Int,
    nationCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Browse",
            style = GhaisTypography.displayEditorial,
            maxLines = 1
        )
        Text(
            text = "Reciters",
            style = GhaisTypography.displayEditorialBold,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Voices of the revelation",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoirRecitersChip(
                text = if (totalReciters == 1) "1 reciter" else "$totalReciters reciters"
            )
            NoirRecitersChip(
                text = if (nationCount == 1) "1 nation" else "$nationCount nations"
            )
        }
    }
}

/** Non-interactive ghost count chip — informational, no press affordance. */
@Composable
private fun NoirRecitersChip(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Engraved search field — carved-in recessed surface (dark inset fill +
 * ~5% rim), search glyph at 38%, hint at 24%. Read-only affordance:
 * tapping pushes the dedicated [ReciterSearchScreen] instead of filtering
 * inline.
 */
@Composable
private fun NoirRecitersSearch(
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .noirClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = "",
            onValueChange = {},
            singleLine = true,
            enabled = false,
            readOnly = true,
            textStyle = TextStyle(
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp
            ),
            decorationBox = {
                Text(
                    text = "Search reciters…",
                    color = GhaisNoir.TextDisabled,
                    fontSize = 15.sp
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoirNationReel(
    nation: String,
    reciters: List<Reciter>,
    photoFor: (String) -> String?,
    onSeeAll: () -> Unit,
    onReciter: (String) -> Unit,
    onPlayReciter: (Reciter) -> Unit = {}
) {
    val followedSlugs by FollowStore.followedSlugs.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        NoirSectionHeader(
            label = "$nation • ${reciters.size}",
            actionLabel = "See all",
            onAction = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = reciters,
                key = { it.catalogKey() }
            ) { reciter ->
                NoirReciterCard(
                    reciter = reciter,
                    photoUrl = photoFor(reciter.slug),
                    onClick = { onReciter(reciter.slug) },
                    onPlayClick = { onPlayReciter(reciter) },
                    isFollowing = reciter.slug in followedSlugs,
                    onFollowClick = { FollowStore.toggle(reciter.slug) }
                )
            }
        }
    }
}

/**
 * Noir reel card: soft glass fill + 1px ghost border + top-only specular.
 * Active (now-playing) state reads through stronger wash + bright hairline,
 * never hue. Artwork is grayscale under a darkening scrim; monogram fallback
 * when there is no photo.
 */
@Composable
private fun NoirReciterCard(
    reciter: Reciter,
    photoUrl: String?,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {}
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug
    val activelyPlaying = isCurrentReciter && isPlaying

    val fill = if (activelyPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(254.dp)
            .clip(NoirReelCardShape)
            .background(fill)
            .border(1.dp, border, NoirReelCardShape)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        // Sheen sweep + bottom scrim for depth (monochrome only).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.32f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center
            ) {
                val cloudPhoto = rememberCloudPhoto(reciter.slug)
                if (photoUrl != null || cloudPhoto != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(NoirAvatarShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, NoirAvatarShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = cloudPhoto ?: photoUrl,
                            contentDescription = reciter.nameEn,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(NoirAvatarShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, NoirAvatarShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reciter.nameEn.take(1),
                            color = GhaisNoir.TextPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play / pause pill: chrome when actively playing, smoked glass otherwise.
                val playModifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (activelyPlaying) Modifier.background(GhaisNoir.chromeFill())
                        else Modifier.background(Color.Black.copy(alpha = 0.70f))
                    )
                Box(
                    modifier = playModifier
                        .border(
                            1.dp,
                            if (activelyPlaying) Color.White.copy(alpha = 0.35f)
                            else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable {
                            if (activelyPlaying) {
                                AudioEngine.pause()
                            } else if (isCurrentReciter) {
                                AudioEngine.resume()
                            } else {
                                onPlayClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (activelyPlaying) "Pause" else "Play",
                        tint = if (activelyPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Follow badge — clay well; following reads via bright glyph +
                // chromium dot, unfollowed via 38% glyph. No hue anywhere.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                        .noirClickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                            contentDescription = if (isFollowing) "Following" else "Follow",
                            tint = if (isFollowing) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (isFollowing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GhaisNoir.chromeFill())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 44.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${reciter.riwayah} • ${reciter.style}",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Ghost empty state — muted mosque well + tertiary copy, no hue. */
@Composable
private fun NoirRecitersEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirReelCardShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirReelCardShape)
            .topSpecular()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(GhaisNoir.wellFill(), GhaisShapes.well)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mosque,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No reciters found",
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Try a different nation",
            color = GhaisNoir.TextTertiary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
