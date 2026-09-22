package com.ghais.ui.components.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir

/**
 * Phase 2 — Noir meters: Password-Health-style segmented progress
 * (chrome fill on engraved track) + ghost section headers.
 */
@Composable
fun NoirSegmentedProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 8.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(CircleShape)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, CircleShape)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(GhaisNoir.chromeFill())
            )
        }
    }
}

/** Section header: bright label + ghost "See All"-style action slot. */
@Composable
fun NoirSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.noirClickable(onAction)
            )
        }
    }
}
