package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Noir Glass reciters chrome — strict monochrome.
 *
 * - Count badge: chrome pill (white gradient, near-black label), the
 *   monochrome stand-in for the old blue badge.
 * - Section divider: 1px ghost hairline.
 */

/** Chrome count pill — informational, no press affordance. */
@Composable
fun RegionCountBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.chromeFill())
            .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
            .drawBehind {
                // Inner top highlight — light catching the chrome edge.
                drawLine(
                    GhaisNoir.ChromeInnerHighlight,
                    Offset(size.width * 0.18f, 1.5f),
                    Offset(size.width * 0.82f, 1.5f),
                    strokeWidth = 1.5f
                )
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GhaisNoir.OnChrome
        )
    }
}

/** Thin ghost divider separating reciter sections. */
@Composable
fun ScrimSectionDivider() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(1.dp)
                .background(GhaisNoir.BorderGhost)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
