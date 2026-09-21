package com.ghais.ui.screens.player.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.ghais.data.seed.StitchAssets
import com.ghais.player.AudioEngine
import com.ghais.ui.theme.QuranifyColors
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
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(26.dp)
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
                color = QuranifyColors.Primary.copy(alpha = 0.85f)
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
                    tint = QuranifyColors.Primary,
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
                tint = Color.White.copy(alpha = 0.85f),
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
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(14.dp),
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
        color = Color(0xDD181D1A),
        border = BorderStroke(1.dp, Color(0x334EDEA3)),
        shadowElevation = 8.dp
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
                letterSpacing = 0.8.sp,
                color = QuranifyColors.Primary
            )
        }
    }
}

/**
 * Sublime Sacred Islamic Geometric Mandala rendered on Canvas with authentic
 * multi-layered 16-point star geometry, Rub el Hizb interlacing, golden filigree rosettes,
 * and radiating emerald & gold arabesque accents.
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

        // 1. Deep Midnight Obsidian & Dark Emerald radiant background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF133626),
                    Color(0xFF0C2419),
                    Color(0xFF071710),
                    Color(0xFF030A07)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // 2. Outer rim concentric decorative rings
        drawCircle(
            color = primaryColor.copy(alpha = 0.45f),
            radius = radius * 0.95f,
            center = center,
            style = Stroke(width = 1.4f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.35f),
            radius = radius * 0.90f,
            center = center,
            style = Stroke(width = 0.8f)
        )

        // 3. Perimeter bead accents (32 golden pearls around circle)
        val beadRadius = radius * 0.925f
        for (i in 0 until 32) {
            val angle = (i * 2.0 * PI / 32).toFloat()
            val bx = center.x + beadRadius * cos(angle)
            val by = center.y + beadRadius * sin(angle)
            drawCircle(
                color = if (i % 2 == 0) accentColor.copy(alpha = 0.6f) else primaryColor.copy(alpha = 0.45f),
                radius = 1.3f,
                center = Offset(bx, by)
            )
        }

        // 4. 16-point Islamic geometric star
        val points16 = 16
        val starOuter = radius * 0.85f
        val starInner = radius * 0.62f
        val star16Path = Path()
        for (i in 0 until points16 * 2) {
            val angle = (i * PI / points16).toFloat()
            val r = if (i % 2 == 0) starOuter else starInner
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) star16Path.moveTo(x, y) else star16Path.lineTo(x, y)
        }
        star16Path.close()
        drawPath(
            path = star16Path,
            color = primaryColor.copy(alpha = 0.42f),
            style = Stroke(width = 1.4f)
        )

        // 5. 8-point interlaced Rub el Hizb squares (two rotated 45° squares)
        for (sq in 0..1) {
            val baseAngle = sq * (PI / 4.0).toFloat()
            val sqPath = Path()
            val sqR = radius * 0.73f
            for (c in 0..3) {
                val a = baseAngle + (c * (PI / 2.0).toFloat())
                val x = center.x + sqR * cos(a)
                val y = center.y + sqR * sin(a)
                if (c == 0) sqPath.moveTo(x, y) else sqPath.lineTo(x, y)
            }
            sqPath.close()
            drawPath(
                path = sqPath,
                color = accentColor.copy(alpha = 0.55f),
                style = Stroke(width = 1.3f)
            )
        }

        // 6. Interlaced arabesque curved petal loops
        for (i in 0 until 8) {
            val angle = (i * PI / 4.0).toFloat()
            val p1x = center.x + (radius * 0.36f) * cos(angle - 0.28f)
            val p1y = center.y + (radius * 0.36f) * sin(angle - 0.28f)
            val tipX = center.x + (radius * 0.64f) * cos(angle)
            val tipY = center.y + (radius * 0.64f) * sin(angle)
            val p2x = center.x + (radius * 0.36f) * cos(angle + 0.28f)
            val p2y = center.y + (radius * 0.36f) * sin(angle + 0.28f)

            val petalPath = Path().apply {
                moveTo(p1x, p1y)
                quadraticTo(
                    center.x + (radius * 0.52f) * cos(angle - 0.16f),
                    center.y + (radius * 0.52f) * sin(angle - 0.16f),
                    tipX, tipY
                )
                quadraticTo(
                    center.x + (radius * 0.52f) * cos(angle + 0.16f),
                    center.y + (radius * 0.52f) * sin(angle + 0.16f),
                    p2x, p2y
                )
            }
            drawPath(
                path = petalPath,
                color = primaryColor.copy(alpha = 0.38f),
                style = Stroke(width = 1.1f)
            )
        }

        // 7. Concentric geometric rings
        drawCircle(
            color = primaryColor.copy(alpha = 0.35f),
            radius = radius * 0.48f,
            center = center,
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.40f),
            radius = radius * 0.34f,
            center = center,
            style = Stroke(width = 1.1f)
        )

        // 8. Radiating geometric rays
        for (i in 0 until 16) {
            val angle = (i * (PI / 8.0)).toFloat()
            val startR = if (i % 2 == 0) radius * 0.34f else radius * 0.48f
            val endR = if (i % 2 == 0) radius * 0.75f else radius * 0.62f
            val startX = center.x + startR * cos(angle)
            val startY = center.y + startR * sin(angle)
            val endX = center.x + endR * cos(angle)
            val endY = center.y + endR * sin(angle)
            drawLine(
                color = if (i % 2 == 0) accentColor.copy(alpha = 0.32f) else primaryColor.copy(alpha = 0.25f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }

        // 9. Inner 8-pointed star rosette
        val rosettePath = Path()
        for (i in 0 until 16) {
            val angle = (i * PI / 8.0).toFloat()
            val r = if (i % 2 == 0) radius * 0.30f else radius * 0.18f
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) rosettePath.moveTo(x, y) else rosettePath.lineTo(x, y)
        }
        rosettePath.close()
        drawPath(
            path = rosettePath,
            color = accentColor.copy(alpha = 0.65f),
            style = Stroke(width = 1.2f)
        )

        // 10. Center glowing golden jewel medallion
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.7f),
                    accentColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.18f
            ),
            radius = radius * 0.18f,
            center = center
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
 * Vinyl record grooved texture with subtle concentric rings, radial gradient,
 * and realistic opposing specular sheen wedges.
 */
