package com.ghais.ui.screens.devotional

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.DevotionalRepository
import com.ghais.domain.model.DevotionKind
import com.ghais.domain.model.DevotionalItem
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/** Sentinel for the "no category filter" chip. */
private const val ALL_CATEGORIES: String = "All"

/**
 * Upper bound for [DevotionalRepository.search]. The catalogues are 97 + 20
 * items, so a limit this high means the search never silently truncates; the
 * parameter is still passed explicitly so the repository owns the contract.
 */
private const val SEARCH_LIMIT: Int = 500

/** Arabic is shown as a one-line teaser only; the reader owns the full text. */
private const val ARABIC_SNIPPET_MAX: Int = 88

/** Plural unit for the header subtitle. */
private val DevotionKind.unitWord: String
    get() = when (this) {
        DevotionKind.DUA -> "supplications"
        DevotionKind.RUQIYAH -> "verses"
    }

/**
 * Devotional catalogue list — Duas and Ruqiyah in one screen.
 *
 * The two collections are structurally identical (title + citation + optional
 * audio), so they share one parameterised screen: Home pushes
 * `DevotionListScreen(DevotionKind.DUA)` and
 * `DevotionListScreen(DevotionKind.RUQIYAH)` and everything below — chrome,
 * filters, rows, empty states — reads off [kind].
 *
 * Visual language is copied, not invented: [NoirScreenRoot] canvas and
 * 20.dp/12.dp screen inset, the 40.dp/20.dp back [IconWell], 20.sp Bold title
 * with a 12.sp tertiary subtitle and a ghost count chip (HistoryScreen /
 * BroadcastsScreen), the engraved [NoirInsetField] search and the
 * chrome-fill / Fill2-resting pill row (AllCuratedPlaylistsScreen), the
 * ghost-well-in-soft-card empty state (BroadcastsScreen) and the
 * `HistoryNoirRow` card language: `cardFillSoft` + 1.dp [GhaisNoir.BorderCard]
 * + 22.dp top-only specular + [GhaisShapes.row] + 12.dp padding + a
 * [NoirArtworkWell] monogram plate.
 *
 * Filtering is two independent axes: a single-select category chip row driven
 * by [DevotionalRepository.byKindAndCategory], and a query driven by
 * [DevotionalRepository.search]. When both are active the category is applied
 * as an intersection over the search hits so the two controls compose instead
 * of the category silently winning.
 *
 * Audio: the play affordance is driven by the RESOLVED url from
 * [DevotionalRepository.audioUrlFor], not by [DevotionalItem.hasAudio] alone —
 * ruqiyah ships with a null `audioUrl` and gets an everyayah url derived at
 * read time, so keying off `hasAudio` would wrongly mute all 20 ruqiyah rows.
 * The inverse also holds: a dua resolves to null and gets no play control at
 * all. That resolved value is a presence gate only; nothing here plays it (see
 * the note on the row). See [DevotionNoirRow].
 */
