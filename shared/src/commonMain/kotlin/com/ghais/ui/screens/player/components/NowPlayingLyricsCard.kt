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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.MasteryStatus
import com.ghais.player.AudioEngine
import com.ghais.player.DownloadKeys
import com.ghais.player.QuranDownloads
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
import com.ghais.data.repository.AyahVerse
import com.ghais.domain.model.TrackItem
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.AyahEndMark

/**
 * Synchronized Ayah Lyrics Card — MINIMAL (monochrome, zero hue).
 *
 * - Transparent surface: no card fills, borders, sheens, or engraved insets.
 *   Hairline dividers + spacing only.
 * - Header: single slim row — X ghost well + "AYAH {ayahNo}" text +
 *   sync dot + Surah name. No pills.
 * - Tools: tiny ghost icon wells in one row (Loop / Tafseer / Learning),
 *   active states via fill + specular only.
 * - Active ayah: prominent Arabic Uthmani in quranFont (25.sp, RTL,
 *   2.0x line height) + enclosed rosette, transliteration tertiary italic,
 *   translation secondary (14.5.sp).
 * - NEXT preview removed; Record & Compare removed (params kept for compatibility).
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
    val repTarget by AudioEngine.ayahRepetitionTarget.collectAsState()
    val currentRep by AudioEngine.currentAyahRepetition.collectAsState()
    val isGapActive by AudioEngine.isRecitationGapActive.collectAsState()
    val gapCountdown by AudioEngine.recitationGapCountdown.collectAsState()
    val hifzRange by AudioEngine.hifzRange.collectAsState()

    val displayAyahNo = currentTrack?.ayahNo?.takeIf { it > 0 }
        ?: currentAyahVerse?.ayahNo
        ?: 1

    val surahId = currentTrack?.surahId ?: currentAyahVerse?.surahId ?: 1
    val masteryMap by HifzMasteryStore.masteryMap.collectAsState()
    val currentStatus = masteryMap[surahId to displayAyahNo] ?: HifzMasteryStore.getStatus(surahId, displayAyahNo)

    val surahName = currentTrack?.surahNameEn?.takeIf { it.isNotBlank() }
        ?: currentAyahVerse?.surahId?.let { "Surah $it" }
        ?: ""

    val activeArabic = currentAyahVerse?.textUthmani?.takeIf { it.isNotBlank() }
        ?: currentTrack?.textUthmani?.takeIf { it.isNotBlank() }
        ?: ""

    val transliterationText = currentAyahVerse?.transliteration?.takeIf { it.isNotBlank() }
    val translationText = currentAyahVerse?.translation?.takeIf { it.isNotBlank() }

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

                if (onPreviousAyah != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .noirClickable { onPreviousAyah() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Ayah",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Tools: tiny ghost icon wells in one row — Loop / Tafseer / Learning.
            // Active states via fill + specular only. Zero hue.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MinimalLoopWell(
                    repetitionTarget = repTarget,
                    currentRepetition = currentRep,
                    onTargetSelected = { AudioEngine.setAyahRepetitionTarget(it) }
                )

                if (onOpenTafseer != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                            .noirClickable { onOpenTafseer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Tafseer",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                MinimalMasteryWell(
                    status = currentStatus,
                    onCycle = {
                        HifzMasteryStore.cycleStatus(surahId, displayAyahNo)
                        HifzMasteryStore.recordAyahReviewed(surahId, displayAyahNo)
                    }
                )

                // Per-ayah offline toggle — hidden when the card has no
                // reciter slug / stream URL to download from.
                val ayahSlug = currentTrack?.reciterSlug?.takeIf { it.isNotBlank() }
                val ayahAudioUrl = currentTrack?.audioUrl?.takeIf { it.isNotBlank() }
                if (ayahSlug != null && ayahAudioUrl != null) {
                    MinimalAyahDownloadWell(
                        slug = ayahSlug,
                        surahId = surahId,
                        ayahNo = displayAyahNo,
                        url = ayahAudioUrl
                    )
                }

                if (repTarget != 1) {
                    val loopLabel = if (repTarget == -1) "∞ ($currentRep)"
                    else "${repTarget}x ($currentRep/$repTarget)"
                    Text(
                        text = loopLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextTertiary
                    )
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
            // Arabic Uthmani in quranFont (25.sp, RTL) + enclosed rosette, 2.0x line height.
            Column(modifier = Modifier.fillMaxWidth()) {
                    // Arabic verse — QCF Hafs Uthmani text (100% white, RTL)
                    // with the enclosed end-of-ayah rosette. Normal weight:
                    // synthetic bold perturbs mark shaping on dense stacks.
                    if (activeArabic.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            AyahEndMark(
                                number = displayAyahNo,
                                ornamentSize = 26.sp,
                                digitSize = 11.sp,
                                tint = GhaisNoir.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeArabic,
                                fontFamily = GhaisTypography.quranFont,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Normal,
                                color = GhaisNoir.TextPrimary,
                                textAlign = TextAlign.End,
                                lineHeight = 50.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Phonetic transliteration — tertiary italic
                    if (!transliterationText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = transliterationText,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = GhaisNoir.TextTertiary,
                            lineHeight = 18.sp
                        )
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

            // NEXT preview removed — [upcomingAyahVerse]/[onNextAyah] kept in signature
            // for caller compatibility (NowPlayingScreen) but intentionally not rendered.
            // Record & Compare removed — intentionally not rendered.
    }
}

/**
 * Loop ghost well — preserves [AudioEngine] repetition target flow.
 * Active via stronger fill + specular border. Dropdown unchanged.
 */