@Composable
fun VinylRecordGrooves(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = this.center
        val maxRadius = size.minDimension / 2f
        val innerHoleRadius = maxRadius * 0.52f

        // Base vinyl disc dark gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E2421),
                    Color(0xFF121614),
                    Color(0xFF070B09)
                ),
                center = center,
                radius = maxRadius
            ),
            radius = maxRadius,
            center = center
        )

        // Opposing specular sheen wedges (realistic vinyl reflections)
        val sheenColor = Color.White.copy(alpha = 0.04f)
        val sheenPath = Path().apply {
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(center.x - maxRadius, center.y - maxRadius, center.x + maxRadius, center.y + maxRadius),
                startAngleDegrees = 35f,
                sweepAngleDegrees = 32f,
                forceMoveTo = false
            )
            close()
            moveTo(center.x, center.y)
            arcTo(
                rect = Rect(center.x - maxRadius, center.y - maxRadius, center.x + maxRadius, center.y + maxRadius),
                startAngleDegrees = 215f,
                sweepAngleDegrees = 32f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(path = sheenPath, color = sheenColor)

        // Concentric circular micro-grooves
        val numGrooves = 16
        val grooveStep = (maxRadius - innerHoleRadius) / (numGrooves + 1)
        for (i in 1..numGrooves) {
            val r = innerHoleRadius + (i * grooveStep)
            val alpha = if (i % 4 == 0) 0.12f else 0.045f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = center,
                style = Stroke(width = 0.75f)
            )
        }

        // Section groove highlight ring
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = innerHoleRadius + (maxRadius - innerHoleRadius) * 0.5f,
            center = center,
            style = Stroke(width = 1.0f)
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
 * Center soundwave badge: Frosted glass circular cap with spinning infinity/soundwave symbol.
 */
@Composable
fun CenterSoundwaveBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 52.dp,
    iconTint: Color = QuranifyColors.Primary,
    isPlaying: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BadgeSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(Color(0xE6070B09))
            .border(1.dp, Color(0x40FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AllInclusive,
            contentDescription = "Center Symbol",
            tint = iconTint,
            modifier = Modifier
                .size(24.dp)
                .rotate(if (isPlaying) spinAngle else 0f)
        )
    }
}

/**
 * Vinyl disc record: grooved disc with central artwork (AsyncImage or SacredGeometryMandala),
 * specular gloss sheen, smooth rotation, and central soundwave badge.
 */
@Composable
fun VinylDiscRecord(
    imageUrl: String = "",
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Continuous rotation without jumping to 0 on pause
    val rotationAngle = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAngle.animateTo(
                    targetValue = rotationAngle.value + 360f,
                    animationSpec = tween(durationMillis = 20000, easing = LinearEasing)
                )
            }
        }
    }

    // Circular vinyl disc container
    Box(
        modifier = modifier
            .size(236.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Rotating disc body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle.value),
            contentAlignment = Alignment.Center
        ) {
            // Outer disc: dark circular vinyl record texture with micro-grooves
            VinylRecordGrooves(modifier = Modifier.fillMaxSize())

            // Center artwork container (124.dp)
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, QuranifyColors.Secondary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val resolvedUrl = imageUrl.ifBlank { StitchAssets.NowPlayingVinylArtUrl }
                if (resolvedUrl.isNotBlank()) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = "Surah Center Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SacredGeometryMandala(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Specular glass gloss sweep overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.09f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    ),
                    shape = CircleShape
                )
        )

        // Center glass soundwave cap
        CenterSoundwaveBadge(
            isPlaying = isPlaying,
            iconTint = QuranifyColors.Primary
        )
    }
}

/**
 * Vinyl Disc Artwork component for Now Playing screen:
 * - Multi-layered Apple-style atmospheric radiant glowing background halo
 * - Circular vinyl disc record container with microgrooves & specular light
 * - Floating liquid glass Tajweed Ribbon at bottom edge
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
        // Multi-layered atmospheric radiant background: glowing emerald & warm halo
        Box(
            modifier = Modifier
                .size(290.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuranifyColors.Primary.copy(alpha = 0.28f),
                            QuranifyColors.PrimaryContainer.copy(alpha = 0.12f),
                            QuranifyColors.Secondary.copy(alpha = 0.08f),
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
