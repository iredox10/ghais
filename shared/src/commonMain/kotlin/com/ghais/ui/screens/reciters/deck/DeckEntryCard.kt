package com.ghais.ui.screens.reciters.deck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ghais.domain.model.Reciter
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

/**
 * Entry card for the "Today's voices" swipe deck: stacked-covers visual
 * (Spotify Daily Mix language) + live count + affinity why-line.
 *
 * - [preview]: first voices of today's deck (same order), fanned 3-deep.
 * - [totalCount]: deck size for the kicker ("10 fresh").
 * - [affinityLabel]: e.g. a followed voice name → "Because you follow X";
 *   null (cold) keeps the generic subtitle.
 * - [doneToday]: completion re-arms the copy ("fresh tomorrow") instead of
 *   hiding the card — the deck is a daily appointment.
 */
@Composable
fun DeckEntryCard(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    preview: List<Reciter> = emptyList(),
    totalCount: Int = MaxDeckCards,
    affinityLabel: String? = null,
    doneToday: Boolean = false,
) {
    NoirHeroCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Text(
            text = if (doneToday) "TODAY'S VOICES • DONE"
            else "TODAY'S VOICES • $totalCount FRESH",
            color = GhaisNoir.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StackedVoicesVisual(preview = preview)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (doneToday) "Done — fresh tomorrow" else "Swipe the stack",
                    style = GhaisTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = GhaisNoir.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        doneToday -> "New voices arrive in the morning"
                        affinityLabel != null -> "Because you follow $affinityLabel"
                        else -> "$totalCount new voices in a minute"
                    },
                    style = GhaisTypography.bodyMedium,
                    color = GhaisNoir.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = GhaisNoir.TextSecondary
            )
        }
    }
}

/** Three fanned grayscale avatars (Spotify cover-stack language). */
@Composable
private fun StackedVoicesVisual(preview: List<Reciter>) {
    Box(modifier = Modifier.width(104.dp).height(56.dp)) {
        val shown = preview.take(3)
        if (shown.isEmpty()) {
            NoirReciterAvatar(
                photoUrl = null,
                nameEn = "?",
                size = 56.dp,
                shape = CircleShape,
                monogramSize = 22.sp,
                ring = false,
                slug = null,
                grayscale = true,
                scrimAlpha = 0f,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            return
        }
        shown.forEachIndexed { index, reciter ->
            NoirReciterAvatar(
                photoUrl = photoForSlug(reciter.slug),
                nameEn = reciter.nameEn,
                size = 56.dp,
                shape = CircleShape,
                monogramSize = 22.sp,
                ring = false,
                slug = reciter.slug,
                grayscale = true,
                scrimAlpha = 0f,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (index * 24).dp)
                    .zIndex((shown.size - index).toFloat())
            )
        }
    }
}
