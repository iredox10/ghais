package com.quranify.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C0F10))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(QuranifyColors.SurfaceContainer)
                                    .border(
                                        1.dp,
                                        QuranifyColors.Primary.copy(alpha = 0.35f),
                                        RoundedCornerShape(10.dp)
                                    )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onSelectTab(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextTertiary
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Section Header with Glowing Dot Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Curated Themes",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(QuranifyColors.Primary)
                )
            }

            Text(
                text = "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuranifyColors.Primary,
                modifier = Modifier.clickable { onThemeClick("all") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Spotify-Style Editorial Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Meccan & Medinan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    title = "Meccan",
                    subtitle = "86 Surahs • Faith & Soul",
                    badgeText = "Origins",
                    badgeColor = QuranifyColors.Primary,
                    watermarkIcon = Icons.Default.Mosque,
                    gradientColors = listOf(
                        Color(0xFF0D3829),
                        Color(0xFF13221B),
                        QuranifyColors.Background
                    ),
                    borderColor = QuranifyColors.Primary.copy(alpha = 0.25f),
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("meccan") }
                )

                ThemeCard(
                    title = "Medinan",
                    subtitle = "28 Surahs • Society & Laws",
                    badgeText = "Guidance",
                    badgeColor = QuranifyColors.Secondary,
                    watermarkIcon = Icons.Default.Balance,
                    gradientColors = listOf(
                        Color(0xFF3B2A10),
                        Color(0xFF221A11),
                        QuranifyColors.Background
                    ),
                    borderColor = QuranifyColors.Secondary.copy(alpha = 0.25f),
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("medinan") }
                )
            }

            // Row 2: Healing & Ruqyah & Stories of Prophets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    title = "Healing & Ruqyah",
                    subtitle = "Calm, Ease & Comfort",
                    badgeText = "Inner Peace",
                    badgeColor = Color(0xFF68DBA9),
                    watermarkIcon = Icons.Default.Spa,
                    gradientColors = listOf(
                        Color(0xFF16322A),
                        Color(0xFF14201C),
                        QuranifyColors.Background
                    ),
                    borderColor = Color(0xFF68DBA9).copy(alpha = 0.22f),
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeClick("ruqyah") }
                )

                ThemeCard(
                    title = "Stories of Prophets",
                    subtitle = "Yusuf, Musa, Ibrahim",
                    badgeText = "Narratives",
                    badgeColor = Color(0xFFFFDD78),
                    watermarkIcon = Icons.Default.AutoStories,
                    gradientColors = listOf(
                        Color(0xFF33271A),
                        Color(0xFF1E1813),
                        QuranifyColors.Background
                    ),
                    borderColor = Color(0xFFFFDD78).copy(alpha = 0.22f),
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
    badgeColor: Color,
    watermarkIcon: ImageVector,
    gradientColors: List<Color>,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(brush = Brush.linearGradient(gradientColors))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        // Watermark Icon in bottom-right corner with subtle opacity
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
        ) {
            Icon(
                imageVector = watermarkIcon,
                contentDescription = null,
                tint = badgeColor.copy(alpha = 0.14f),
                modifier = Modifier.size(72.dp)
            )
        }

        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = QuranifyColors.TextSecondary.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }

            // Glass Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0F10).copy(alpha = 0.65f))
                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
