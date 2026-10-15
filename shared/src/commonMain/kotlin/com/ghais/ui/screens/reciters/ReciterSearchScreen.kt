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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.ReciterSearchHistoryStore
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.personal.rankReciters
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.filter.FilterChipsRow
import com.ghais.ui.screens.reciters.filter.ReciterFilterState
import com.ghais.ui.screens.reciters.filter.applyFilters
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlinx.coroutines.delay

/**
 * Noir reciter search — monochrome chrome with natural-colour portraits.
 *
 * Top: back well + autofocused engraved search field. Blank query shows the
 * "Recent searches" section (rows resolved via [QuranDataRepository.getReciterBySlug],
 * per-item remove, "Clear all" ghost action, ghost-well empty state). Typing
 * shows live results with the same matching as RecitersScreen
 * (nameEn / nameAr / country contains, ignoreCase, ~300ms debounce) plus a
 * transliteration-tolerant pass ([reciterMatchesNormalizedQuery]): both the
 * query and the candidate nameEn/slug/country go through
 * [normalizeReciterSearchText] (lowercase, diacritic + Arabic-harakat strip,
 * common variant unification, non-alphanumeric strip), so "Ahmed" finds
 * "Ahmad", "abdul basit" finds "abdul-basit", "Mahmoud" finds "Mahmud" and
 * "Qaree" finds "Qari". The plain contains check stays as the fallback (it
 * is the only path that matches Arabic script).
 *
 * Row tap records the slug in [ReciterSearchHistoryStore] and pushes
 * [ReciterProfileScreen]. The trailing play disc keeps the direct-play path:
 * builds the full-surah queue for that reciter, starts [AudioEngine.playQueue]
 * and pushes [NowPlayingScreen] — the same play wiring as RecitersScreen's
 * onPlayReciter block. History is recorded on both profile-open and play.
 *
 * Owner binding: [com.ghais.data.sync.SyncTriggers] owns store rebinding —
 * this screen never calls setOwner (follow-up).
 *
 * AGENT P6 — taste-ranked results: matched candidates are ordered by
 * [rankReciters] (affinity desc, followed first, then alphabetical) via the
 * local [currentAffinities] provider, and the top-3 affine hits carry a
 * small "For you" badge. Filtering, recents, play and open paths are
 * otherwise unchanged.
 */

/**
 * AGENT P6 — local affinity provider (slug -> score, higher = more loved).
 *
 * Currently unbound (empty = keep catalog order modulo followed/alpha
 * tiebreaks) until the coordinator wires the real P1/P2 source.
 *
 * ONE-LINE SWAP (coordinator): replace the body with the live source, e.g.
 * `fun currentAffinities(): Map<String, Float> = SignalStore.scores()`
 * (P1 `scoreSignals` totals keyed by reciter slug).
 */
fun currentAffinities(): Map<String, Float> = try {
    com.ghais.personal.SignalStore.scores()
} catch (_: Exception) {
    emptyMap()
}

