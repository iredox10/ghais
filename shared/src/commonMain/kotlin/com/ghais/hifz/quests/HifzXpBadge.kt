package com.ghais.hifz.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Noir Hifz XP badge: chrome star well + level title + lifetime total.
 * Zero hue — mirrors the library CuratorBadge recipe.
 */
@Composable
fun HifzXpBadge(
    level: HifzXpLevel,
    totalXp: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(GhaisNoir.Fill2, GhaisShapes.pill)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(GhaisNoir.chromeFill(), GhaisShapes.well)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = GhaisNoir.OnChrome,
                modifier = Modifier.width(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = level.title.uppercase(),
            color = GhaisNoir.TextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$totalXp XP",
            color = GhaisNoir.TextTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
