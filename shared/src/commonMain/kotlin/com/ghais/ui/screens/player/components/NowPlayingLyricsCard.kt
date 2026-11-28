package com.ghais.ui.screens.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.MasteryStatus
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.hifz.mutashabihatFor
import com.ghais.hifz.splitAyahByWaqf
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
import com.ghais.ui.theme.GhaisShapes
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
 *   QURAN_LINE_HEIGHT_RATIO line height) + enclosed rosette, translation
 *   secondary (14.5.sp). The verse is the card's only vertically-flexible
 *   child and always claims the full allocation the parent hands down, so long
 *   ayat read without the card reflowing. Scroll is keyed per verse, so each
 *   ayah opens at the top.
 *   No transliteration, no tools row (loop/tafseer/learning/download live in
 *   their own screens), no NEXT preview, no Record & Compare.
 * - Range-loop and recitation-gap banners render only while active.
 * - Translation switcher: a subdued pill (12sp, TextSecondary, GhaisNoir.Fill2
 *   + 1dp BorderCard hairline, GhaisShapes.pill) below the translation naming
 *   the current edition, opening the player's translation picker. Renders
 *   whether or not translation text loaded — see the note at its call site.
 * - Blind self-test (hifz): an eye toggle beside the translation pill conceals
 *   the Arabic verse and the translation behind per-block tap-to-reveal
 *   placeholders plus a "Reveal all" control; while concealed, "Got it" /
 *   "Missed" grade buttons write MASTERED / REVIEW_NEEDED for the current
 *   ayah via HifzMasteryStore. Blind state is remember-only (no Settings).
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
    /**
     * Display name of the translation edition currently in use (e.g.
     * "Saheeh International"). Required — no default, so a new call site cannot
     * forget it and silently render a dead chip.
     */
    translationLabel: String,
    /**
     * Opens the translation picker. Null hides the affordance entirely (used by
     * the backward-compatible overload below, which has no sheet to host).
     * Still required at the call site: null is an explicit decision, not a
     * forgotten one.
     */
    onOpenTranslationPicker: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /**
     * Starting value for blind self-test mode. Optional with a default so
     * existing call sites are untouched; the live toggle is remember-only and
     * never persisted to Settings.
     */
    blindInitial: Boolean = false
) {
    val isGapActive by AudioEngine.isRecitationGapActive.collectAsState()
    val gapCountdown by AudioEngine.recitationGapCountdown.collectAsState()
    val hifzRange by AudioEngine.hifzRange.collectAsState()
    val waqfLoopState by AudioEngine.waqfLoopState.collectAsState()
    val waqfRepeatCount by AudioEngine.waqfRepeatTarget.collectAsState()
    val loadingSurahs by QuranAyahRepository.loadingSurahs.collectAsState()

    // Blind self-test preference: in-memory only (remember), shared across
    // verse changes so reciting through a range stays concealed. Per-block
    // reveal state lives inside the keyed verse block below and resets per ayah.
    var blindOn by remember { mutableStateOf(blindInitial) }
    val masteryMap by HifzMasteryStore.masteryMap.collectAsState()

    // Persisted Quran text size in the player (22.sp..42.sp, default 27.sp).
    val playerSettings = remember { com.russhwolf.settings.Settings() }
    var arabicFontSp by remember {
        mutableStateOf(playerSettings.getInt("ghais_player_quran_font_sp", 27).coerceIn(22, 42))
    }
    val translationFontSp = remember(arabicFontSp) {
        (14f + (arabicFontSp - 27) * 0.28f).coerceIn(12.5f, 19f)
    }

    val displayAyahNo = currentTrack?.ayahNo?.takeIf { it > 0 }
        ?: currentAyahVerse?.ayahNo
        ?: 1

    val surahId = currentTrack?.surahId ?: currentAyahVerse?.surahId ?: 1

    val surahName = currentTrack?.surahNameEn?.takeIf { it.isNotBlank() }
        ?: currentAyahVerse?.surahId?.let { "Surah $it" }
        ?: ""

    val isCurrentVerseUnavailable = currentAyahVerse == null ||
        currentAyahVerse.isPlaceholder ||
        currentAyahVerse.textUthmani.isBlank()

    LaunchedEffect(surahId, isCurrentVerseUnavailable) {
        if (isCurrentVerseUnavailable && surahId !in loadingSurahs) {
            QuranAyahRepository.getAyahsForSurah(surahId)
        }
    }

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

    // MINIMAL Quran-first Ayah mode: transparent surface, centered sacred typography,
    // single clean header & quiet study footer.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
            // Single clean header: (X) + Surah · Ayah n + live dot  |  [Range pill] + [Hide/Peek]
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

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) GhaisNoir.Lavender else GhaisNoir.TextTertiary)
                    )

                    val refText = if (surahName.isNotBlank()) "$surahName · $displayAyahNo" else "Ayah $displayAyahNo"
                    Text(
                        text = refText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hifzRange != null) {
                        val range = hifzRange!!
                        val loopStr = if (range.targetLoops == -1) "${range.currentLoop}/∞"
                        else "${range.currentLoop}/${range.targetLoops}"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Lavender.copy(alpha = 0.14f))
                                .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.30f), GhaisShapes.pill)
                                .noirClickable { AudioEngine.clearHifzRange() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${range.startAyah}–${range.endAyah} · $loopStr",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.Lavender,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Range Loop",
                                tint = GhaisNoir.Lavender,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    if (!isCurrentVerseUnavailable) {
                        BlindedRecallPill(
                            blindOn = blindOn,
                            onToggle = { blindOn = !blindOn }
                        )
                    }
                }
            }

            if (isGapActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Lavender.copy(alpha = 0.12f))
                        .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.28f), GhaisShapes.pill)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recite aloud • ${gapCountdown}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.Lavender,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Skip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary,
                        modifier = Modifier.noirClickable { AudioEngine.skipRecitationGap() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GhaisNoir.BorderGhost)
            )
            Spacer(modifier = Modifier.height(10.dp))

            key(surahId, displayAyahNo) {
            var arabicRevealed by remember { mutableStateOf(false) }
            var translationRevealed by remember { mutableStateOf(false) }
            var selectedPhraseIndex by remember { mutableStateOf<Int?>(null) }
            val waqfPhrases = remember(activeArabic) { splitAyahByWaqf(activeArabic) }
            val mutashabihat = remember(surahId, displayAyahNo) { mutashabihatFor(surahId, displayAyahNo) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
            ) {
                    val showBasmalahHeader =
                        displayAyahNo == 1 && QuranAyahRepository.hasBasmalahHeader(surahId)
                    if (isCurrentVerseUnavailable) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (surahId in loadingSurahs) "Loading ayah…" else "Ayah unavailable offline · Tap to retry",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = GhaisNoir.TextTertiary,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp,
                            modifier = Modifier.noirClickable {
                                QuranAyahRepository.prefetchSurah(surahId)
                            }
                        )
                    } else {
                        if (mutashabihat.isNotEmpty()) {
                            val firstMatch = mutashabihat.first()
                            val otherSurahName = QuranDataRepository.getSurahById(firstMatch.otherSurahId)?.nameEn
                            val refLabel = if (!otherSurahName.isNullOrBlank()) {
                                "$otherSurahName ${firstMatch.otherSurahId}:${firstMatch.otherAyahNo}"
                            } else {
                                "${firstMatch.otherSurahId}:${firstMatch.otherAyahNo}"
                            }
                            val extraSuffix = if (mutashabihat.size > 1) " (+${mutashabihat.size - 1})" else ""
                            SimilarVerseBadge(
                                text = "Similar verse · $refLabel$extraSuffix",
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        val waqfPhrases = remember(activeArabic) { splitAyahByWaqf(activeArabic) }
                        LaunchedEffect(surahId, displayAyahNo, waqfPhrases, waqfRepeatCount) {
                            if (waqfRepeatCount > 1 && waqfPhrases.size > 1) {
                                val weights = waqfPhrases.map { it.length.coerceAtLeast(1) }
                                val existing = AudioEngine.waqfLoopState.value
                                if (existing == null ||
                                    existing.phraseWeights != weights ||
                                    existing.repeatPerWaqf != waqfRepeatCount
                                ) {
                                    AudioEngine.setWaqfLoop(
                                        phraseWeights = weights,
                                        repeatPerWaqf = waqfRepeatCount,
                                    )
                                }
                            } else {
                                AudioEngine.clearWaqfLoop()
                            }
                        }
                        if (waqfPhrases.size > 1 && !(blindOn && !arabicRevealed)) {
                            WaqfPhraseChipsRow(
                                phraseCount = waqfPhrases.size,
                                selectedIndex = selectedPhraseIndex,
                                playingPhraseIndex = waqfLoopState?.activePhraseIndex,
                                currentRep = waqfLoopState?.currentRep ?: 1,
                                repeatPerWaqf = waqfRepeatCount,
                                onSelectPhrase = { idx ->
                                    selectedPhraseIndex = if (selectedPhraseIndex == idx) null else idx
                                    if (waqfRepeatCount > 1) {
                                        AudioEngine.jumpToWaqfPhrase(idx)
                                    }
                                },
                                onCycleWaqfRepeat = {
                                    val next = when (waqfRepeatCount) {
                                        1 -> 2
                                        2 -> 3
                                        3 -> 5
                                        else -> 1
                                    }
                                    AudioEngine.setWaqfRepeatCount(next)
                                },
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        val displayedArabic = selectedPhraseIndex
                            ?.takeIf { it in waqfPhrases.indices }
                            ?.let { waqfPhrases[it] }
                            ?: activeArabic
                        val showAyahMark = selectedPhraseIndex == null || selectedPhraseIndex == waqfPhrases.lastIndex

                        // Centered Sacred Quran Block (Bismillah + Verse)
                        if (showBasmalahHeader || activeArabic.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (showBasmalahHeader) {
                                        Text(
                                            text = QuranAyahRepository.BASMALAH,
                                            style = GhaisTypography.quranScript,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = GhaisNoir.Lavender.copy(alpha = 0.88f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 40.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .height(1.dp)
                                                .background(GhaisNoir.Lavender.copy(alpha = 0.30f))
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    if (activeArabic.isNotBlank()) {
                                        if (blindOn && !arabicRevealed) {
                                            ConcealedVerseBlock(
                                                label = "Arabic concealed — tap to peek",
                                                onReveal = { arabicRevealed = true }
                                            )
                                        } else {
                                            Text(
                                                text = buildAnnotatedString {
                                                    append(displayedArabic)
                                                    if (showAyahMark) {
                                                        appendAyahMark(displayAyahNo)
                                                    }
                                                },
                                                style = GhaisTypography.quranScript,
                                                fontSize = arabicFontSp.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = GhaisNoir.TextPrimary,
                                                textAlign = TextAlign.Center,
                                                lineHeight = (arabicFontSp * GhaisTypography.QURAN_LINE_HEIGHT_RATIO).sp,
                                                inlineContent = ayahMarkContent(
                                                    ringSize = (arabicFontSp * 0.74f).sp,
                                                    digitSize = (arabicFontSp * 0.35f).sp
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Centered Translation
                        if (!translationText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            if (blindOn && !translationRevealed) {
                                ConcealedVerseBlock(
                                    label = "Meaning concealed — tap to reveal",
                                    onReveal = { translationRevealed = true }
                                )
                            } else {
                                Text(
                                    text = translationText,
                                    fontSize = translationFontSp.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = GhaisNoir.TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (translationFontSp * 1.55f).sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        // Blind self-test footer
                        val arabicStillHidden = blindOn && activeArabic.isNotBlank() && !arabicRevealed
                        val translationStillHidden = blindOn && !translationText.isNullOrBlank() && !translationRevealed
                        if (blindOn && (arabicStillHidden || translationStillHidden)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Reveal all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GhaisNoir.Lavender,
                                modifier = Modifier.noirClickable {
                                    arabicRevealed = true
                                    translationRevealed = true
                                }
                            )
                        }
                        if (blindOn && !isCurrentVerseUnavailable) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val currentStatus = masteryMap[surahId to displayAyahNo]
                                ?: MasteryStatus.NEW
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SelfGradeButton(
                                    text = "Got it",
                                    active = currentStatus == MasteryStatus.MASTERED,
                                    onGrade = {
                                        HifzMasteryStore.setStatus(
                                            surahId,
                                            displayAyahNo,
                                            MasteryStatus.MASTERED
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SelfGradeButton(
                                    text = "Missed",
                                    active = currentStatus == MasteryStatus.REVIEW_NEEDED,
                                    onGrade = {
                                        HifzMasteryStore.setStatus(
                                            surahId,
                                            displayAyahNo,
                                            MasteryStatus.REVIEW_NEEDED
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Minimal centered study footer: borderless whisper-quiet links
                    val openTranslationPicker = onOpenTranslationPicker
                    val openTafseer = onOpenTafseer
                    val showTafseer = openTafseer != null && !isCurrentVerseUnavailable
                    if (openTranslationPicker != null || showTafseer) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (openTranslationPicker != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .widthIn(max = 200.dp)
                                        .clip(GhaisShapes.pill)
                                        .noirClickable { openTranslationPicker() }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = translationLabel.ifBlank { "Translation" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GhaisNoir.TextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Change translation",
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            if (openTranslationPicker != null && showTafseer) {
                                Text(
                                    text = "·",
                                    fontSize = 11.sp,
                                    color = GhaisNoir.TextDisabled
                                )
                            }
                            if (showTafseer) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(GhaisShapes.pill)
                                        .noirClickable { openTafseer!!.invoke() }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tafseer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GhaisNoir.TextTertiary,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "·",
                                    fontSize = 11.sp,
                                    color = GhaisNoir.TextDisabled
                                )
                            } else if (openTranslationPicker != null) {
                                Text(
                                    text = "·",
                                    fontSize = 11.sp,
                                    color = GhaisNoir.TextDisabled
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "A−",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (arabicFontSp > 22) GhaisNoir.TextTertiary else GhaisNoir.TextDisabled,
                                    modifier = Modifier
                                        .clip(GhaisShapes.pill)
                                        .then(
                                            if (arabicFontSp > 22) Modifier.noirClickable {
                                                val next = (arabicFontSp - 3).coerceAtLeast(22)
                                                arabicFontSp = next
                                                playerSettings.putInt("ghais_player_quran_font_sp", next)
                                            } else Modifier
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                                Text(
                                    text = "A+",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (arabicFontSp < 42) GhaisNoir.TextSecondary else GhaisNoir.TextDisabled,
                                    modifier = Modifier
                                        .clip(GhaisShapes.pill)
                                        .then(
                                            if (arabicFontSp < 42) Modifier.noirClickable {
                                                val next = (arabicFontSp + 3).coerceAtMost(42)
                                                arabicFontSp = next
                                                playerSettings.putInt("ghais_player_quran_font_sp", next)
                                            } else Modifier
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
            }
            }
    }
}

/**
 * Eyes-Free / Blinded Recall toggle pill ("Hide Text" / "Tap to Peek")
 * in Noir Glass pill styling.
 */
@Composable
private fun BlindedRecallPill(
    blindOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(if (blindOn) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(
                1.dp,
                if (blindOn) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.pill
            )
            .noirClickable { onToggle() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (blindOn) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (blindOn) "Tap to Peek" else "Hide Text",
            tint = if (blindOn) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = if (blindOn) "Tap to Peek" else "Hide Text",
            fontSize = 11.sp,
            fontWeight = if (blindOn) FontWeight.SemiBold else FontWeight.Medium,
            color = if (blindOn) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            maxLines = 1
        )
    }
}

/**
 * Subtle Mutashabihat badge showing a similar verse reference.
 */
@Composable
private fun SimilarVerseBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GhaisNoir.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact phrase indicator chips for long ayahs split by Quranic waqf marks.
 */
@Composable
private fun WaqfPhraseChipsRow(
    phraseCount: Int,
    selectedIndex: Int?,
    playingPhraseIndex: Int?,
    currentRep: Int,
    repeatPerWaqf: Int,
    onSelectPhrase: (Int) -> Unit,
    onCycleWaqfRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = "Waqf",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = GhaisNoir.TextTertiary,
            letterSpacing = 0.5.sp
        )
        for (idx in 0 until phraseCount) {
            val active = selectedIndex == idx
            val isRepeatingThis = repeatPerWaqf > 1 && playingPhraseIndex == idx
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .then(
                        when {
                            isRepeatingThis -> Modifier.background(GhaisNoir.lavenderFill())
                            active -> Modifier
                                .background(GhaisNoir.Fill4)
                                .border(1.dp, GhaisNoir.SpecularTop, GhaisShapes.pill)
                            else -> Modifier
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        }
                    )
                    .noirClickable { onSelectPhrase(idx) }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${idx + 1}",
                    fontSize = 11.sp,
                    fontWeight = if (active || isRepeatingThis) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isRepeatingThis -> GhaisNoir.LavenderInk
                        active -> GhaisNoir.TextPrimary
                        else -> GhaisNoir.TextSecondary
                    }
                )
            }
        }
        // Repeat-each-Waqf pill: cycles 1× (off) -> 2× -> 3× -> 5× -> 1×
        val repActive = repeatPerWaqf > 1
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(if (repActive) GhaisNoir.Lavender.copy(alpha = 0.16f) else GhaisNoir.Fill1)
                .border(
                    1.dp,
                    if (repActive) GhaisNoir.Lavender.copy(alpha = 0.40f) else GhaisNoir.BorderGhost,
                    GhaisShapes.pill
                )
                .noirClickable { onCycleWaqfRepeat() }
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RepeatOne,
                contentDescription = "Repeat each waqf",
                tint = if (repActive) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (repActive) "$currentRep/$repeatPerWaqf×" else "1×",
                fontSize = 10.5.sp,
                fontWeight = if (repActive) FontWeight.Bold else FontWeight.Medium,
                color = if (repActive) GhaisNoir.Lavender else GhaisNoir.TextTertiary
            )
        }
    }
}

/**
 * Placeholder for one concealed verse block in blind self-test mode.
 * Monochrome ghost pill; tapping reveals just that block.
 */
@Composable
private fun ConcealedVerseBlock(
    label: String,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable { onReveal() }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GhaisNoir.TextTertiary
        )
    }
}

/**
 * One half of the blind self-grade row ("Got it" / "Missed"). Ghost pill
 * that reads active (Fill4 + specular border + primary text) while the
 * ayah's stored status matches the grade it writes.
 */
@Composable
private fun SelfGradeButton(
    text: String,
    active: Boolean,
    onGrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(if (active) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(
                1.dp,
                if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.pill
            )
            .noirClickable { onGrade() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary
        )
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
        // This overload is a static sample with no sheet host, so the chip is
        // passed as null (no dead affordance) rather than a no-op lambda.
        translationLabel = "Saheeh International",
        onOpenTranslationPicker = null,
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
                    fontSize = 11.sp,
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