class ReciterSearchScreen(
    private val initialFilters: ReciterFilterState = ReciterFilterState(),
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        var query by remember { mutableStateOf("") }
        var debouncedQuery by remember { mutableStateOf("") }
        // Refinement filters (moved off the browse scroll): docked under
        // the query field, always visible, combined before ranking.
        var filterState by remember { mutableStateOf(initialFilters) }

        LaunchedEffect(query) {
            delay(300)
            debouncedQuery = query
        }

        val recentSlugs by ReciterSearchHistoryStore.recentSlugs.collectAsState()
        // AGENT P6 — taste inputs for ranking (affinity map + followed set).
        // Live P1 signal scores; signalTick re-triggers recompute on change.
        val signalTick by com.ghais.personal.SignalStore.signals.collectAsState()
        val affinities = remember(signalTick) { currentAffinities() }
        val followedSlugs by FollowStore.followedSlugs.collectAsState()
        // Reactive cloud-merged catalog: a reciter uploaded from the admin
        // panel becomes searchable (and its history row resolvable) as soon
        // as the catalog syncs, without restarting the app.
        val browseReciters = rememberBrowseReciters()

        val recentReciters = remember(recentSlugs, browseReciters) {
            // Collapse aliases to the browse winner so one human is one row
            // (and so catalogKey keys below stay unique).
            recentSlugs
                .mapNotNull { slug -> browseReciters.firstOrNull { it.slug.equals(slug, ignoreCase = true) } }
                .distinctBy { it.catalogKey() }
        }

        val results = remember(debouncedQuery, browseReciters, affinities, followedSlugs, filterState) {
            val q = debouncedQuery.trim()
            // Filters first (hard constraint, shrinks the set), then text
            // match, then affinity ranking — so ForYou badges always agree
            // with the visible order.
            val refined = applyFilters(browseReciters, filterState)
            if (q.isBlank()) {
                if (!filterState.hasActiveFilters()) emptyList()
                else rankReciters(
                    candidates = refined,
                    affinities = affinities,
                    followed = followedSlugs,
                )
            } else rankReciters(
                candidates = refined.filter {
                    it.nameEn.contains(q, ignoreCase = true) ||
                        it.nameAr.contains(q, ignoreCase = true) ||
                        it.country.contains(q, ignoreCase = true) ||
                        it.slug.contains(q, ignoreCase = true) ||
                        reciterMatchesNormalizedQuery(it, q)
                },
                affinities = affinities,
                followed = followedSlugs,
            )
        }

        // AGENT P6 — "For you" badge set: top-3 ranked hits with positive
        // affinity (same exact-then-normalized lookup as rankReciters, so the
        // badge never disagrees with the order).
        val forYouSlugs = remember(results, affinities) {
            fun affinityOf(slug: String): Float =
                affinities[slug] ?: affinities[slug.trim().lowercase()] ?: 0f
            results.take(3)
                .filter { affinityOf(it.slug) > 0f }
                .map { it.slug }
                .toSet()
        }

        // Shared queue builder — mirrors RecitersScreen onPlayReciter.
        // getSurahsForReciter is already availability-filtered; the explicit
        // isSurahAvailable guard keeps us safe if that ever changes.
        fun openProfile(reciter: Reciter) {
            ReciterSearchHistoryStore.record(reciter.slug)
            rootNavigator.push(ReciterProfileScreen(reciter.slug))
        }

        fun playReciter(reciter: Reciter) {
            ReciterSearchHistoryStore.record(reciter.slug)
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
                rootNavigator.push(NowPlayingScreen())
            }
        }

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                // Top bar — IconWell back + autofocused engraved search.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.noirClickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        IconWell(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            size = 40.dp,
                            iconSize = 20.dp,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ReciterSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Refinement lives here, not on the browse scroll.
                FilterChipsRow(
                    reciters = browseReciters,
                    state = filterState,
                    onStateChange = { filterState = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (query.isBlank() && !filterState.hasActiveFilters()) {
                    NoirSectionHeader(
                        label = "Recent searches",
                        actionLabel = "Clear all".takeIf { recentReciters.isNotEmpty() },
                        onAction = { ReciterSearchHistoryStore.clear() }
                            .takeIf { recentReciters.isNotEmpty() }
                    )
                    if (recentReciters.isEmpty()) {
                        NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconWell(
                                    icon = Icons.Default.Search,
                                    size = 56.dp,
                                    iconSize = 26.dp,
                                    contentDescription = null,
                                    tint = GhaisNoir.TextTertiary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No recent searches",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reciters you play will show up here",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentReciters, key = { "recent-${it.catalogKey()}" }) { reciter ->
                                RecentReciterRow(
                                    reciter = reciter,
                                    onOpen = { openProfile(reciter) },
                                    onPlay = { playReciter(reciter) },
                                    onRemove = { ReciterSearchHistoryStore.remove(reciter.slug) }
                                )
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconWell(
                                icon = Icons.Default.Search,
                                size = 56.dp,
                                iconSize = 26.dp,
                                contentDescription = null,
                                tint = GhaisNoir.TextTertiary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (query.isBlank()) "No voices match those filters"
                                else "No results for \"$query\"",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try a reciter name or country",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    NoirSectionHeader(
                        label = "Reciters",
                        actionLabel = "${results.size} shown",
                        onAction = {}
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // catalogKey (not bare slug) so two catalog variants can
                        // never collide — a duplicate key is a fatal Compose crash.
                        items(results, key = { "result-${it.catalogKey()}" }) { reciter ->
                            ResultReciterRow(
                                reciter = reciter,
                                onOpen = { openProfile(reciter) },
                                onPlay = { playReciter(reciter) },
                                showForYouBadge = reciter.slug in forYouSlugs,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Agent A6 — transliteration-tolerant reciter matching (common-safe: no
 * `java.text`, so it runs on every platform and in commonTest).
 *
 * [normalizeReciterSearchText] folds both the query and the candidate fields
 * to a common spelling: lowercase, Latin-diacritic fold, Arabic harakat /
 * tatweel / combining-mark strip, common transliteration-variant unification
 * (`ahmed` -> `ahmad`, `baset` -> `basit`, `ou`/`oo` -> `u`, `ee` -> `i`),
 * then non-alphanumeric strip (keeps `a-z0-9` + Arabic letters) so
 * hyphen/space/case variants (`abdul-basit` vs `abdul basit` vs
 * `AbdulBaset`) collapse to one key.
 *
 * [reciterMatchesNormalizedQuery] reports the normalized-contains hit over
 * nameEn/slug/country. Callers keep the plain `contains` check as the
 * fallback — it is the only path matching un-normalized Arabic script.
 */
fun normalizeReciterSearchText(raw: String): String {
    var s = raw.lowercase().trim()
    if (s.isEmpty()) return ""
    // Strip Arabic harakat (064B-065F), superscript alef (0670), tatweel
    // (0640) and Latin combining marks (0300-036F) before folding.
    s = s.filter { c ->
        val code = c.code
        !(code in 0x064B..0x065F || code == 0x0670 || code == 0x0640 || code in 0x0300..0x036F)
    }
    val folded = StringBuilder(s.length)
    for (c in s) folded.append(foldLatinDiacritic(c))
    s = folded.toString()
    // Longest-first variant unification (order matters: "ahmed" before the
    // generic folds so the full-name variant wins).
    s = s.replace("ahmed", "ahmad")
        .replace("baset", "basit")
        .replace("ou", "u")
        .replace("oo", "u")
        .replace("ee", "i")
    return s.filter { c ->
        c in 'a'..'z' || c in '0'..'9' || c.code in 0x0621..0x064A
    }
}

/** Latin diacritic fold for [normalizeReciterSearchText]; unknown chars pass through. */
private fun foldLatinDiacritic(c: Char): String = when (c) {
    'à', 'á', 'â', 'ã', 'ä', 'å', 'ā', 'ă', 'ą', 'ǎ', 'ȁ', 'ȃ' -> "a"
    'è', 'é', 'ê', 'ë', 'ē', 'ĕ', 'ė', 'ę', 'ě', 'ȅ', 'ȇ' -> "e"
    'ì', 'í', 'î', 'ï', 'ĩ', 'ī', 'ĭ', 'į', 'ı', 'ǐ', 'ȉ', 'ȋ' -> "i"
    'ò', 'ó', 'ô', 'õ', 'ö', 'ø', 'ō', 'ŏ', 'ő', 'ǒ', 'ȍ', 'ȏ' -> "o"
    'ù', 'ú', 'û', 'ü', 'ũ', 'ū', 'ŭ', 'ů', 'ű', 'ų', 'ǔ', 'ȕ', 'ȗ' -> "u"
    'ý', 'ÿ', 'ŷ' -> "y"
    'ç', 'ć', 'ĉ', 'ċ', 'č' -> "c"
    'ñ', 'ń', 'ņ', 'ň' -> "n"
    'š', 'ś', 'ŝ', 'ş', 'ș' -> "s"
    'ž', 'ź', 'ż' -> "z"
    'ĝ', 'ğ', 'ġ', 'ģ' -> "g"
    'ĥ', 'ħ' -> "h"
    'ĵ' -> "j"
    'ķ' -> "k"
    'ľ', 'ĺ', 'ļ', 'ł' -> "l"
    'ď', 'đ', 'ð' -> "d"
    'ř', 'ŗ', 'ŕ' -> "r"
    'ţ', 'ť', 'ŧ', 'ț' -> "t"
    'ß' -> "ss"
    'æ' -> "ae"
    'œ' -> "oe"
    'þ' -> "th"
    else -> c.toString()
}

/**
 * Normalized-contains match of [query] against a reciter's nameEn/slug/
 * country. False when the normalized query is blank (so blank input never
 * matches everything). Never throws.
 */
fun reciterMatchesNormalizedQuery(reciter: Reciter, query: String): Boolean {
    return try {
        val nq = normalizeReciterSearchText(query)
        if (nq.isBlank()) return false
        normalizeReciterSearchText(reciter.nameEn).contains(nq) ||
            normalizeReciterSearchText(reciter.slug).contains(nq) ||
            normalizeReciterSearchText(reciter.country).contains(nq)
    } catch (_: Exception) {
        false
    }
}

/**
 * Resolves a history slug to a known reciter, or null when the slug is stale.
 * [QuranDataRepository.getReciterBySlug] falls back to Mishary for unknown
 * slugs, so only accept the resolution when the slug actually matches.
 */
private fun resolveKnownReciter(slug: String): Reciter? {
    val reciter = runCatching { QuranDataRepository.getReciterBySlug(slug) }.getOrNull()
        ?: return null
    return reciter.takeIf { it.slug.equals(slug, ignoreCase = true) }
}

/**
 * Autofocused engraved search: carved recess ([NoirInsetField]), white cursor,
 * 24% hint ([GhaisNoir.TextDisabled]), ghost clear disc.
 */
@Composable
private fun ReciterSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    NoirInsetField(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search reciters...",
                        color = GhaisNoir.TextDisabled,
                        fontSize = 13.5.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape)
                        .noirClickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Recent-search row in the NoirListRow language: natural-colour avatar well,
 * dual text, trailing play disc for direct play, per-item remove (X well).
 * Row tap opens the reciter profile.
 */
@Composable
private fun RecentReciterRow(
    reciter: Reciter,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoirReciterAvatar(
            photoUrl = photoForSlug(reciter.slug),
            nameEn = reciter.nameEn,
            size = 48.dp,
            shape = CircleShape,
            monogramSize = 18.sp,
            slug = reciter.slug,
            grayscale = false,
            scrimAlpha = 0f
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                .background(GhaisNoir.Fill2, CircleShape)
                .noirClickable(onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play reciter ${reciter.nameEn}",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                .background(GhaisNoir.Fill2, CircleShape)
                .noirClickable(onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove ${reciter.nameEn}",
                tint = GhaisNoir.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Live-result row in the NoirListRow language: natural-colour avatar well,
 * dual text, ghost circular play affordance. Row tap opens the reciter profile.
 */
@Composable
private fun ResultReciterRow(
    reciter: Reciter,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    showForYouBadge: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoirReciterAvatar(
            photoUrl = photoForSlug(reciter.slug),
            nameEn = reciter.nameEn,
            size = 48.dp,
            shape = CircleShape,
            monogramSize = 18.sp,
            slug = reciter.slug,
            grayscale = false,
            scrimAlpha = 0f
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // AGENT P6 — taste badge: only on the top-3 affine hits (see
            // forYouSlugs above); ghost pill, zero hue, matches the Noir
            // library badge language.
            if (showForYouBadge) {
                Spacer(Modifier.height(6.dp))
                ForYouBadge()
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                .background(GhaisNoir.Fill2, CircleShape)
                .noirClickable(onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play reciter ${reciter.nameEn}",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** AGENT P6 — small ghost "FOR YOU" pill marking top taste-ranked hits. */
@Composable
private fun ForYouBadge() {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "FOR YOU",
            color = GhaisNoir.TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
    }
}
