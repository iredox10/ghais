package com.ghais.ui.components.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 2 — Noir surfaces.
 *
 * Depth recipe (reference-faithful): gradient fill + 1px ghost border +
 * bright TOP-ONLY specular hairline. No flat greys, no colored shadows.
 */

/** Bright top specular hairline — the signature glass read. Inset avoids corner arcs. */
fun Modifier.topSpecular(
    inset: Dp = 26.dp,
    color: Color = GhaisNoir.SpecularTop
): Modifier = drawBehind {
    val ix = inset.toPx()
    if (size.width > ix * 2f) {
        drawLine(
            color = color,
            start = Offset(ix, 1f),
            end = Offset(size.width - ix, 1f),
            strokeWidth = 1f
        )
    }
}

/** Press physics: tactile tap with no Material ripple — noir stays pure. */
fun Modifier.noirClickable(onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, onClick = onClick)

/** Elevated glass card (28dp noir radius). Active = stronger wash for selection. */
@Composable
fun NoirCard(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    soft: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val fill = when {
        active -> GhaisNoir.cardFillActive()
        soft -> GhaisNoir.cardFillSoft()
        else -> GhaisNoir.cardFill()
    }
    var cardModifier = modifier
        .background(fill, GhaisShapes.cardNoir)
        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
        .topSpecular()
    if (onClick != null) cardModifier = cardModifier.noirClickable(onClick)
    Box(
        modifier = cardModifier.padding(all = 20.dp),
        content = content
    )
}

/** Large hero card (32dp) for bento feature tiles. */
@Composable
fun NoirHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var cardModifier = modifier
        .background(GhaisNoir.cardFill(), GhaisShapes.cardLarge)
        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardLarge)
        .topSpecular(inset = 30.dp)
    if (onClick != null) cardModifier = cardModifier.noirClickable(onClick)
    Box(
        modifier = cardModifier.padding(all = 20.dp),
        content = content
    )
}
