package com.ghais.ui.screens.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.player.AudioEngine
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.AyahVerse
import com.ghais.domain.model.TrackItem
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.util.appendAyahMark
import com.ghais.ui.util.ayahMarkContent
import com.ghais.ui.theme.GhaisTypography

/**
 * Synchronized Ayah Lyrics Card — MINIMAL (monochrome, zero hue).
 *
 * - Transparent surface: no card fills, borders, sheens, or engraved insets.
 *   Hairline dividers + spacing only.
 * - Header: single slim row — X ghost well + "AYAH {ayahNo}" text +
 *   sync dot + Surah name. No pills, no chevrons (transport covers prev/next).
 * - Verse: prominent Arabic Uthmani in quranScript (25.sp, RTL,
 *   2.0x line height) + enclosed rosette, translation secondary (14.5.sp).
 *   No transliteration, no tools row (loop/tafseer/learning/download live in
 *   their own screens), no NEXT preview, no Record & Compare.
 * - Range-loop and recitation-gap banners render only while active.
 * - All callbacks/logic/signatures preserved; scrubber + controls bar untouched.
 */
@Composable
fun NowPlayingLyricsCard(
    currentTrack: TrackItem?,
    currentAyahVerse: AyahVerse?,
    upcomingAyahVerse: AyahVerse?,
    isPlaying: Boolean,
    onNextAyah: (() -> Unit)? = null,
    onPreviousAyah: (() -> Unit)? = null,
    onToggleAyahMode: (() -> Unit)? = null,
    onOpenTafseer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isGapActive by AudioEngine.isRecitationGapActive.collectAsState()
    val gapCountdown by AudioEngine.recitationGapCountdown.collectAsState()
    val hifzRange by AudioEngine.hifzRange.collectAsState()
    val loadingSurahs by QuranAyahRepository.loadingSurahs.collectAsState()

    val displayAyahNo = currentTrack?.ayahNo?.takeIf { it > 0 }
        ?: currentAyahVerse?.ayahNo
        ?: 1

    val surahId = currentTrack?.surahId ?: currentAyahVerse?.surahId ?: 1

    val surahName = currentTrack?.surahNameEn?.takeIf { it.isNotBlank() }
        ?: currentAyahVerse?.surahId?.let { "Surah $it" }
        ?: ""

    val isCurrentVerseUnavailable = currentAyahVerse?.isPlaceholder != false ||
        !QuranAyahRepository.isAyahAvailable(surahId, displayAyahNo)
    val activeArabic = if (isCurrentVerseUnavailable) {
        ""
    } else {
        currentAyahVerse?.textUthmani?.takeIf { it.isNotBlank() } ?: ""
    }
    val translationText = if (isCurrentVerseUnavailable) {
        null
    } else {
        currentAyahVerse?.translation?.takeIf { it.isNotBlank() }
    }

    // MINIMAL Ayah mode: transparent surface, no card fills/borders/sheens.
    // Spacing + hairline dividers only. Verse stays prominent.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
            if (hifzRange != null) {
                val range = hifzRange!!
                val loopStr = if (range.targetLoops == -1) "${range.currentLoop}/∞"
                else "${range.currentLoop}/${range.targetLoops}"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANGE ${range.startAyah}–${range.endAyah} • LOOP $loopStr",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = GhaisNoir.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .noirClickable { AudioEngine.clearHifzRange() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Range Loop",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Header: single slim row — X ghost well + AYAH n + sync dot + surah. No pills.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (onToggleAyahMode != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.wellFill())
                                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                .noirClickable { onToggleAyahMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Ayah Mode",
                                tint = GhaisNoir.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = "AYAH $displayAyahNo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = GhaisNoir.TextPrimary
                    )

                    // Sync state as a quiet dot only (no pill)
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary)
                    )

                    if (surahName.isNotBlank()) {
                        Text(
                            text = surahName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isGapActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recite aloud • ${gapCountdown}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextSecondary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Skip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextTertiary,
                        modifier = Modifier.noirClickable { AudioEngine.skipRecitationGap() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GhaisNoir.BorderGhost)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Active verse — transparent, prominent. No inset, no indicator bar.
            // Arabic Uthmani in quranScript (25.sp, RTL) + enclosed rosette, 2.0x line height.
            // Basmalah header renders as a centered line above ayah 1 (except surahs 1 & 9).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                    val showBasmalahHeader =
                        displayAyahNo == 1 && QuranAyahRepository.hasBasmalahHeader(surahId)
                    if (isCurrentVerseUnavailable) {
                        Text(
                            text = if (surahId in loadingSurahs) "Loading ayah…" else "Ayah unavailable offline",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = GhaisNoir.TextTertiary,
                            lineHeight = 21.sp
                        )
                    } else {
                        // Arabic block runs RTL (basmalah header + verse Row); translation stays LTR.
                        if (showBasmalahHeader || activeArabic.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (showBasmalahHeader) {
                                        Text(
                                            text = QuranAyahRepository.BASMALAH,
                                            style = GhaisTypography.quranScript,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = GhaisNoir.TextSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 42.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                        )
                                    }
                                    // Arabic verse — QCF Hafs Uthmani text (100% white, RTL)
                                    // with the enclosed end-of-ayah rosette glued to the
                                    // last word (inline content). Normal weight:
                                    // synthetic bold perturbs mark shaping on dense stacks.
                                    if (activeArabic.isNotBlank()) {
                                        Text(
                                            text = buildAnnotatedString {
                                                append(activeArabic)
                                                appendAyahMark(displayAyahNo)
                                            },
                                            style = GhaisTypography.quranScript,
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = GhaisNoir.TextPrimary,
                                            // Start, not End: inside the Rtl provider
                                            // above, End resolves to the LEFT edge.
                                            textAlign = TextAlign.Start,
                                            lineHeight = 50.sp,
                                            inlineContent = ayahMarkContent(ringSize = 19.sp, digitSize = 9.sp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // English translation — secondary
                        if (!translationText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = translationText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = GhaisNoir.TextSecondary,
                                lineHeight = 21.sp
                            )
                        }
                    }
            }

            // NEXT preview removed — [upcomingAyahVerse]/[onNextAyah] kept in signature
            // for caller compatibility (NowPlayingScreen) but intentionally not rendered.
            // Record & Compare removed — intentionally not rendered.
            // Tools row (loop/tafseer/learning/download) removed for minimal —
            // [onNextAyah]/[onPreviousAyah]/[onToggleAyahMode]/[onOpenTafseer]
            // kept in signature but intentionally not rendered here.
    }
}


/**
 * Backward-compatible overload for [NowPlayingLyricsCard].
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
    val track = remember(upcomingAyahNumber) {
        TrackItem(
            reciterSlug = "mishari-al-afasy",
            reciterName = "Mishari Rashid Al-Afasy",
            surahId = 55,
            surahNameEn = "Ar-Rahman",
            surahNameAr = "الرحمن",
            ayahNo = (upcomingAyahNumber - 1).coerceAtLeast(1),
            audioUrl = ""
        )
    }
    val currentVerse = remember(arabicVerse, translation, transliteration, upcomingAyahNumber) {
        AyahVerse(
            surahId = 55,
            ayahNo = (upcomingAyahNumber - 1).coerceAtLeast(1),
            textUthmani = arabicVerse,
            translation = translation,
            transliteration = transliteration
        )
    }
    val upcomingVerse = remember(upcomingAyahNumber, upcomingArabic, upcomingTranslation) {
        if (upcomingArabic.isNotBlank()) {
            AyahVerse(
                surahId = 55,
                ayahNo = upcomingAyahNumber,
                textUthmani = upcomingArabic,
                translation = upcomingTranslation
            )
        } else null
    }

    NowPlayingLyricsCard(
        currentTrack = track,
        currentAyahVerse = currentVerse,
        upcomingAyahVerse = upcomingVerse,
        isPlaying = true,
        modifier = modifier
    )
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
