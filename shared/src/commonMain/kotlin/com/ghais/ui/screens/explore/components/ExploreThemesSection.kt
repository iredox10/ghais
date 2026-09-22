package com.ghais.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Noir Glass segmented switcher — strict monochrome.
 *
 * Engraved inset track (carved-in black gradient + 5% rim); the selected
 * segment is an extruded chrome pill (white gradient, near-black label),
 * resting segments are transparent with 38% labels. State reads through
 * fill elevation + weight, never hue.
 */
@Composable
fun ExploreSegmentedSwitcher(
    selectedTab: Int = 0,
    onSelectTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Surah Index", "Juz Index", "Topics")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(GhaisShapes.row)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedTab
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.chromeFill())
                            .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                            .noirClickable { onSelectTab(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.OnChrome
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(GhaisShapes.pill)
                            .background(Color.Transparent)
                            .noirClickable { onSelectTab(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Noir Glass curated-themes bento — strict monochrome.
 *
 * Header follows the shared [NoirSectionHeader] rhythm (bright label + ghost
 * "View All" action); tiles use the elevated card recipe (gradient fill +
 * ghost border + top-only specular + diagonal sheen). Watermarks and badges
 * are white-alpha washes — zero hue anywhere.
 */
@Composable
fun CuratedThemesGrid(
    onThemeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        NoirSectionHeader(
            label = "Curated Themes",
            actionLabel = "View All",
            onAction = { onThemeClick("all") }
        )

        Text(
            text = "Faith, law, comfort & stories",
            fontSize = 12.sp,
            color = GhaisNoir.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // 2x2 bento grid.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    title = "Meccan",
                    subtitle = "86 Surahs • Faith & Soul",
                    badgeText = "Origins",
                    watermarkIcon = Icons.Default.Mosque,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("meccan") }
                )

                ThemeCard(
                    title = "Medinan",
                    subtitle = "28 Surahs • Society & Laws",
                    badgeText = "Guidance",
                    watermarkIcon = Icons.Default.Balance,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("medinan") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    title = "Healing & Ruqyah",
                    subtitle = "Calm, Ease & Comfort",
                    badgeText = "Inner Peace",
                    watermarkIcon = Icons.Default.Spa,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("ruqyah") }
                )

                ThemeCard(
                    title = "Stories of Prophets",
                    subtitle = "Yusuf, Musa, Ibrahim",
                    badgeText = "Narratives",
                    watermarkIcon = Icons.Default.AutoStories,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("stories") }
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    watermarkIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(116.dp)
            .heightIn(min = 48.dp)
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFill())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        // Diagonal glass sheen sweep (monochrome only).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )

        // Watermark glyph in bottom-right — white wash, never tinted.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
        ) {
            Icon(
                imageVector = watermarkIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(72.dp)
            )
        }

        // Foreground content.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = GhaisNoir.TextSecondary,
                    maxLines = 1
                )
            }

            // Ghost pill badge — informational only, no press affordance.
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextSecondary
                )
            }
        }
    }
}
