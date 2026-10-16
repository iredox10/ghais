package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.util.bidiIsolate
import com.ghais.data.repository.DevotionalRepository
import com.ghais.domain.model.DevotionKind
import kotlin.time.Clock

/**
 * Home redesign — "Noir Lavender Grid".
 *
 * Reference synthesis (user set):
 * - AirPods lavender set: deep plum hero with display headline + pill CTA,
 *   2-col tinted grid, pill filter chips, search + avatar top, price-pill CTA.
 * - Lawyer dark set: near-black canvas, stats row, stacked deck, lavender
 *   selected states, rounded bottom dock.
 * - Discover set: 2-col image grid with scrim overlay + heart action,
 *   full-bleed detail with vertical actions.
 *
 * Roles are strict: black canvas stays; lavender is selected/CTA/hero only.
 */

// ---------------------------------------------------------------------------
// Search — Ref1 middle screen + Ref3 discover bar, Noir engraved recipe.
// ---------------------------------------------------------------------------
@Composable
fun HomeSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GhaisNoir.insetFill(), GhaisShapes.pill)
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Search surahs, qaris, moods…",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------
// Filter chips — Ref1 "All / AirPods / Accessories" pill row.
// Each chip navigates (no dead controls): All shows everything, the rest
// jump to their dedicated surface.
// ---------------------------------------------------------------------------
data class HomeChip(val id: String, val label: String)

val HomeChips = listOf(
    HomeChip("all", "All"),
    HomeChip("reciters", "Qaris"),
    HomeChip("surahs", "Surahs"),
    HomeChip("moods", "Moods"),
)

@Composable
fun HomeFilterChips(
    selectedId: String,
    onSelect: (HomeChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HomeChips, key = { it.id }) { chip ->
            val selected = chip.id == selectedId
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(
                        if (selected) GhaisNoir.lavenderFill() else GhaisNoir.cardFillSoft()
                    )
                    .border(
                        1.dp,
                        if (selected) Color.Transparent else GhaisNoir.BorderCard,
                        GhaisShapes.pill
                    )
                    .noirClickable { onSelect(chip) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chip.label,
                    color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Plum hero — Ref1 left screen: deep gradient card, eyebrow pill,
// display headline, pill CTA. Right side floats the mix artwork.
// ---------------------------------------------------------------------------
@Composable
fun HomePlumHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    coverUrl: String,
    monogram: String,
    ctaLabel: String,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** Lawyer-ref stats row: (value, label) triples in white, e.g. mins/streak/qaris. */
    stats: List<Pair<String, String>> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GhaisNoir.plumFill(), GhaisShapes.cardLarge)
            .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.35f), GhaisShapes.cardLarge)
            .topSpecular(inset = 30.dp)
            .noirClickable(onClick = onOpen)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // Eyebrow pill — Ref1 "New arrivals +".
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = eyebrow,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(12.dp))
            NoirArtworkWell(
                coverUrl = coverUrl,
                monogram = monogram,
                shape = CircleShape,
                size = 96.dp,
                monogramSize = 30.sp,
                ring = true,
                scrimAlpha = 0f
            )
        }
        Spacer(Modifier.height(16.dp))
        // Primary pill — Ref1 "Add to cart" / Ref2 "Book Appointment".
        Row(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.lavenderFill())
                .noirClickable(onClick = onPlay)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = GhaisNoir.LavenderInk,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = ctaLabel,
                color = GhaisNoir.LavenderInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Lawyer-ref stats row: numbers carry the hierarchy.
        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.cardNoir)
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stats.forEachIndexed { index, (value, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = value,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    if (index < stats.lastIndex) {
                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = 32.dp)
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dua of the Day — editorial display card (Arabic calligraphy moment).
// Deterministic daily pick from the duas catalogue; hidden when empty.
// ---------------------------------------------------------------------------
@Composable
fun HomeDuaOfDay(onOpen: (itemId: String) -> Unit, modifier: Modifier = Modifier) {
    val daySeed = remember { Clock.System.now().toEpochMilliseconds() / 86_400_000L }
    val dua = remember(daySeed) {
        try {
            val duas = DevotionalRepository.all(DevotionKind.DUA)
            if (duas.isEmpty()) null else duas[(daySeed % duas.size).toInt()]
        } catch (_: Exception) {
            null
        }
    }
    if (dua == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GhaisNoir.tintFill(), GhaisShapes.cardLarge)
            .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.25f), GhaisShapes.cardLarge)
            .topSpecular(inset = 28.dp)
            .noirClickable { onOpen(dua.id) }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DUA OF THE DAY",
                color = GhaisNoir.Lavender,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            if (dua.reference.trim().isNotEmpty()) {
                Text(
                    text = dua.reference.trim(),
                    color = GhaisNoir.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
        if (dua.arabic.trim().isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = bidiIsolate(dua.arabic.trim()),
                color = GhaisNoir.TextPrimary,
                fontSize = 22.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = dua.title.trim().ifEmpty { "Daily supplication" },
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.lavenderFill())
                .noirClickable { onOpen(dua.id) }
                .padding(horizontal = 20.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = GhaisNoir.LavenderInk,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Read & listen",
                color = GhaisNoir.LavenderInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
