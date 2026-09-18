package com.quranify.ui.screens.player.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top bar for the Now Playing screen adhering to the Stitch Obsidian & Emerald design system.
 */
@Composable
fun NowPlayingTopBar(
    playlistTitle: String = "Heart Soothing Recitations",
    onMinimizeClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onPlaylistDropdownClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    playlistName: String? = null,
    onBackClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null
) {
    val displayTitle = playlistName ?: playlistTitle
    val handleMinimize = onBackClick ?: onMinimizeClick
    val handleMoreOptions = onOptionsClick ?: onMoreOptionsClick

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimize player icon button
        IconButton(
            onClick = handleMinimize,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize player",
                tint = QuranifyColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Center column: 'PLAYING FROM PLAYLIST' + Playlist title dropdown
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "PLAYING FROM PLAYLIST",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = QuranifyColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPlaylistDropdownClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Playlist dropdown",
                    tint = QuranifyColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // More options icon button
        IconButton(
            onClick = handleMoreOptions,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = QuranifyColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Animated glowing green ping dot representing live status/presence.
 */
@Composable
fun GlowingPingDot(
    modifier: Modifier = Modifier,
    dotColor: Color = QuranifyColors.Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PingTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing ping halo
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .background(dotColor, CircleShape)
        )
        // Solid center dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
    }
}

/**
 * Floating Tajweed Ribbon pill badge at the bottom of the vinyl disc artwork.
 */
@Composable
fun FloatingTajweedRibbon(
    riwayahText: String = "Hafs 'an 'Asim",
    modifier: Modifier = Modifier,
    riwayah: String? = null
) {
    val text = riwayah ?: riwayahText
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = QuranifyColors.SurfaceHigh,
        border = BorderStroke(1.dp, QuranifyColors.Primary.copy(alpha = 0.25f)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowingPingDot(dotColor = QuranifyColors.Primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = QuranifyColors.Primary
            )
        }
    }
}

/**
 * Sacred Islamic Art Mandala rendered geometrically on Canvas.
 */
@Composable
fun SacredGeometryMandala(
    modifier: Modifier = Modifier,
    primaryColor: Color = QuranifyColors.Primary,
    accentColor: Color = QuranifyColors.Secondary
) {
    Canvas(modifier = modifier) {
        val center = this.center
        val radius = size.minDimension / 2f

        // Dark emerald radiant background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF132D22),
                    Color(0xFF0D1C15),
                    Color(0xFF08120E)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Outer decorative ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = radius * 0.94f,
            center = center,
            style = Stroke(width = 1.2f)
        )

        drawCircle(
            color = accentColor.copy(alpha = 0.25f),
            radius = radius * 0.88f,
            center = center,
            style = Stroke(width = 0.8f)
        )

        // 16-point Islamic geometric star
        val points16 = 16
        val starOuterRadius = radius * 0.82f
        val starInnerRadius = radius * 0.58f
        val path16 = Path()
        for (i in 0 until points16 * 2) {
            val angle = (i * PI / points16).toFloat()
            val r = if (i % 2 == 0) starOuterRadius else starInnerRadius
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) path16.moveTo(x, y) else path16.lineTo(x, y)
        }
        path16.close()
        drawPath(
            path = path16,
            color = primaryColor.copy(alpha = 0.38f),
            style = Stroke(width = 1.4f)
        )

        // 8-point interlaced squares (Rub el Hizb style)
        for (squareIndex in 0..1) {
            val rotationAngle = squareIndex * (PI / 4.0).toFloat()
            val squarePath = Path()
            val sqRadius = radius * 0.70f
            for (corner in 0..3) {
                val cornerAngle = rotationAngle + (corner * (PI / 2.0).toFloat())
                val x = center.x + sqRadius * cos(cornerAngle)
                val y = center.y + sqRadius * sin(cornerAngle)
                if (corner == 0) squarePath.moveTo(x, y) else squarePath.lineTo(x, y)
            }
            squarePath.close()
            drawPath(
                path = squarePath,
                color = accentColor.copy(alpha = 0.45f),
                style = Stroke(width = 1.2f)
            )
        }

        // Concentric geometric ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.3f),
            radius = radius * 0.48f,
            center = center,
            style = Stroke(width = 1f)
        )

        // Radiating geometric spokes
        for (i in 0 until 8) {
            val angle = (i * (PI / 4.0)).toFloat()
            val startX = center.x + (radius * 0.32f) * cos(angle)
            val startY = center.y + (radius * 0.32f) * sin(angle)
            val endX = center.x + (radius * 0.72f) * cos(angle)
            val endY = center.y + (radius * 0.72f) * sin(angle)
            drawLine(
                color = primaryColor.copy(alpha = 0.28f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }

        // Inner rosette circle
        drawCircle(
            color = accentColor.copy(alpha = 0.4f),
            radius = radius * 0.32f,
            center = center,
            style = Stroke(width = 1f)
        )
    }
}

/**
 * Backward compatibility alias for SacredGeometryMandala.
 */
@Composable
fun SacredMandalaArtwork(
    modifier: Modifier = Modifier,
    primaryColor: Color = QuranifyColors.Primary,
    accentColor: Color = QuranifyColors.Secondary
) {
    SacredGeometryMandala(
        modifier = modifier,
        primaryColor = primaryColor,
        accentColor = accentColor
    )
}

/**
 * Vinyl record grooved texture with subtle concentric rings and specular sheen.
 */
