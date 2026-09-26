package com.ghais.ui.components.noir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 2 — Noir rows: the bento "Netflix / Proton VPN" list row with
 * icon-well + dual text + circular chevron affordance + badge dot.
 */
@Composable
fun NoirListRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    var rowModifier = modifier
        .fillMaxWidth()
        .clip(GhaisShapes.row)
        .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
        .topSpecular(inset = 22.dp)
    if (onClick != null) rowModifier = rowModifier.noirClickable(onClick)
    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconWell(icon = icon, size = 44.dp, iconSize = 22.dp)
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(GhaisNoir.chromeFill(), CircleShape)
                        .border(1.5.dp, GhaisNoir.NoirBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.coerceAtMost(9).toString(),
                        color = GhaisNoir.OnChrome,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = subtitleLetterSpacing(subtitle)
            )
        }
        if (trailing != null) {
            trailing()
        } else if (chevron) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private val ARABIC_IN_SUBTITLE = Regex("[؀-ۿݐ-ݿﭐ-﷿ﹰ-ﻼ]")

/**
 * Subtitles often mix an Arabic surah name with a Latin count. The theme's
 * `bodyLarge` tracking is negative, and per CSS Text L3 §7.2.1 tracking must
 * not be inserted between the letters of a cursive script, so it is zeroed
 * whenever the string actually contains Arabic. Latin-only subtitles keep
 * inheriting the theme value untouched.
 */
private fun subtitleLetterSpacing(subtitle: String) =
    if (ARABIC_IN_SUBTITLE.containsMatchIn(subtitle)) 0.sp else TextUnit.Unspecified
