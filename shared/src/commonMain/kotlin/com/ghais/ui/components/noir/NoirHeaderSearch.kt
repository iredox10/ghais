package com.ghais.ui.components.noir

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.math.roundToInt

/**
 * NoirHeaderSearch — a header search affordance that morphs in place: a 40dp
 * circular well, pixel-matched to [NoirNavButton], that expands into the
 * inset-fill pill search field (the [SurahSearchField] style used by
 * ReciterProfileScreen), dressed for a 56dp header row.
 *
 * WHY the state machine is caller-owned: both planned call sites must
 * choreograph sibling content from the very same boolean (Site A lets a
 * weighted "Reciters" title get squeezed out while the field claims the row;
 * Site B keeps a back well to the left of the growing field). An internal
 * `remember { mutableStateOf(false) }` would force every caller into a
 * callback dance to mirror that state back out. So the caller passes
 * [expanded] / [onExpandedChange] and owns the boolean; this component owns
 * everything else — the morph animation, focus, keyboard and accessibility
 * — and reacts to [expanded] changes no matter what flipped them (the well
 * tap, the trailing close well, or caller logic).
 *
 * States and transitions:
 *  - COLLAPSED (`expanded = false`): 40dp circular well — wellFill gradient,
 *    BorderCard rim, 19dp Search glyph in TextPrimary. Tapping opens expansion.
 *  - EXPANDED (`expanded = true`): inset-fill pill filling the remaining row
 *    width — Search glyph in TextTertiary, 14sp singleLine [query] field in
 *    TextPrimary, TextTertiary [placeholder] when empty, a 28dp Fill2 clear
 *    well when the query is non-empty, and a trailing close well that
 *    collapses back to the icon state. Auto-focused with the keyboard shown;
 *    closing hides the keyboard.
 *  - MORPH (between them): one 280ms FastOutSlowIn progress float drives
 *    width (40dp → remaining row width), height (40dp → 44dp) and the
 *    staggered well↔field crossfade, so the parent Row's occupied space
 *    grows and shrinks gracefully instead of popping.
 *
 * Placement contract: put it as the LAST child of a bounded-width Row
 * (56dp header per [NoirNavBar]). While expanding it is measured BEFORE
 * weighted siblings and claims the full remaining width first, so a weighted
 * title is smoothly squeezed to zero and the field visually grows leftward
 * into the header — the well rides the right edge while the pill reveals
 * beneath it. Unbounded-width parents (no finite remaining space to claim)
 * fall back to a 280dp expansion instead of a crash or a stuck 40dp.
 *
 * Accessibility: the collapsed well announces "Search", the clear well
 * "Clear search" and the close well "Close search"; whichever surface is
 * invisible mid-morph is stripped from the semantics tree so screen readers
 * only ever reach the visible state.
 *
 * @param expanded     caller-owned expansion state; true = pill, false = well.
 * @param onExpandedChange  request to flip the state (open from the well tap,
 *   close from the trailing close well).
 * @param query        current query text; caller-owned, shared with filtering.
 * @param onQueryChange    called on every keystroke and by the clear well.
 * @param placeholder  TextTertiary hint shown while the query is empty.
 * @param onSearch     optional IME "Search" action hook (current [query]).
 */
@Composable
fun NoirHeaderSearch(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
    onSearch: ((String) -> Unit)? = null,
) {
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "noirHeaderSearchMorph"
    )

    // Staggered handover instead of a symmetric crossfade: the outgoing well
    // leaves over the first 60% of the morph, the incoming field arrives over
    // the last 60% — they overlap only briefly and never both read at half.
    val wellAlpha = (1f - progress / 0.6f).coerceIn(0f, 1f)
    val fieldAlpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)

    // Focus and keyboard follow the STATE, not the tap gesture: whatever flips
    // [expanded] (well tap, close well, caller), focus and keyboard track it.
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(expanded) {
        if (expanded) {
            withFrameNanos { } // let the just-re-enabled field settle, then focus
            focusRequester.requestFocus()
        } else {
            keyboard?.hide()
        }
    }

    val density = LocalDensity.current
    val collapsedWidthPx = with(density) { 40.dp.toPx() }
    val collapsedHeightPx = with(density) { 40.dp.toPx() }
    // 12dp vertical padding + 20dp glyph row — SurahSearchField's exact height.
    val expandedHeightPx = with(density) { 44.dp.toPx() }
    val fallbackWidthPx = with(density) { 280.dp.toPx() }

    // Single morph surface: one progress float interpolates the node's own
    // size between the well's 40dp circle and the field's remaining-width
    // pill, so content inside never has to re-layout against a jumping parent.
    val sizedByProgress = Modifier.layout { measurable, constraints ->
        val targetWidth = when {
            constraints.hasBoundedWidth ->
                constraints.maxWidth.toFloat().coerceAtLeast(collapsedWidthPx)
            else -> fallbackWidthPx // unbounded parent: claim a fixed span instead
        }
        val widthPx = collapsedWidthPx + progress * (targetWidth - collapsedWidthPx)
        val heightPx = collapsedHeightPx + progress * (expandedHeightPx - collapsedHeightPx)
        val width = widthPx.roundToInt()
            .coerceAtMost(if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE)
        val height = heightPx.roundToInt()
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(width, height) { placeable.placeRelative(0, 0) }
    }

    Box(
        modifier = modifier.then(sizedByProgress),
        contentAlignment = Alignment.CenterEnd // the well rides the right edge while the pill grows leftward
    ) {
        // EXPANDED surface — always composed so the FocusRequester stays
        // attached through mid-morph focus requests; invisible-but-dead while
        // collapsed (disabled field, stripped semantics at zero opacity).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .alpha(fieldAlpha)
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.insetFill())
                .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .then(if (fieldAlpha < 0.05f) Modifier.clearAndSetSemantics { } else Modifier)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                enabled = expanded, // dead while fading out on collapse; drops focus with it
                textStyle = TextStyle(
                    color = GhaisNoir.TextPrimary,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(query) }),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = GhaisNoir.TextTertiary,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    inner()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .noirClickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .noirClickable { onExpandedChange(false) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close search",
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // COLLAPSED well, drawn on top so taps resolve to it whenever it is
        // visible; token-for-token the NoirNavButton recipe.
        Box(
            modifier = Modifier
                .alpha(wellAlpha)
                .size(40.dp)
                .background(GhaisNoir.wellFill(), CircleShape)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .then(
                    if (!expanded) Modifier.noirClickable { onExpandedChange(true) }
                    else Modifier
                )
                .then(if (wellAlpha < 0.05f) Modifier.clearAndSetSemantics { } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