data class DevotionListScreen(val kind: DevotionKind) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var query by remember { mutableStateOf("") }
        // null = the "All" chip, i.e. no category narrowing.
        var selectedCategory by remember(kind) { mutableStateOf<String?>(null) }

        // Total of the WHOLE catalogue, so typing and filtering never shrink
        // the header (same rule as the history list).
        val all = remember(kind) { DevotionalRepository.all(kind) }
        val categories = remember(kind) { DevotionalRepository.categories(kind) }

        val visible = remember(all, kind, query, selectedCategory) {
            if (query.isBlank()) {
                selectedCategory?.let { DevotionalRepository.byKindAndCategory(kind, it) } ?: all
            } else {
                val hits = DevotionalRepository.search(kind, query, SEARCH_LIMIT)
                selectedCategory?.let { cat -> hits.filter { it.category == cat } } ?: hits
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
                // ----------------------------------------------------------
                // Top bar — IconWell back + title/subtitle + count chip.
                // ----------------------------------------------------------
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = kind.label,
                            color = GhaisNoir.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${all.size} ${kind.unitWord}",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${all.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ----------------------------------------------------------
                // Search — engraved inset, white cursor, 24% hint, ghost clear.
                // ----------------------------------------------------------
                NoirInsetField(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search ${kind.label.lowercase()}...",
                                    color = GhaisNoir.TextDisabled,
                                    fontSize = 13.5.sp
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (query.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                    .background(GhaisNoir.Fill2, CircleShape)
                                    .noirClickable { query = "" },
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

                // ----------------------------------------------------------
                // Category chips — rendered only when there is something to
                // choose between. Single-select, "All" clears the filter.
                // ----------------------------------------------------------
                if (categories.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryPillsRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (visible.isEmpty()) {
                    EmptyState(
                        kind = kind,
                        query = query,
                        selectedCategory = selectedCategory,
                        catalogueIsEmpty = all.isEmpty()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Key is `id#index`, not `id`: the seed reuses short ids
                        // across categories (dua-1 exists under Daily Dua,
                        // Morning, Evening and After Prayer), and a duplicate
                        // LazyColumn key throws at runtime. The index suffix
                        // only disambiguates; ordering stays catalogue order.
                        itemsIndexed(
                            items = visible,
                            key = { index, item -> "${item.id}#$index" }
                        ) { _, item ->
                            DevotionNoirRow(
                                kind = kind,
                                item = item,
                                onOpen = { navigator.push(DevotionPlayerScreen(itemId = item.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter pills: selected = chrome gradient fill + near-black label
 * (primary action); resting = Fill2 wash + card hairline + 62% label.
 * Shape and metrics copied from the curated-playlist filter row
 * (horizontal 16.dp, vertical 8.dp).
 */
@Composable
private fun CategoryPillsRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(listOf(ALL_CATEGORIES) + categories) { chip ->
            // A category that was somehow blank would render as an invisible
            // chip; fold it into "All" instead of drawing one.
            val label = chip.ifBlank { ALL_CATEGORIES }
            val isSelected = if (label == ALL_CATEGORIES) {
                selectedCategory == null
            } else {
                selectedCategory == label
            }
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    // Both arms must be a Brush: `Modifier.background` has a
                    // Color overload and a Brush overload, so an `if` mixing
                    // the two infers `Any` and neither overload applies. The
                    // resting arm is a flat Fill2 wash written as a
                    // single-stop gradient (TafseerSheet's idiom) so the pill
                    // keeps its Fill2 look.
                    .background(
                        if (isSelected) {
                            GhaisNoir.chromeFill()
                        } else {
                            Brush.verticalGradient(listOf(GhaisNoir.Fill2, GhaisNoir.Fill2))
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.White.copy(alpha = 0.35f) else GhaisNoir.BorderCard,
                        GhaisShapes.pill
                    )
                    .noirClickable {
                        onCategorySelected(if (label == ALL_CATEGORIES) null else label)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) GhaisNoir.OnChrome else GhaisNoir.TextSecondary
                )
            }
        }
    }
}

/**
 * Ghost-well empty state in a soft card. Three distinct cases, because "you
 * filtered everything away" and "this catalogue is empty" are different
 * problems with different fixes:
 * - query present            -> no matches for what you typed
 * - category filter only     -> that category came back empty
 * - neither, catalogue empty -> nothing shipped yet
 */
@Composable
private fun EmptyState(
    kind: DevotionKind,
    query: String,
    selectedCategory: String?,
    catalogueIsEmpty: Boolean
) {
    val searching = query.isNotBlank()
    val headline: String
    val hint: String
    val icon = when {
        searching -> {
            headline = "No matches"
            hint = "Nothing in ${kind.label} matches \"$query\". Try a different word or clear the search."
            Icons.Filled.Search
        }
        selectedCategory != null -> {
            headline = "Nothing in $selectedCategory"
            hint = "This category has no ${kind.unitWord} in it yet."
            Icons.AutoMirrored.Filled.MenuBook
        }
        catalogueIsEmpty -> {
            headline = "No ${kind.label.lowercase()} yet"
            hint = "This collection will appear here once it is added."
            Icons.AutoMirrored.Filled.MenuBook
        }
        else -> {
            headline = "Nothing to show"
            hint = "No ${kind.unitWord} in this view."
            Icons.AutoMirrored.Filled.MenuBook
        }
    }

    NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconWell(
                icon = icon,
                size = 56.dp,
                iconSize = 26.dp,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = headline,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * One devotional entry in the history-row language.
 *
 * ONE tap target: the whole row opens `DevotionPlayerScreen(itemId)`, which
 * resolves the entry, queues its own first track and starts it. There is
 * deliberately no separate "play" disc target — a second push with a "start
 * playing" intent would be redundant against a screen that already autoplays,
 * and calling play here as well would double-start the same id. The trailing
 * disc is therefore a STATUS GLYPH (has audio / read-only), paired with the
 * "Recite" / "Read-only" line above it, and the two branches are the visual
 * contract: a lit play triangle appears only where audio actually resolves.
 * Keeping that gate is what stops a lit-up play control from existing on an
 * item that cannot play.
 */
@Composable
private fun DevotionNoirRow(
    kind: DevotionKind,
    item: DevotionalItem,
    onOpen: () -> Unit
) {
    // Presence gate ONLY — the resolved string is never played from here. A
    // multi-ayah ruqiyah entry resolves to per-ayah urls joined with
    // `DevotionalRepository.MULTI_AUDIO_SEPARATOR`, and `PlayerBridge.play`
    // takes one url, so a joined string is not playable: this value is asked
    // "does ANY url resolve?" and nothing more. Playback always goes through
    // `DevotionalRepository.playableTracks`, which expands a multi-ayah entry
    // into one real track per ayah — that is what `DevotionPlayerScreen`
    // queues. Resolved url, not `hasAudio`: ruqiyah ships `audioUrl = null`
    // and gets an everyayah url built on read, so `hasAudio` alone would
    // disable all 20 ruqiyah rows. A blank/null resolution is the only real
    // "cannot play".
    val audioResolves = remember(item) {
        DevotionalRepository.audioUrlFor(item)?.takeIf { it.isNotBlank() } != null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onOpen)
            .padding(12.dp)
    ) {
        NoirArtworkWell(
            coverUrl = "",
            monogram = item.monogramOr(kind),
            shape = CircleShape,
            size = 44.dp,
            monogramSize = 16.sp,
            ring = true,
            grayscale = false,
            scrimAlpha = 0f
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { kind.label },
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Blank-safe subtitle: the pieces are joined only when present, so
            // a missing reference can never leave a dangling "• ".
            val subtitle = item.metaSubtitle()
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val arabic = remember(item) { arabicSnippet(item.arabic) }
            if (arabic.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                // Arabic must never ride a Latin display style: the ambient
                // MaterialTheme text style carries negative tracking, and
                // non-zero tracking breaks cursive joins. arabicSmall +
                // explicit letterSpacing = 0 is the fix, and the block is
                // wrapped in RTL so the teaser reads right-to-left.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabic,
                        style = GhaisTypography.arabicSmall,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp,
                        color = GhaisNoir.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            // The state is said in words, not only implied by a dimmed glyph.
            Text(
                text = if (audioResolves) item.reciterName.ifBlank { "Recite" } else "Read-only",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (audioResolves) {
                // Status glyph, NOT a second target: there is no `noirClickable`
                // here on purpose. `DevotionPlayerScreen` already queues this
                // entry's own first track and starts it, so a disc that pushed
                // the same screen with a "start playing" intent would either be
                // a no-op or — if it also called play — a double start of the
                // same id. The row is the one and only tap target; this disc
                // only says "this entry has audio". Meaning is carried by the
                // "Recite" / "Read-only" line above it, so the glyph itself is
                // decorative to accessibility, like the read-only one.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // No audio: a dimmed play triangle would read as "broken
                // play" — the entry cannot be recited at all, so the glyph
                // states what the row IS instead (reading), and the row stays
                // fully tappable. Both branches are inert markers; the tap
                // target is the row in either case.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = GhaisNoir.TextDisabled,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Latin-initial monogram for the leading plate. Arabic-initial or symbol-initial
 * titles are skipped in favour of the kind's letter, so the plate is never a
 * glyph the Latin face has no coverage for.
 */
private fun DevotionalItem.monogramOr(kind: DevotionKind): String =
    title.trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.takeIf { it in 'A'..'Z' }
        ?.toString()
        ?: kind.label.take(1).uppercase()

/**
 * Citation line: "Category • Reference" with only the present parts joined, so
 * neither a blank category nor a blank reference (both occur in the seed) can
 * produce an empty string or a dangling separator. Falls back to the opening
 * of the translation only when BOTH are missing, so a row is never
 * title-only-and-ambiguous.
 */
private fun DevotionalItem.metaSubtitle(): String {
    val parts = listOfNotNull(
        category.trim().takeIf { it.isNotEmpty() },
        reference.trim().takeIf { it.isNotEmpty() }
    )
    if (parts.isNotEmpty()) return parts.joinToString(" • ")
    return translation.trim().takeIf { it.isNotEmpty() }
        ?.let { if (it.length > 72) it.take(72).trimEnd() + "…" else it }
        .orEmpty()
}

/** Whitespace-collapsed one-line Arabic teaser; the reader owns the full text. */
private fun arabicSnippet(raw: String): String {
    val flat = raw.replace(Regex("\\s+"), " ").trim()
    if (flat.isEmpty()) return ""
    if (flat.length <= ARABIC_SNIPPET_MAX) return flat
    val cut = flat.take(ARABIC_SNIPPET_MAX)
    val lastSpace = cut.lastIndexOf(' ')
    val head = if (lastSpace > ARABIC_SNIPPET_MAX / 2) cut.take(lastSpace) else cut
    return "$head…"
}
