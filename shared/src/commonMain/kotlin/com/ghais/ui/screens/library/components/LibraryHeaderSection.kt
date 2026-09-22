package com.ghais.ui.screens.library.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DownloadDone
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.seed.GhaisAssets
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import org.jetbrains.compose.resources.painterResource
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import kotlin.math.roundToInt

/**
 * Phase 4 — Noir Library header.
 *
 * Strict Noir Glass monochrome: alpha-white glass, top-only specular
 * hairlines, chrome pills and grayscale imagery. Signatures, filter defaults
 * and callback contracts are unchanged; state reads through fill elevation,
 * weight and opacity — zero hue anywhere.
 */

/** True-grayscale filter — avatars stay recognisable, strictly monochrome. */
private val NoirLibraryGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Top App Header matching Stitch design specs:
 * Ghais logo, "Assalamu Alaikum", "Library" title, Search button & Profile avatar.
 */
@Composable
fun LibraryAppHeader(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ghais_mark),
                    contentDescription = "Ghais App Logo",
                    contentScale = ContentScale.Fit,
                    colorFilter = NoirLibraryGrayscale,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "ASSALAMU ALAIKUM",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Library",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Quran",
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            AsyncImage(
                model = GhaisAssets.ProfileAvatarUrl,
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                colorFilter = NoirLibraryGrayscale,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                    .clickable { onProfileClick() }
            )
        }
    }
}

/**
 * Composite header container including TopBar, FilterChips, and RamadanKhatmCard.
 */
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

/**
 * "Your Library" title row with live pulsing monochrome dot and quick action buttons.
 */
@Composable
fun LibraryTopBar(
    onSearchClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Library",
                color = GhaisNoir.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            // Live pulsing monochrome dot with soft glow (zero hue).
            val infiniteTransition = rememberInfiniteTransition(label = "LiveDotTransition")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "DotAlpha"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "DotScale"
            )

            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha * 0.4f)
                        .background(GhaisNoir.TextPrimary, CircleShape)
                )
                // Center dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer(alpha = alpha)
                        .background(GhaisNoir.TextPrimary, CircleShape)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Library",
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Playlist or Bookmark",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Filter chips row — selected chip is an extruded chrome pill (near-black
 * label), unselected chips are Fill2 washes with ghost rims. State reads
 * through elevation + label opacity, never hue.
 */
@Composable
fun LibraryFilterChips(
    selectedFilter: String = "All",
    onSelectFilter: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    filters: List<String> = listOf("All", "Playlists", "Downloaded", "Saved Verses", "Reciters", "Hifz Goals")
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters, key = { it }) { filter ->
            val isSelected = filter == selectedFilter
            val isDownloaded = filter == "Downloaded"
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable { onSelectFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = GhaisNoir.OnChrome,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.OnChrome
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .noirClickable { onSelectFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = GhaisNoir.TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ramadan Khatm progress card — soft glass fill + 1px card border + top-only
 * specular, monochrome progress ring, chrome resume pill. Depth comes from
 * fill elevation and a white top-right glow, never hue.
 */
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
    val cardShape = RoundedCornerShape(20.dp)
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(cardShape)
        .background(GhaisNoir.cardFill())
        .border(1.dp, GhaisNoir.BorderCard, cardShape)
        .topSpecular(inset = 22.dp)
        .drawBehind {
            // Subtle top-right monochrome glow (ambient light, zero hue).
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GhaisNoir.AmbientGlow,
                        Color.Transparent
                    ),
                    center = Offset(size.width, 0f),
                    radius = 120.dp.toPx()
                )
            )
        }
        .then(
            if (onClick != null) Modifier.noirClickable { onClick() } else Modifier
        )
        .padding(16.dp)

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
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RAMADAN GOAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                    Text(
                        text = "$daysLeft days left",
                        fontSize = 11.sp,
                        color = GhaisNoir.TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ramadan Khatm Tracker",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
                Text(
                    text = "Juz $currentJuz of $totalJuz • On track today",
                    fontSize = 12.sp,
                    color = GhaisNoir.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.chromeFill())
                            .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                            .noirClickable { onResumeClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = GhaisNoir.OnChrome,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Resume Juz $currentJuz",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.OnChrome
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Trending",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "+18m daily avg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Circular Progress Ring (monochrome arc on engraved track).
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.5.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val arcPaddingX = (size.width - diameter) / 2f
                    val arcPaddingY = (size.height - diameter) / 2f
                    val arcSize = Size(diameter, diameter)
                    val topLeft = Offset(arcPaddingX, arcPaddingY)

                    // Track circle (engraved).
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = diameter / 2f,
                        style = Stroke(width = strokeWidth)
                    )
                    // Active arc: chrome-white, round cap.
                    drawArc(
                        color = Color.White.copy(alpha = 0.92f),
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary
                    )
                    Text(
                        text = "KHATM",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = GhaisNoir.TextTertiary
                    )
                }
            }
        }
    }
}
