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
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkObsidian)
            .border(0.5.dp, HairlineSubtleBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(
                                        width = 0.5.dp,
                                        brush = HairlineSpecularBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onSelectTab(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) OffWhiteText else SystemGrey
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
        // Section Header with Glowing Dot Indicator and 48dp touch target on "View All"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Curated Themes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhiteText,
                    letterSpacing = (-0.4).sp
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(QuranifyColors.Primary)
                )
            }

            // "View All" Button with 48dp minimum touch target
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onThemeClick("all") },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, HairlineSubtleBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuranifyColors.Primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Editorial Glass Bento Grid
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
                        Color(0xFF0D2E22),
                        Color(0xFF0F1A16),
                        DarkObsidian
                    ),
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
                        Color(0xFF2E220D),
                        Color(0xFF1C1710),
                        DarkObsidian
                    ),
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
                        Color(0xFF142B24),
                        Color(0xFF101C18),
                        DarkObsidian
                    ),
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
                        Color(0xFF2B2114),
                        Color(0xFF1A1510),
                        DarkObsidian
                    ),
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(116.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(brush = Brush.linearGradient(gradientColors))
            .border(
                width = 0.5.dp,
                brush = HairlineSpecularBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
    ) {
        // Diagonal glass sheen overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        // Top specular hairline inside the card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.75.dp)
                .padding(horizontal = 20.dp)
                .offset(y = 5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Watermark Icon in bottom-right corner with subtle opacity
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
        ) {
            Icon(
                imageVector = watermarkIcon,
                contentDescription = null,
                tint = badgeColor.copy(alpha = 0.10f),
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhiteText,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SystemGrey,
                    maxLines = 1
                )
            }

            // Glass Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkObsidian.copy(alpha = 0.70f))
                    .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}
