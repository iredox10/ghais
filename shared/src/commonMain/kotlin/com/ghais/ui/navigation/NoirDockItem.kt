package com.ghais.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ghais.ui.theme.GhaisNoir

/**
 * Noir dock tab item: a plain circular button, no label.
 *
 * Selected: light-chrome filled circle + near-black glyph.
 * Unselected: transparent circle + quiet light glyph.
 * Noir tokens only; press scale + tint crossfade retained.
 */
@Composable
fun DockTabItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 550f),
        label = "dockItemPress"
    )
    val fill by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.92f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "dockItemFill"
    )
    val glyphTint by animateColorAsState(
        targetValue = if (selected) GhaisNoir.OnChrome else GhaisNoir.TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "dockItemTint"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = glyphTint,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
