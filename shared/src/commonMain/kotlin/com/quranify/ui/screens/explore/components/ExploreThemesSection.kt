package com.quranify.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.ui.theme.QuranifyColors

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
            .background(QuranifyColors.SurfaceLow, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) QuranifyColors.SurfaceContainer else QuranifyColors.SurfaceLow
                        )
                        .clickable { onSelectTab(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CuratedThemesGrid(
    onThemeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Curated Themes",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = QuranifyColors.TextPrimary
            )
            Text(
                text = "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = QuranifyColors.Primary,
                modifier = Modifier.clickable { onThemeClick("all") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    title = "Meccan",
                    subtitle = "86 Surahs • Faith & Soul",
                    badgeText = "Origins",
                    gradientColors = listOf(
                        QuranifyColors.PrimaryContainer.copy(alpha = 0.4f),
                        QuranifyColors.SurfaceContainer
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("meccan") }
                )
                ThemeCard(
                    title = "Medinan",
                    subtitle = "28 Surahs • Society & Laws",
                    badgeText = "Guidance",
                    gradientColors = listOf(
                        QuranifyColors.SecondaryContainer.copy(alpha = 0.35f),
                        QuranifyColors.SurfaceContainer
                    ),
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
                    gradientColors = listOf(
                        QuranifyColors.SurfaceHigh,
                        QuranifyColors.SurfaceContainer
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("ruqyah") }
                )
                ThemeCard(
                    title = "Day of Judgment",
                    subtitle = "Reflection & Eternity",
                    badgeText = "Eternity",
                    gradientColors = listOf(
                        QuranifyColors.SurfaceHigh,
                        QuranifyColors.SurfaceContainer
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("judgment") }
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
    gradientColors: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush = Brush.linearGradient(gradientColors))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = QuranifyColors.TextSecondary,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        QuranifyColors.Background.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.Primary
                )
            }
        }
    }
}
