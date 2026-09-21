package com.ghais.ui.components.noir

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 2 — Noir wells & insets: clay icon-wells, chrome FAB, engraved fields.
 */

/**
 * Clay icon-well: circular embossed container (claymorphic iconography).
 * Top-lit dome + shaded base + 1px rim. Default 40dp; 44/52dp for heroes.
 */
@Composable
fun IconWell(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    contentDescription: String? = null,
    tint: Color = GhaisNoir.TextPrimary
) {
    Box(
        modifier = modifier
            .size(size)
            .background(GhaisNoir.wellFill(), GhaisShapes.well)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Chrome circular FAB (dock center) — raised chrome disc with dark glyph. */
@Composable
fun ChromeFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    contentDescription: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "fabPress"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(GhaisShapes.well)
            .background(GhaisNoir.chromeFill())
            .border(1.dp, Color.White.copy(alpha = 0.4f), GhaisShapes.well)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides GhaisNoir.OnChrome) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = GhaisNoir.OnChrome,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Engraved search / input field — carved-in recessed surface. */
@Composable
fun NoirInsetField(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(GhaisNoir.insetFill(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        content = content
    )
}
