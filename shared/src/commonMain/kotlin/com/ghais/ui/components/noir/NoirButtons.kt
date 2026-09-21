package com.ghais.ui.components.noir

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 2 — Noir buttons: chrome primary pill + ghost secondary pill.
 */

/** Primary CTA: white chrome gradient pill with near-black label + press physics. */
@Composable
fun ChromePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "chromePress"
    )
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .scale(scale)
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.chromeFill())
            .border(1.dp, Color.White.copy(alpha = 0.35f * alpha), GhaisShapes.pill)
            .drawBehind {
                // Inner top highlight — light catching the chrome edge.
                drawLine(
                    GhaisNoir.ChromeInnerHighlight.copy(alpha = 0.55f * alpha),
                    Offset(size.width * 0.18f, 1.5f),
                    Offset(size.width * 0.82f, 1.5f),
                    strokeWidth = 1.5f
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 28.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = GhaisNoir.OnChrome.copy(alpha = alpha),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = text,
                color = GhaisNoir.OnChrome.copy(alpha = alpha),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Secondary action: ghost pill — white wash fill + hairline, white label. */
@Composable
fun GhostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val fill = if (active) GhaisNoir.Fill4 else GhaisNoir.Fill2
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(fill)
            .border(1.dp, if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
