package com.ghais.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir

private val DockItemWell = RoundedCornerShape(14.dp)

/**
 * Noir dock tab item: icon in a rounded-square well above a semibold label.
 *
 * Selected: white/chrome wash well fill + near-black glyph, primary label.
 * Unselected: transparent well + tertiary glyph, tertiary/ghost label.
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
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 550f),
        label = "dockItemPress"
    )
    val washAlpha by animateFloatAsState(
        targetValue = if (selected) 0.92f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dockItemWash"
    )
    val glyphTint by animateColorAsState(
        targetValue = if (selected) GhaisNoir.OnChrome else GhaisNoir.TextTertiary,
        animationSpec = tween(durationMillis = 200),
        label = "dockItemTint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
        animationSpec = tween(durationMillis = 200),
        label = "dockItemLabel"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(44.dp, 40.dp)
                .clip(DockItemWell)
                .background(Color.White.copy(alpha = washAlpha))
                .then(
                    if (selected) Modifier.border(
                        0.5.dp,
                        Color.White.copy(alpha = 0.2f),
                        DockItemWell
                    )
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = glyphTint,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.title,
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = (-0.2).sp
        )
    }
}
