package com.ghais.ui.screens.reciters.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.FollowStore
import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlinx.coroutines.launch

/**
 * AGENT A4 — bottom-sheet host for the "Today's voices" swipe deck.
 *
 * Swipe language: drag the card right to follow + open the reciter profile,
 * drag left to skip. The Skip / Follow pill buttons below the card are the
 * accessible (non-gesture) equivalent and perform the exact same transitions.
 *
 * - Right (follow): [FollowStore.follow] + [onOpenProfile] (the profile pushes
 *   on top; backing out returns to the next card), then advances the deck.
 * - Left (skip): advances without following.
 * - Completion: "You explored N voices today" + follow summary + Done.
 *
 * Presentation + deck-progress state only. Deck construction is [todayDeck];
 * the 30s preview lifecycle is owned by [DeckCard].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckSheet(
    deck: List<Reciter>,
    photoFor: (String) -> String?,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    taste: TasteProfile? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var index by remember(deck) { mutableIntStateOf(0) }
    val followedNames = remember(deck) { mutableStateListOf<String>() }
    var skippedCount by remember(deck) { mutableIntStateOf(0) }
    val done = index >= deck.size

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
            Spacer(Modifier.height(16.dp))

            if (deck.isEmpty()) {
                DeckEmptyState()
            } else if (done) {
                DeckCompletionState(
                    explored = deck.size,
                    followedNames = followedNames.toList(),
                    onDone = onDismiss
                )
            } else {
                val reciter = deck[index]
                DeckSwipeCard(
                    reciter = reciter,
                    position = index,
                    total = deck.size,
                    photoUrl = photoFor(reciter.slug),
                    taste = taste,
                    onSwipeRight = {
                        FollowStore.follow(reciter.slug)
                        if (reciter.nameEn !in followedNames) followedNames.add(reciter.nameEn)
                        index++
                        onOpenProfile(reciter.slug)
                    },
                    onSwipeLeft = {
                        skippedCount++
                        index++
                    }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GhostPillButton(
                        text = "Skip",
                        onClick = {
                            skippedCount++
                            index++
                        },
                        modifier = Modifier.weight(1f)
                    )
                    // Follow pill: chrome primary action (check icon via content row).
                    Box(modifier = Modifier.weight(1f)) {
                        ChromePillButton(
                            text = "Follow",
                            onClick = {
                                FollowStore.follow(reciter.slug)
                                if (reciter.nameEn !in followedNames) followedNames.add(reciter.nameEn)
                                index++
                                onOpenProfile(reciter.slug)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Drag right to follow · left to skip",
                    color = GhaisNoir.TextDisabled,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Draggable card wrapper: horizontal drag past the threshold (or a fast fling)
 * settles the card off-screen in the fling direction; otherwise it springs
 * back. Tilt + fade read the drag direction while moving (monochrome only).
 */
@Composable
private fun DeckSwipeCard(
    reciter: Reciter,
    position: Int,
    total: Int,
    photoUrl: String?,
    taste: TasteProfile?,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 96.dp.toPx() } }
    val dragState = remember(reciter, position) {
        DeckDragState()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = dragState.offsetX
                rotationZ = dragState.offsetX / 60f
                alpha = 1f - (kotlin.math.abs(dragState.offsetX) / (thresholdPx * 3f)).coerceIn(0f, 0.35f)
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    dragState.offsetX += delta
                },
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val x = dragState.offsetX
                    scope.launch {
                        when {
                            x > thresholdPx || velocity > 1_200f -> onSwipeRight()
                            x < -thresholdPx || velocity < -1_200f -> onSwipeLeft()
                            else -> dragState.offsetX = 0f
                        }
                    }
                }
            )
    ) {
        DeckCard(
            reciter = reciter,
            position = position,
            total = total,
            photoUrl = photoUrl,
            taste = taste
        )
    }
}

/** Plain holder so the drag offset survives recomposition per card. */
private class DeckDragState {
    var offsetX: Float = 0f
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
                .background(GhaisNoir.wellFill()),
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
