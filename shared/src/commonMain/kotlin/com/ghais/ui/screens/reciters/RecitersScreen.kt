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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.ghais.ui.screens.home.HomeScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.ChartsRepository
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
import com.ghais.ui.screens.reciters.taste.TasteQuizSheet
import com.ghais.ui.screens.reciters.taste.TasteStore
import com.ghais.ui.screens.reciters.today.HeardEntry
import com.ghais.ui.screens.reciters.today.WeeklyMostHeardSection
import com.ghais.ui.screens.reciters.today.weeklyTopEntries
import com.ghais.ui.screens.reciters.today.currentEpochDay
import com.ghais.ui.screens.reciters.today.voiceOfDay
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirHeaderSearch
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

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
        val tabNavigator = LocalTabNavigator.current

        // Cloud-merged catalog (bundled seeds + Appwrite `reciters`), so a
        // reciter uploaded from the admin panel shows up here on the next
        // catalog sync instead of only in search. Reactive to the cache.
        val browseReciters = rememberBrowseReciters()

        // Engagement hook: taste quiz (A1). Refinement filters
        // live on the search screen; browse here is unfiltered discovery.
        val taste by TasteStore.taste.collectAsState()
        var showTasteQuiz by remember { mutableStateOf(!taste.completed) }

        // Header search state. WHY caller-owned: NoirHeaderSearch's contract
        // puts the expansion boolean (and the query) in the screen so sibling
        // content can react to it — here that sibling is the weighted
        // "Reciters" title, which gets squeezed to zero while the pill claims
        // the nav bar's remaining width.
        var searchExpanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        /**
         * ReciterSearchScreen entry. Trigger rules for the animated header
         * search (documented for the next maintainer):
         *  - tap the collapsed 40dp search well  -> expand the pill in place
         *    (280ms morph inside NoirHeaderSearch, auto-focused);
         *  - first non-blank keystroke OR the IME "Search" action  -> push
         *    the dedicated search screen, then collapse + clear here. No
         *    debounce, no inline filtering: the pill is a doorway, not an
         *    editor, and ReciterSearchScreen owns its own query state (it
         *    takes no query argument; the user types there instead).
         *  - trailing Close well with nothing typed -> plain collapse.
         * The blank check is what keeps NoirHeaderSearch's own clear well
         * (which reports onQueryChange("")) from re-triggering navigation.
         */
        fun openReciterSearch() {
            searchExpanded = false
            searchQuery = ""
            rootNavigator?.push(ReciterSearchScreen())
        }

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

        // Fixed editorial top: SEARCH header (nav bar with the animated
        // header search), VOICE weekly-top, then personalized rails,
        // RITUAL standalone, NATIONS last. Charts live on Home's Top Qaris
        // shelf — one home for charts, so this scroll stays lean. The deck
        // entry card that used to sit second here was removed; the deck
        // feature no longer owns a section on this screen (orderSections may
        // still emit SectionId.DECK — see the no-op branch below).
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
                    SectionId.VOICE_OF_DAY,
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp)
            ) {
                // P4-ordered sections: SEARCH header first, NATIONS last,
                // everything in between per orderSections(...).
                sectionOrder.forEach { section ->
                    when (section) {
                        SectionId.SEARCH -> item(key = "search_header") {
                            // The nav bar IS the search entry now: the old
                            // engraved full-width field and the actions-slot
                            // search button were replaced by NoirHeaderSearch
                            // living in the bar's trailing slot (single-row
                            // morph; see NoirRecitersHeader kdoc).
                            NoirRecitersHeader(
                                onBack = { tabNavigator.current = HomeScreen },
                                expanded = searchExpanded,
                                onExpandedChange = { searchExpanded = it },
                                query = searchQuery,
                                onQueryChange = { next ->
                                    searchQuery = next
                                    // First non-blank keystroke commits: push
                                    // straight into the dedicated search screen
                                    // (see openReciterSearch trigger rules).
                                    if (next.isNotBlank()) openReciterSearch()
                                },
                                onOpenSearch = ::openReciterSearch
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

                        SectionId.VOICE_OF_DAY -> item(key = "noir_voice_of_day") {
                            val epochDay = remember { currentEpochDay() }
                            val voice = remember(browseReciters, epochDay) {
                                voiceOfDay(browseReciters, epochDay)
                            }
                            // Weekly most heard (device history, last 7
                            // days); falls back to the deterministic voice
                            // when empty.
                            val nowMs = remember {
                                try {
                                    kotlin.time.Clock.System.now().toEpochMilliseconds()
                                } catch (_: Exception) {
                                    epochDay * 86_400_000L
                                }
                            }
                            val heardEntries = remember(usageHistory, nowMs, browseReciters) {
                                try {
                                    weeklyTopEntries(usageHistory, nowMs, 5)
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
                            // Weekly most heard: cloud 7-day chart first
                            // (every listener, not just this device), then
                            // the local 7-day device window; both fall back
                            // to the deterministic voice. The charts rail
                            // below triggers the cloud refresh; the snapshot
                            // flow repaints this section when it lands.
                            val chartsSnapshot by ChartsRepository.snapshot.collectAsState()
                            val globalEntries = remember(chartsSnapshot, browseReciters) {
                                try {
                                    chartsSnapshot?.topQaris7d
                                        ?.take(5)
                                        ?.mapNotNull { slug ->
                                            val reciter = try {
                                                QuranDataRepository.getBrowseReciterBySlug(slug)
                                            } catch (_: Exception) {
                                                null
                                            } ?: shelfBySlug[slug]
                                            if (reciter == null) null
                                            else HeardEntry(
                                                slug = reciter.slug,
                                                nameEn = reciter.nameEn,
                                                plays = null
                                            )
                                        }
                                        ?.takeIf { it.isNotEmpty() }
                                        ?: emptyList()
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                            val shownEntries =
                                if (globalEntries.isNotEmpty()) globalEntries else heardEntries
                            val playingTrack by AudioEngine.currentTrack.collectAsState()
                            val enginePlaying by AudioEngine.isPlaying.collectAsState()
                            WeeklyMostHeardSection(
                                entries = shownEntries,
                                fallbackReciter = voice,
                                playingSlug = playingTrack?.reciterSlug,
                                isPlaying = enginePlaying,
                                onOpenProfile = { slug ->
                                    rootNavigator?.push(ReciterProfileScreen(slug))
                                },
                                onPlay = { slug ->
                                    val reciter = shelfBySlug[slug] ?: return@WeeklyMostHeardSection
                                    try {
                                        com.ghais.analytics.HookAnalytics.track(
                                            com.ghais.analytics.HookEvents.Event.MOMENT_PLAYED,
                                            mapOf(
                                                com.ghais.analytics.HookEvents.Prop.RECITER_SLUG to slug,
                                                // "weekly_top" (was "yesterday_top"): the
                                                // source tag must follow the section's
                                                // window so dashboards can tell the
                                                // 7-day funnel apart from the old
                                                // single-day one.
                                                com.ghais.analytics.HookEvents.Prop.SOURCE to "weekly_top",
                                            ),
                                        )
                                    } catch (_: Exception) {
                                    }
                                    playReciterQueue(reciter)
                                },
                                // Canonical player wiring (matches HomeScreen):
                                // "Play now" starts the queue, this pushes the
                                // since-playing player screen. It used to be
                                // miswired to open the deck sheet, which is gone.
                                onOpenPlayer = { rootNavigator?.push(NowPlayingScreen()) }
                            )
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

                        // SectionId.DECK (and any other enum entry this
                        // screen no longer owns) renders nothing here —
                        // orderSections still emits DECK, and this else-noop
                        // keeps the when exhaustive-with-else instead of
                        // ever falling through on an unmatched section.
                        else -> Unit
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
        }
    }
}

private val NoirReelCardShape = RoundedCornerShape(28.dp)

/**
 * Reciters nav bar: back to Home left, centered title, header search right.
 * Count chips are gone — the search screen owns discovery numbers now.
 *
 * Choreography: SINGLE-ROW MORPH, no crossfade. NoirNavBar is already the
 * exact 56dp bounded row NoirHeaderSearch's placement contract asks for, so
 * the collapsed 40dp search well simply replaces the old NoirNavButton in
 * the trailing `actions` slot. Row measures non-weighted children in order
 * — so on expansion NoirHeaderSearch claims the full remaining width FIRST,
 * then the title's weight(1f) is measured last and squeezed to zero: the
 * pill grows leftward over the title while the well fades out, all driven
 * by the component's own 280ms staggered morph. This is the exact "Site A"
 * choreography NoirHeaderSearch was authored for. A Crossfade between two
 * hand-built bars was rejected: it would duplicate the bar's layout
 * (drift-prone) and fight the component's built-in well→pill animation.
 *
 * The back well stays visible while expanded (rather than hiding with the
 * title): navigation never disappears mid-search, the collapsed row stays
 * visually balanced (40dp back well ⟷ 40dp search well), and the pill
 * grows only as far as the back well — iOS search-and-cancel row style.
 *
 * Trigger rules are documented next to `openReciterSearch` in Content.
 */
@Composable
private fun NoirRecitersHeader(
    onBack: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
) {
    NoirNavBar(
        title = "Reciters",
        onBack = onBack,
        backIcon = Icons.AutoMirrored.Filled.ArrowBack,
        actions = {
            NoirHeaderSearch(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Search reciters…",
                onSearch = { onOpenSearch() }
            )
        }
    )
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

    // Full-bleed portrait card — same anatomy as the shared reel card:
    // photo header, left-aligned identity, lavender FAB + follow badge.
    val fill = if (activelyPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (activelyPlaying) GhaisNoir.Lavender else GhaisNoir.BorderCard

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(272.dp)
            .clip(NoirReelCardShape)
            .background(fill)
            .border(1.dp, border, NoirReelCardShape)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        Column(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .background(GhaisNoir.wellFill())
            ) {
                val cloudPhoto = rememberCloudPhoto(reciter.slug)
                if (photoUrl != null || cloudPhoto != null) {
                    AsyncImage(
                        model = cloudPhoto ?: photoUrl,
                        contentDescription = reciter.nameEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = reciter.nameEn.take(1),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Bottom scrim — text-zone transition, never a floating edge.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Follow badge — lavender fill when following (selected-state
                // language), clay well otherwise.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (isFollowing) GhaisNoir.lavenderFill()
                            else GhaisNoir.wellFill()
                        )
                        .border(
                            1.dp,
                            if (isFollowing) Color.Transparent else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                        contentDescription = if (isFollowing) "Following" else "Follow",
                        tint = if (isFollowing) GhaisNoir.LavenderInk else GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Identity block: left-aligned name + arabic + single meta line,
            // with the lavender FAB riding the photo boundary.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciter.nameEn,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                    Text(
                        text = reciter.nameAr,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metaLineOf(reciter, null),
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Signature FAB: always lavender, pause glyph while playing.
                Box(
                    modifier = Modifier
                        .offset(y = (-26).dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.lavenderFill())
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
                        tint = GhaisNoir.LavenderInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        // Diagonal glass sheen swept across the finished card.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
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