@Composable
private fun MinimalLoopWell(
    repetitionTarget: Int,
    currentRepetition: Int,
    onTargetSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = repetitionTarget != 1
    Box {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Brush.verticalGradient(
                        listOf(GhaisNoir.Fill4, GhaisNoir.FillDeep)
                    ) else GhaisNoir.wellFill()
                )
                .border(
                    1.dp,
                    if (isActive) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                    CircleShape
                )
                .noirClickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (repetitionTarget == 1) Icons.Default.Repeat else Icons.Default.RepeatOne,
                contentDescription = "Ayah Repetition: $repetitionTarget",
                tint = if (isActive) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(15.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(GhaisNoir.CanvasTop)
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(12.dp))
        ) {
            listOf(1, 3, 5, 7, 10, -1).forEach { target ->
                val label = if (target == -1) "Continuous (∞)" else "$target times (${target}x)"
                val selected = repetitionTarget == target
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        onTargetSelected(target)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Learning ghost well — preserves [HifzMasteryStore] cycle flow.
 * NEW dim → REVIEW specular → MASTERED bright. Zero hue, no emoji.
 */
@Composable
private fun MinimalMasteryWell(
    status: MasteryStatus,
    onCycle: () -> Unit
) {
    val (fill, border, tint) = when (status) {
        MasteryStatus.NEW -> Triple(GhaisNoir.wellFill(), GhaisNoir.BorderCard, GhaisNoir.TextTertiary)
        MasteryStatus.REVIEW_NEEDED -> Triple(
            GhaisNoir.wellFill(),
            GhaisNoir.SpecularTop.copy(alpha = 0.5f),
            GhaisNoir.TextSecondary
        )
        MasteryStatus.MASTERED -> Triple(
            Brush.verticalGradient(listOf(GhaisNoir.Fill4, GhaisNoir.FillDeep)),
            GhaisNoir.TextPrimary.copy(alpha = 0.7f),
            GhaisNoir.TextPrimary
        )
    }
    val description = when (status) {
        MasteryStatus.NEW -> "Learning"
        MasteryStatus.REVIEW_NEEDED -> "Needs review"
        MasteryStatus.MASTERED -> "Mastered"
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, border, CircleShape)
            .noirClickable { onCycle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "Mastery: $description",
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * Per-ayah download ghost well — offline toggle for the CURRENT ayah.
 *
 * States: idle (Download) → downloading (progress ring, taps ignored) →
 * downloaded (Done, tap removes) / failed (Retry, tap retries).
 * Wired to [QuranDownloads.downloadAyah]/[isAyahDownloaded]/[deleteAyah];
 * in-flight/failed membership is read from the shared [QuranDownloads]
 * flows using the per-ayah [DownloadKeys.ayahKey].
 */
@Composable
private fun MinimalAyahDownloadWell(
    slug: String,
    surahId: Int,
    ayahNo: Int,
    url: String
) {
    val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
    val dlProgress by QuranDownloads.progress.collectAsState()
    val failedKeys by QuranDownloads.failedKeys.collectAsState()
    val ayahKey = DownloadKeys.ayahKey(slug, surahId, ayahNo)
    // Strict precedence: downloaded > downloading > failed > idle —
    // a completed key never renders a stale progress/failed affordance.
    val isDownloaded = ayahKey in downloadedKeys ||
        QuranDownloads.isAyahDownloaded(slug, surahId, ayahNo)
    val pending: Float? = if (isDownloaded) null else dlProgress[ayahKey]
    val isDownloading = pending != null
    val isFailed = !isDownloaded && !isDownloading && ayahKey in failedKeys

    val border = when {
        isDownloaded -> GhaisNoir.TextPrimary.copy(alpha = 0.7f)
        else -> GhaisNoir.BorderCard
    }
    val tint = when {
        isDownloaded -> GhaisNoir.TextPrimary
        isFailed -> GhaisNoir.TextSecondary
        else -> GhaisNoir.TextTertiary
    }
    val description = when {
        isDownloaded -> "Ayah downloaded — tap to remove"
        isDownloading -> "Downloading ayah"
        isFailed -> "Ayah download failed — tap to retry"
        else -> "Download ayah"
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isDownloaded) Brush.verticalGradient(
                    listOf(GhaisNoir.Fill4, GhaisNoir.FillDeep)
                ) else GhaisNoir.wellFill()
            )
            .border(1.dp, border, CircleShape)
            .noirClickable {
                when {
                    isDownloaded -> QuranDownloads.deleteAyah(slug, surahId, ayahNo)
                    isDownloading -> Unit // in-flight: ignore double-taps
                    else -> QuranDownloads.downloadAyah(slug, surahId, ayahNo, url)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isDownloading -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = GhaisNoir.TextPrimary,
                strokeWidth = 2.dp,
                trackColor = GhaisNoir.Fill2
            )
            isDownloaded -> Icon(
                imageVector = Icons.Default.Done,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            isFailed -> Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            else -> Icon(
                imageVector = Icons.Default.Download,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
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
