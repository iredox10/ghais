package com.quranify.ui.screens.library.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.ui.theme.QuranifyColors
import kotlin.math.roundToInt

@Composable
fun LibraryHeaderSection(
    selectedFilter: String = "All",
    onSelectFilter: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LibraryTopBar(
            onSearchClick = onSearchClick,
            onAddClick = onAddClick
        )
        LibraryFilterChips(
            selectedFilter = selectedFilter,
            onSelectFilter = onSelectFilter
        )
        Spacer(modifier = Modifier.height(4.dp))
        RamadanKhatmCard(
            onResumeClick = onResumeClick,
            onClick = onCardClick
        )
    }
}

@Composable
fun LibraryTopBar(
    onSearchClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Library",
                color = QuranifyColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Live green dot
            val infiniteTransition = rememberInfiniteTransition(label = "LiveDotTransition")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "DotAlpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(QuranifyColors.Primary, CircleShape)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.SurfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = QuranifyColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.SurfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = QuranifyColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LibraryFilterChips(
    selectedFilter: String = "All",
    onSelectFilter: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    filters: List<String> = listOf("All", "Playlists", "Downloaded", "Saved Verses", "Reciters", "Hifz Goals")
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters, key = { it }) { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) QuranifyColors.Primary else QuranifyColors.SurfaceContainer
                    )
                    .clickable { onSelectFilter(filter) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = filter,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) QuranifyColors.OnPrimary else QuranifyColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun RamadanKhatmCard(
    onResumeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    currentJuz: Int = 13,
    totalJuz: Int = 30,
    daysLeft: Int = 24,
    progress: Float = 0.42f
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(QuranifyColors.SurfaceContainer)
            .clickable { onClick() }
            .padding(16.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(QuranifyColors.SurfaceContainer)
            .padding(16.dp)
    }

    Box(modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(QuranifyColors.Secondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Ramadan Goal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuranifyColors.Secondary
                        )
                    }
                    Text(
                        text = "$daysLeft days left",
                        fontSize = 11.sp,
                        color = QuranifyColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ramadan Khatm Tracker",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Text(
                    text = "Juz $currentJuz of $totalJuz • On track today",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(QuranifyColors.Primary)
                            .clickable { onResumeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = QuranifyColors.OnPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Resume Juz $currentJuz",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuranifyColors.OnPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Trending",
                            tint = QuranifyColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "+18m daily avg",
                            fontSize = 10.sp,
                            color = QuranifyColors.TextTertiary
                        )
                    }
                }
            }

            // Right: Circular Progress Arc
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val arcPaddingX = (size.width - diameter) / 2f
                    val arcPaddingY = (size.height - diameter) / 2f
                    val arcSize = Size(diameter, diameter)
                    val topLeft = Offset(arcPaddingX, arcPaddingY)

                    // Track
                    drawCircle(
                        color = QuranifyColors.SurfaceHighest,
                        radius = diameter / 2f,
                        style = Stroke(width = strokeWidth)
                    )
                    // Active arc: 42% = 151.2 degrees
                    drawArc(
                        color = QuranifyColors.Primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )
                    Text(
                        text = "KHATM",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuranifyColors.TextSecondary
                    )
                }
            }
        }
    }
}
