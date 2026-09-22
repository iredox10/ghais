package com.ghais.ui.screens.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Synchronized Ayah Lyrics Card — Noir Glass (monochrome Carbon Glass).
 *
 * - Card: elevated glass fill + ghost border + top specular hairline.
 * - Ambient sheens: white-only radial glows (zero hue).
 * - Active ayah: engraved inset section, white gradient indicator bar,
 *   arabic in [TextPrimary], transliteration in [TextTertiary] italic,
 *   translation in [TextSecondary].
 * - Signature unchanged (used by NowPlayingScreen: arabicVerse + modifier).
 */
@Composable
fun NowPlayingLyricsCard(
    arabicVerse: String = "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
    transliteration: String = "\"Fabi-ayyi ala-i Rabbikuma tukaththiban\"",
    translation: String = "So which of the favors of your Lord would you both deny?",
    upcomingAyahNumber: Int = 14,
    upcomingArabic: String = "خَلَقَ الْإِنسَانَ مِن صَلْصَالٍ كَالْفَخَّارِ",
    upcomingTranslation: String = "He created man from clay like that of pottery...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFill())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular()
    ) {
        // Ambient monochrome corner sheens (white only, zero hue)
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GhaisNoir.AmbientGlow,
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GhaisNoir.Fill1,
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Subtitles icon + SYNCHRONIZED AYAH LYRICS + Sync ghost pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYNCHRONIZED AYAH LYRICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        color = GhaisNoir.TextSecondary
                    )
                }

                // Sync Active ghost pill with white indicator dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Sync Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Recited Ayah — engraved inset section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.row)
                    .background(GhaisNoir.insetFill())
                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
                    .padding(14.dp)
            ) {
                // Left monochrome indicator bar (white gradient)
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(72.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    GhaisNoir.TextPrimary,
                                    GhaisNoir.TextSecondary,
                                    GhaisNoir.TextTertiary
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                ) {
                    // Arabic verse — primary rung of the text ladder
                    Text(
                        text = arabicVerse,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary,
                        textAlign = TextAlign.End,
                        lineHeight = 44.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Phonetic transliteration — tertiary italic
                    Text(
                        text = transliteration,
                        fontSize = 12.5.sp,
                        fontStyle = FontStyle.Italic,
                        color = GhaisNoir.TextTertiary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // English translation — secondary
                    Text(
                        text = translation,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextSecondary,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upcoming Ayah (previewed softly in muted white)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.55f }
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "AYAH $upcomingAyahNumber PREVIEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = GhaisNoir.TextTertiary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = upcomingArabic,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Normal,
                    color = GhaisNoir.TextSecondary,
                    textAlign = TextAlign.End,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = upcomingTranslation,
                    fontSize = 12.sp,
                    color = GhaisNoir.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Waveform + Micro-Track Progress Scrubber — Noir Glass monochrome.
 *
 * - Played bars: solid white; cursor thumb: chrome pill; unplayed: faint white.
 * - Linear track: engraved inset with chrome fill. Timestamps on text ladder.
 * - Signature unchanged.
 */
@Composable
fun NowPlayingScrubber(
    progress: Float,
    elapsedText: String,
    totalText: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simulated acoustic waveform bar heights (28 bars)
    val barAmplitudes = remember {
        listOf(
            8, 12, 18, 14, 22, 11, 19, 15, 23, 20,
            12, 22, 16, 20, 13, 17, 10, 19, 21, 14,
            18, 11, 15, 9, 16, 20, 12, 8
        )
    }
    val barCount = barAmplitudes.size
    val clampedProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val activeIndex = (clampedProgress * (barCount - 1)).toInt()

            // Waveform visualizer bars — grayscale only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                barAmplitudes.forEachIndexed { index, ampDp ->
                    val isPlayed = index <= activeIndex
                    val isCurrent = index == activeIndex

                    if (isCurrent) {
                        // Chrome cursor thumb pill
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.chromeFill())
                                .border(1.dp, GhaisNoir.SpecularTop.copy(alpha = 0.5f), CircleShape)
                        )
                    } else {
                        // Standard waveform bar: white played / ghost unplayed
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(ampDp.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlayed) GhaisNoir.TextPrimary
                                    else Color.White.copy(alpha = 0.18f)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Engraved Micro Track with chrome fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(GhaisNoir.insetFill())
                .border(1.dp, GhaisNoir.InsetBorder, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(GhaisNoir.chromeFill())
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Timestamps — text ladder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = elapsedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GhaisNoir.TextSecondary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Audio Playing",
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = totalText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GhaisNoir.TextSecondary
                )
            }
        }
    }
}

/**
 * Playback Controls — Noir Glass monochrome.
 *
 * - Side buttons: ghost wells (Shuffle / Previous / Next / Repeat), active
 *   toggles read bright white, inactive dim tertiary.
 * - Hero Play/Pause: chrome disc with near-black glyph + neutral shadow
 *   (no colored ambient/spot glow). Repeat "∞" badge in OnChrome.
 * - Signature unchanged.
 */
@Composable
fun NowPlayingControlsBar(
    isPlaying: Boolean,
    isShuffle: Boolean = false,
    isRepeat: Boolean = false,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit = {},
    onRepeatToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Recitation Mode (ghost well)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable { onShuffleToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle Recitation Mode",
                tint = if (isShuffle) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Previous Ayah Button (ghost well)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable { onPrevious() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Ayah",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Central Hero Play / Pause — chrome disc, near-black glyph
        val playScale by animateFloatAsState(
            targetValue = if (isPlaying) 1.0f else 0.97f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "PlayScale"
        )

        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(GhaisNoir.chromeFill())
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                .noirClickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause Recitation" else "Play Recitation",
                tint = GhaisNoir.OnChrome,
                modifier = Modifier.size(36.dp)
            )
        }

        // Next Ayah Button (ghost well)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Ayah",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Memorization Repeat Mode Toggle (ghost well)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(
                    1.dp,
                    if (isRepeat) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                    CircleShape
                )
                .noirClickable { onRepeatToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRepeat) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Repeat Mode",
                tint = if (isRepeat) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            if (isRepeat) {
                Text(
                    text = "∞",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 4.dp)
                )
            }
        }
    }
}