@Composable
fun VinylRecordGrooves(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = this.center
        val maxRadius = size.minDimension / 2f
        val innerHoleRadius = maxRadius * 0.54f

        // Base vinyl disc gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E2224),
                    Color(0xFF141718),
                    Color(0xFF0E1011)
                ),
                center = center,
                radius = maxRadius
            ),
            radius = maxRadius,
            center = center
        )

        // Opposing specular sheen wedges (realistic vinyl reflections)
        val sheenColor = Color.White.copy(alpha = 0.035f)
        val sheenPath = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(center.x - maxRadius, center.y - maxRadius, center.x + maxRadius, center.y + maxRadius),
                startAngleDegrees = 35f,
                sweepAngleDegrees = 30f,
                forceMoveTo = false
            )
            close()
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(center.x - maxRadius, center.y - maxRadius, center.x + maxRadius, center.y + maxRadius),
                startAngleDegrees = 215f,
                sweepAngleDegrees = 30f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(path = sheenPath, color = sheenColor)

        // Concentric circular micro-grooves
        val numGrooves = 14
        val grooveStep = (maxRadius - innerHoleRadius) / (numGrooves + 1)
        for (i in 1..numGrooves) {
            val r = innerHoleRadius + (i * grooveStep)
            val alpha = if (i % 3 == 0) 0.12f else 0.05f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = center,
                style = Stroke(width = 0.8f)
            )
        }

        // Section groove border
        drawCircle(
            color = Color(0xFF2C3235).copy(alpha = 0.5f),
            radius = innerHoleRadius + (maxRadius - innerHoleRadius) * 0.5f,
            center = center,
            style = Stroke(width = 1.2f)
        )

        // Outer rim border
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = maxRadius - 1f,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }
}

/**
 * Center soundwave badge: small circular badge (size 48.dp, background QuranifyColors.SurfaceContainer) with secondary amber symbol.
 */
@Composable
fun CenterSoundwaveBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 48.dp,
    iconTint: Color = QuranifyColors.Secondary
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(QuranifyColors.SurfaceContainer)
            .border(1.dp, QuranifyColors.OutlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Center Soundwave Badge",
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Vinyl disc record: grooved disc with central artwork (AsyncImage or SacredGeometryMandala)
 * and central soundwave badge.
 */
@Composable
fun VinylDiscRecord(
    imageUrl: String = "",
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Circular vinyl disc container (size 220.dp)
    Box(
        modifier = modifier
            .size(220.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Vinyl disc rotation animation when playing
        val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "VinylAngle"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isPlaying) rotation else 0f),
            contentAlignment = Alignment.Center
        ) {
            // Outer disc: dark circular vinyl record texture with subtle ring borders
            VinylRecordGrooves(modifier = Modifier.fillMaxSize())

            // Center image: AsyncImage or Box with sacred Islamic art mandala
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(CircleShape)
                    .border(1.dp, QuranifyColors.Secondary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Surah Center Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SacredGeometryMandala(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Center soundwave badge
        CenterSoundwaveBadge()
    }
}

/**
 * Vinyl Disc Artwork component for Now Playing screen:
 * - Atmospheric glowing radiant background: circular blur gradient (Brush.radialGradient)
 * - Circular vinyl disc record container
 * - Floating Tajweed Ribbon at bottom edge
 */
@Composable
fun VinylDiscArtwork(
    imageUrl: String? = null,
    riwayah: String = "Hafs 'an 'Asim",
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric glowing radiant background: circular blur gradient (Brush.radialGradient)
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuranifyColors.Primary.copy(alpha = 0.22f),
                            QuranifyColors.Primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Disc container with bottom floating ribbon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            VinylDiscRecord(
                imageUrl = imageUrl ?: "",
                isPlaying = isPlaying
            )

            // Floating Tajweed Ribbon at bottom edge: Pill with glowing green ping dot + text 'Hafs \'an \'Asim'
            FloatingTajweedRibbon(
                riwayahText = riwayah,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 16.dp)
            )
        }
    }
}

/**
 * Now Playing Vinyl Disc composable exported and accessible with parameters:
 * @param imageUrl Surah center artwork URL
 * @param riwayahText Riwayah recitation authority text (e.g. "Hafs 'an 'Asim")
 * @param modifier Modifier for styling and layout
 */
@Composable
fun NowPlayingVinylDisc(
    imageUrl: String = "",
    riwayahText: String = "Hafs 'an 'Asim",
    modifier: Modifier = Modifier
) {
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    VinylDiscArtwork(
        imageUrl = imageUrl,
        riwayah = riwayahText,
        isPlaying = isPlaying,
        modifier = modifier
    )
}

/**
 * Overload for NowPlayingVinylDisc allowing explicit playback state control.
 */
@Composable
fun NowPlayingVinylDisc(
    imageUrl: String,
    riwayahText: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    VinylDiscArtwork(
        imageUrl = imageUrl,
        riwayah = riwayahText,
        isPlaying = isPlaying,
        modifier = modifier
    )
}

/**
 * Composite Now Playing Artwork header containing Top Bar and Vinyl Disc Artwork.
 */
@Composable
fun NowPlayingArtwork(
    playlistTitle: String = "Heart Soothing Recitations",
    imageUrl: String? = null,
    riwayah: String = "Hafs 'an 'Asim",
    isPlaying: Boolean = false,
    onMinimizeClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onPlaylistDropdownClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NowPlayingTopBar(
            playlistTitle = playlistTitle,
            onMinimizeClick = onMinimizeClick,
            onMoreOptionsClick = onMoreOptionsClick,
            onPlaylistDropdownClick = onPlaylistDropdownClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        VinylDiscArtwork(
            imageUrl = imageUrl,
            riwayah = riwayah,
            isPlaying = isPlaying
        )
    }
}
