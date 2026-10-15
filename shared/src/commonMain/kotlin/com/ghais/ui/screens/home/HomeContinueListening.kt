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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.prettifySlug
import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.Surah
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.util.bidiIsolate

/**
 * Phase 4 — "Continue listening".
 *
 * Bento split: the most recent entry becomes a hero plate (artwork + progress
 * meter + chromium Resume), the remainder becomes a rail of compact plates.
 * The live row is signalled by fill elevation, a brighter specular border and a
 * chromium disc — never by the old emerald accent or animated colour borders.
 *
 * Both plates carry the full surah identity (English name, Arabic name, ordinal,
 * ayah count, revelation type) resolved from the entry's `surahId`, matching the
 * surah line used by the reciter and memorization screens. A plate whose id is
 * not in the catalog shows the title the history record itself stored and drops
 * the unresolvable fragments rather than drawing a blank or invented one.
 */
@Composable
fun HomeContinueListeningRow(onPlay: (JumpBackInItem) -> Unit) {
    val history by UserUsageRepository.history.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isEnginePlaying by AudioEngine.isPlaying.collectAsState()

    // Latest-first at read: hero = single newest, rail follows newest-first.
    // sortedByDescending is stable, so timestamp ties keep store (MRU-first) order.
    val recent = remember(history) { history.sortedByDescending { it.lastPlayedTimestampMs }.take(7) }

    if (recent.isEmpty()) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nothing queued yet",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Play any surah and your place will be kept here.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    // Stacked deck (lawyer-ref): the selected entry rides front with full
    // detail + lavender Resume; ghost layers peek from behind for depth.
    // Prev/next chevrons page the stack; play resumes the front entry.
    val deck = recent.take(5)
    var selectedIndex by remember { mutableStateOf(0) }
    val selected = deck[selectedIndex.coerceIn(deck.indices)]
    val selectedActive = isLive(selected, currentTrack?.surahId, currentTrack?.reciterSlug)
    val selectedPhoto = rememberCloudPhoto(selected.reciterSlug)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        // Ghost back layers — pure depth, non-interactive.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .height(48.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp)
                .background(GhaisNoir.cardFillSoft(), GhaisShapes.cardNoir)
                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.cardNoir)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(48.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-13).dp)
                .background(GhaisNoir.cardFillSoft(), GhaisShapes.cardNoir)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
        )
        // Front card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GhaisNoir.cardFill(), GhaisShapes.cardNoir)
                .border(
                    1.dp,
                    if (selectedActive) GhaisNoir.Lavender.copy(alpha = 0.5f) else GhaisNoir.BorderCard,
                    GhaisShapes.cardNoir
                )
                .topSpecular(inset = 30.dp)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPhoto != null) {
                    NoirArtworkWell(
                        coverUrl = selectedPhoto,
                        monogram = reciterMonogramOf(selected),
                        shape = CircleShape,
                        size = 76.dp,
                        monogramSize = 26.sp,
                        ring = true,
                        grayscale = false,
                        scrimAlpha = 0f,
                        errorFallbackUrl = selected.coverUrl
                    )
                } else {
                    NoirArtworkWell(
                        coverUrl = selected.coverUrl,
                        monogram = monogramOf(selected),
                        shape = RoundedCornerShape(20.dp),
                        size = 76.dp,
                        monogramSize = 26.sp,
                        ring = selectedActive
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CONTINUE",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = surahNameOf(selected),
                            color = GhaisNoir.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val arabicName = arabicNameOf(selected)
                        if (arabicName.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = bidiIsolate(arabicName),
                                color = GhaisNoir.TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                // Bound the Arabic so it can never starve the
                                // English name out of the row.
                                modifier = Modifier.widthIn(max = 96.dp)
                            )
                        }
                    }
                    val selectedMeta = surahMetaOf(selected)
                    if (selectedMeta.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = selectedMeta,
                            color = GhaisNoir.TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = selected.subtitle,
                        color = if (selectedActive) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selectedActive) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            NoirSegmentedProgress(progress = progressOf(selected), trackHeight = 8.dp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progressOf(selected) * 100).toInt()}% through",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
                val remaining = selected.durationMs - selected.positionMs
                if (selected.durationMs > 0L && remaining > 0L) {
                    Text(
                        text = "${formatClock(remaining)} left",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.lavenderFill())
                    .noirClickable { onPlay(selected) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (selectedActive && isEnginePlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = null,
                    tint = GhaisNoir.LavenderInk,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (selectedActive && isEnginePlaying) "Now playing" else "Resume",
                    color = GhaisNoir.LavenderInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (deck.size > 1) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DeckPagerButton(
                        contentDescription = "Previous in stack",
                        onClick = {
                            selectedIndex = (selectedIndex - 1 + deck.size) % deck.size
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = null,
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "${selectedIndex + 1} / ${deck.size}",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    DeckPagerButton(
                        contentDescription = "Next in stack",
                        onClick = {
                            selectedIndex = (selectedIndex + 1) % deck.size
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 48dp pager well — touch-compliant deck navigation. */
@Composable
private fun DeckPagerButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(GhaisNoir.wellFill(), GhaisShapes.well)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
            .noirClickable(onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

private fun progressOf(item: JumpBackInItem): Float = item.progress.coerceIn(0f, 1f)

// ---------------------------------------------------------------------------
// Surah / reciter identity
//
// [JumpBackInItem] (data/seed/GhaisAssets.kt) stores no Arabic name and no
// ayah count — only a `surahId`. That is enough: the id indexes the full
// 114-surah catalog, so the whole house surah line (`id. nameEn` /
// `nameAr • n Ayahs • revelation`) is derivable at render time and was simply
// never drawn. Every helper below therefore resolves through the catalog and
// degrades to the identity the history record itself carries, never to blank.
// ---------------------------------------------------------------------------

/** The catalog surah for a history entry, or null when the id is not a surah. */
private fun surahOf(item: JumpBackInItem): Surah? =
    QuranDataRepository.getSurahById(item.surahId)

/** Arabic surah name; empty when the catalog has no such surah. */
private fun arabicNameOf(item: JumpBackInItem): String =
    surahOf(item)?.nameAr?.trim().orEmpty()

/**
 * English surah name. Prefers the catalog (canonical, always present for a
 * real surah) and otherwise keeps the title the history record stored. Never
 * blank: with neither, it reports the raw id, matching the fallback the
 * history writer itself uses.
 */
private fun surahNameOf(item: JumpBackInItem): String {
    surahOf(item)?.nameEn?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    return item.title.trim().ifEmpty { "Surah ${item.surahId}" }
}

/** "Surah 88 • 26 Ayahs • Meccan"; empty when the catalog has no such surah. */
private fun surahMetaOf(item: JumpBackInItem): String {
    val surah = surahOf(item) ?: return ""
    return listOfNotNull(
        "Surah ${surah.id}",
        surah.ayahsCount.takeIf { it > 0 }?.let { "$it Ayahs" },
        surah.revelationType.trim().takeIf { it.isNotEmpty() }
    ).joinToString(" • ")
}

/**
 * Reciter name for a history entry.
 *
 * `getReciterBySlug` is deliberately lossy — an unknown slug resolves to
 * Mishary Rashid Alafasy — so it must not be used for display identity. The
 * alias-aware, null-on-miss `getBrowseReciterBySlug` reads the same
 * cloud-merged catalog and reports a genuine miss, which then falls back to
 * the entry's own slug rather than borrowing another human's name.
 */
private fun reciterNameOf(item: JumpBackInItem): String =
    QuranDataRepository.getBrowseReciterBySlug(item.reciterSlug)
        ?.nameEn?.trim()?.takeIf { it.isNotEmpty() }
        ?: item.reciterSlug.trim().takeIf { it.isNotEmpty() }?.prettifySlug()
        ?: "Unknown reciter"

/** Surah monogram — the initial of the name the card actually shows. */
private fun monogramOf(item: JumpBackInItem): String =
    surahNameOf(item).firstOrNull { it.isLetter() }?.uppercase() ?: "?"

/**
 * Reciter monogram. "?" (not a borrowed "M" and not a fake "Q") when the
 * reciter genuinely cannot be identified, so the well never asserts an
 * identity the data does not support.
 */
private fun reciterMonogramOf(item: JumpBackInItem): String =
    reciterNameOf(item).firstOrNull { it.isLetter() }?.uppercase() ?: "?"

private fun isLive(item: JumpBackInItem, surahId: Int?, reciterSlug: String?): Boolean =
    surahId != null && reciterSlug != null &&
        item.surahId == surahId && item.reciterSlug == reciterSlug

/** mm:ss read-out for the progress labels. */
private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${if (seconds < 10) "0$seconds" else "$seconds"}"
}
