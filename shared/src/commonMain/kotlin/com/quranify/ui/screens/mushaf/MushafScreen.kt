package com.quranify.ui.screens.mushaf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data representation for an Ayah entry in the Mushaf screen,
 * supporting Tajweed segments, translations, audio, and Tafsir excerpts.
 */
data class MushafAyahData(
    val ayahNumber: Int,
    val textUthmani: String,
    val tajweedPart: String? = null,
    val tajweedPrefix: Boolean = false,
    val translation: String,
    val footnote: String? = null,
    val audioUrl: String,
    val durationMs: Long,
    val tafsirSnippet: String
)

object MushafScreen : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Filled.AutoStories)
            return remember {
                TabOptions(
                    index = 3u,
                    title = "Mushaf",
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        var isTajweedActive by remember { mutableStateOf(true) }
        var showTranslation by remember { mutableStateOf(true) }
        var arabicFontSize by remember { mutableStateOf(28) }
        var playingAyah by remember { mutableStateOf(1) }
        var bookmarkedAyahs by remember { mutableStateOf(setOf(1)) }
        var repeatStateIdx by remember { mutableStateOf(1) } // 0: 1x, 1: 3x Loop, 2: ∞ Hifz, 3: Loop Off
        var isPageViewMode by remember { mutableStateOf(false) }
        var activeTafsirAyah by remember { mutableStateOf<MushafAyahData?>(null) }
        var isSurahMenuOpen by remember { mutableStateOf(false) }
        var selectedSurahName by remember { mutableStateOf("Al-Kahf (18)") }

        val isAudioPlaying by AudioEngine.isPlaying.collectAsState()
        val currentTrack by AudioEngine.currentTrack.collectAsState()

        val repeatStates = listOf("1x Loop", "3x Loop", "∞ Hifz", "Loop Off")

        val ayahs = remember {
            listOf(
                MushafAyahData(
                    ayahNumber = 1,
                    textUthmani = "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُۥ",
                    tajweedPart = " عِوَجَاۜ",
                    tajweedPrefix = false,
                    translation = "[All] praise is [due] to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.",
                    footnote = "Sahih International • Footnote: i.e., crookedness, corruption or inaccuracy",
                    audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018001.mp3",
                    durationMs = 15000L,
                    tafsirSnippet = "Praise be to Allah, Who has sent down to His servant Muhammad the Book (the Qur'an) without any crookedness or ambiguity, upright in its guidance."
                ),
                MushafAyahData(
                    ayahNumber = 2,
                    textUthmani = " لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا",
                    tajweedPart = "قَيِّمًا",
                    tajweedPrefix = true,
                    translation = "[He has made it] straight, to warn of severe punishment from Him and to give good tidings to the believers who do righteous deeds that they will have a good reward.",
                    footnote = "Sahih International",
                    audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018002.mp3",
                    durationMs = 20000L,
                    tafsirSnippet = "Upright and balanced, to warn the disbelievers of severe punishment directly from Allah and to give glad tidings to believers who perform good deeds."
                ),
                MushafAyahData(
                    ayahNumber = 3,
                    textUthmani = "مَّاكِثِينَ فِيهِ أَبَدًا",
                    tajweedPart = null,
                    tajweedPrefix = false,
                    translation = "In which they will remain forever.",
                    footnote = "Sahih International",
                    audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018003.mp3",
                    durationMs = 8000L,
                    tafsirSnippet = "Abiding eternally in the gardens of bliss, never desiring any transfer or departure from it."
                ),
                MushafAyahData(
                    ayahNumber = 4,
                    textUthmani = "وَيُنذِرَ الَّذِينَ قَالُوا اتَّخَذَ اللَّهُ وَلَدًا",
                    tajweedPart = null,
                    tajweedPrefix = false,
                    translation = "And to warn those who say, 'Allah has taken a son.'",
                    footnote = "Sahih International",
                    audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018004.mp3",
                    durationMs = 11000L,
                    tafsirSnippet = "A targeted warning to those among polytheists and groups who blasphemously claim that the Almighty has offspring."
                ),
                MushafAyahData(
                    ayahNumber = 5,
                    textUthmani = "مَّا لَهُم بِهِ مِنْ عِلْمٍ وَلَا لِآبَائِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَاهِهِمْ ۚ",
                    tajweedPart = " إِن يَقُولُونَ إِلَّا كَذِبًا",
                    tajweedPrefix = false,
                    translation = "They have no knowledge of it, nor had their fathers. Grave is the word that comes out of their mouths; they speak not except a lie.",
                    footnote = "Sahih International",
                    audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018005.mp3",
                    durationMs = 18000L,
                    tafsirSnippet = "They and their ancestors speak without proof or knowledge; monstrous is the word that leaves their mouths."
                )
            )
        }

        // Function to start audio for a specific Ayah
        val playAyahAudio: (MushafAyahData) -> Unit = { ayah ->
            playingAyah = ayah.ayahNumber
            AudioEngine.playTrack(
                TrackItem(
                    reciterSlug = "mishary",
                    reciterName = "Sheikh Mishary Rashid Alafasy",
                    surahId = 18,
                    surahNameEn = "Al-Kahf",
                    surahNameAr = "الكهف",
                    ayahNo = ayah.ayahNumber,
                    audioUrl = ayah.audioUrl,
                    textUthmani = ayah.textUthmani,
                    durationMs = ayah.durationMs
                )
            )
        }

        val toggleGlobalAudio: () -> Unit = {
            if (isAudioPlaying) {
                AudioEngine.pause()
            } else {
                val targetAyah = ayahs.find { it.ayahNumber == playingAyah } ?: ayahs.first()
                playAyahAudio(targetAyah)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QuranifyColors.Background),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp)
            ) {
                // 1. Top Floating Mushaf Control Capsule
                item {
                    MushafControlCapsule(
                        selectedSurah = selectedSurahName,
                        isSurahMenuOpen = isSurahMenuOpen,
                        onToggleSurahMenu = { isSurahMenuOpen = !isSurahMenuOpen },
                        onSelectSurah = { surah ->
                            selectedSurahName = surah
                            isSurahMenuOpen = false
                        },
                        arabicFontSize = arabicFontSize,
                        onCycleFontSize = {
                            arabicFontSize = when (arabicFontSize) {
                                28 -> 32
                                32 -> 36
                                else -> 28
                            }
                        },
                        showTranslation = showTranslation,
                        onToggleTranslation = { showTranslation = !showTranslation },
                        isTajweed = isTajweedActive,
                        onToggleTajweed = { isTajweedActive = !isTajweedActive },
                        isAudioPlaying = isAudioPlaying,
                        onToggleAudio = toggleGlobalAudio
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Surah Ornamentation Header Banner
                item {
                    SurahHeaderBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. Ayah Reading Stream OR Classic Mushaf Page View
                if (isPageViewMode) {
                    item {
                        MushafPageViewCard(
                            ayahs = ayahs,
                            activeAyah = playingAyah,
                            arabicFontSize = arabicFontSize,
                            isTajweed = isTajweedActive,
                            onSelectAyah = { ayah -> playAyahAudio(ayah) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    items(ayahs.size) { index ->
                        val ayah = ayahs[index]
                        val isActive = playingAyah == ayah.ayahNumber
                        val isBookmarked = bookmarkedAyahs.contains(ayah.ayahNumber)

                        AyahBlock(
                            ayah = ayah,
                            isActive = isActive,
                            isBookmarked = isBookmarked,
                            isTajweed = isTajweedActive,
                            showTranslation = showTranslation,
                            arabicFontSize = arabicFontSize,
                            onCardClick = { playingAyah = ayah.ayahNumber },
                            onPlayClick = {
                                if (isActive && isAudioPlaying) {
                                    AudioEngine.pause()
                                } else {
                                    playAyahAudio(ayah)
                                }
                            },
                            onBookmarkClick = {
                                bookmarkedAyahs = if (isBookmarked) {
                                    bookmarkedAyahs - ayah.ayahNumber
                                } else {
                                    bookmarkedAyahs + ayah.ayahNumber
                                }
                            },
                            onTafsirClick = { activeTafsirAyah = ayah },
                            onShareClick = { /* Share functionality */ }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 4. Reading Mode Bottom Utilities Capsule (Apple-style Quick Tools)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    MushafBottomToolsCapsule(
                        repeatLabel = repeatStates[repeatStateIdx],
                        onCycleRepeat = {
                            repeatStateIdx = (repeatStateIdx + 1) % repeatStates.size
                        },
                        onOpenTafsir = {
                            activeTafsirAyah = ayahs.find { it.ayahNumber == playingAyah } ?: ayahs.first()
                        },
                        isPageView = isPageViewMode,
                        onTogglePageView = { isPageViewMode = !isPageViewMode }
                    )
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }

            // Tafsir Modal Sheet Dialog
            if (activeTafsirAyah != null) {
                TafsirDialog(
                    ayah = activeTafsirAyah!!,
                    onDismiss = { activeTafsirAyah = null }
                )
            }
        }
    }
}

/**
 * Top Floating Mushaf Control Capsule (Apple glassmorphism)
 * Controls: Surah Dropdown, Juz/Page indicator, Font Size cycle,
 * Translation toggle, Tajweed mode toggle, and Recitation Audio toggle.
 */
@Composable
private fun MushafControlCapsule(
    selectedSurah: String,
    isSurahMenuOpen: Boolean,
    onToggleSurahMenu: () -> Unit,
    onSelectSurah: (String) -> Unit,
    arabicFontSize: Int,
    onCycleFontSize: () -> Unit,
    showTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isTajweed: Boolean,
    onToggleTajweed: () -> Unit,
    isAudioPlaying: Boolean,
    onToggleAudio: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.92f))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        QuranifyColors.OutlineVariant.copy(alpha = 0.6f),
                        QuranifyColors.Primary.copy(alpha = 0.2f),
                        QuranifyColors.OutlineVariant.copy(alpha = 0.6f)
                    )
                ),
                RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah Selector Capsule
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(QuranifyColors.SurfaceContainer)
                        .clickable { onToggleSurahMenu() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Surah Selector",
                        tint = QuranifyColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedSurah,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuranifyColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Dropdown",
                        tint = QuranifyColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isSurahMenuOpen,
                    onDismissRequest = onToggleSurahMenu,
                    modifier = Modifier.background(QuranifyColors.SurfaceHigh)
                ) {
                    val surahList = listOf(
                        "Al-Fatihah (1)",
                        "Al-Baqarah (2)",
                        "Al-Kahf (18)",
                        "Ya-Sin (36)",
                        "Ar-Rahman (55)",
                        "Al-Mulk (67)"
                    )
                    surahList.forEach { surah ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = surah,
                                    color = if (surah == selectedSurah) QuranifyColors.Primary else QuranifyColors.TextPrimary,
                                    fontWeight = if (surah == selectedSurah) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { onSelectSurah(surah) }
                        )
                    }
                }
            }

            // Juz / Page Indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(QuranifyColors.Background.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "JUZ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuranifyColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "15",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )
                    Text(
                        text = " • ",
                        fontSize = 10.sp,
                        color = QuranifyColors.Primary
                    )
                    Text(
                        text = "P.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuranifyColors.TextSecondary
                    )
                    Text(
                        text = "293",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )
                }
            }

            // Quick Preferences Pill Group (Font Size, Translation, Tajweed, Audio)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Font Size Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(QuranifyColors.SurfaceContainer)
                        .clickable { onCycleFontSize() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font size: $arabicFontSize",
                            tint = QuranifyColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${arabicFontSize}pt",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuranifyColors.TextSecondary,
                            lineHeight = 9.sp
                        )
                    }
                }

                // 2. Translation Toggle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (showTranslation) QuranifyColors.Primary.copy(alpha = 0.18f)
                            else QuranifyColors.SurfaceContainer
                        )
                        .clickable { onToggleTranslation() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Toggle Translation",
                        tint = if (showTranslation) QuranifyColors.Primary else QuranifyColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 3. Tajweed Toggle Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isTajweed) QuranifyColors.Primary.copy(alpha = 0.18f)
                            else QuranifyColors.SurfaceContainer
                        )
                        .clickable { onToggleTajweed() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Tajweed",
                            tint = if (isTajweed) QuranifyColors.Primary else QuranifyColors.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tajweed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTajweed) QuranifyColors.Primary else QuranifyColors.TextSecondary
                        )
                    }
                }

                // 4. Audio Recitation Quick Toggle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAudioPlaying) QuranifyColors.Primary
                            else QuranifyColors.SurfaceContainer
                        )
                        .clickable { onToggleAudio() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = if (isAudioPlaying) "Pause Recitation" else "Play Recitation",
                        tint = if (isAudioPlaying) QuranifyColors.OnPrimary else QuranifyColors.Primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Surah Banner Ornamentation with Bismillah
 * Features sacred geometry arabesque canvas backdrop, Meccan/Verses/Friday pills,
 * ornate Surah rosette title, and luminous Bismillah calligraphy.
 */
@Composable
private fun SurahHeaderBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        QuranifyColors.SurfaceHigh.copy(alpha = 0.95f),
                        QuranifyColors.SurfaceContainer.copy(alpha = 0.85f),
                        QuranifyColors.Background.copy(alpha = 0.92f)
                    )
                )
            )
            .border(1.dp, QuranifyColors.OutlineVariant.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Sacred Geometry Arabesque Motif Backdrop
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r1 = size.minDimension * 0.35f
            val r2 = size.minDimension * 0.46f

            // Concentric geometric circles
            drawCircle(
                color = QuranifyColors.Primary.copy(alpha = 0.08f),
                radius = r1,
                center = center,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = QuranifyColors.Primary.copy(alpha = 0.06f),
                radius = r2,
                center = center,
                style = Stroke(
                    width = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            )

            // 8-Pointed Star Motif
            val path = Path()
            val numPoints = 8
            val outerR = size.minDimension * 0.42f
            val innerR = size.minDimension * 0.22f
            for (i in 0 until numPoints * 2) {
                val angle = (i * PI / numPoints) - (PI / 2)
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = QuranifyColors.Primary.copy(alpha = 0.05f),
                style = Stroke(width = 1.5f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Surah Info Pills Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meccan Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Meccan",
                        tint = Color(0xFF95D3BA),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Meccan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF95D3BA)
                    )
                }

                // 110 Verses Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Verses",
                        tint = QuranifyColors.TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "110 Verses",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuranifyColors.TextSecondary
                    )
                }

                // Friday Sunnah Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuranifyColors.SecondaryContainer.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Friday Sunnah",
                        tint = QuranifyColors.Secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Friday Sunnah",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.Secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Surah Title & Decorative Surah Rosette
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    QuranifyColors.OutlineVariant.copy(alpha = 0.5f),
                                    QuranifyColors.Primary.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(QuranifyColors.Background.copy(alpha = 0.85f))
                        .border(1.dp, QuranifyColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سُورَةُ الكَهْفِ",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuranifyColors.Primary
                        )
                        Text(
                            text = "THE CAVE",
                            fontSize = 10.sp,
                            letterSpacing = 2.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QuranifyColors.TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    QuranifyColors.Primary.copy(alpha = 0.4f),
                                    QuranifyColors.OutlineVariant.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Luminous Bismillah Calligraphy
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuranifyColors.Primary,
                lineHeight = 44.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = QuranifyColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Ayah Block with Specular Emerald Highlight Strip for Active Recitation.
 * Features top header row with Ayah badge, "RECITING NOW" waveform,
 * action buttons (Play, Bookmark, Tafsir, Share), Tajweed-colored Arabic text,
 * and English translation with footnotes.
 */
@Composable
private fun AyahBlock(
    ayah: MushafAyahData,
    isActive: Boolean,
    isBookmarked: Boolean,
    isTajweed: Boolean,
    showTranslation: Boolean,
    arabicFontSize: Int,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTafsirClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isActive) QuranifyColors.SurfaceHigh.copy(alpha = 0.95f)
                else QuranifyColors.SurfaceContainer.copy(alpha = 0.65f)
            )
            .border(
                1.dp,
                if (isActive) QuranifyColors.Primary.copy(alpha = 0.45f)
                else QuranifyColors.OutlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onCardClick() }
    ) {
        // Specular Emerald Highlight Strip for Active Reciting Ayah
        if (isActive) {
            Box(
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(6.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    QuranifyColors.Primary,
                                    QuranifyColors.Secondary,
                                    QuranifyColors.PrimaryContainer
                                )
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header Row: Ayah Index + Reciting Waveform + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Ayah Index and Reciting Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) QuranifyColors.Primary.copy(alpha = 0.2f)
                                else QuranifyColors.SurfaceHighest.copy(alpha = 0.6f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ayah.ayahNumber.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) QuranifyColors.Primary else QuranifyColors.TextSecondary
                        )
                    }

                    if (isActive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(QuranifyColors.Primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Waveform",
                                tint = QuranifyColors.Primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "RECITING NOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuranifyColors.Primary,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                // Right: Micro Ayah Actions Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play / Pause Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) QuranifyColors.Primary
                                else QuranifyColors.SurfaceHighest.copy(alpha = 0.5f)
                            )
                            .clickable { onPlayClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isActive) "Pause verse audio" else "Play verse audio",
                            tint = if (isActive) QuranifyColors.OnPrimary else QuranifyColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Bookmark Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f))
                            .clickable { onBookmarkClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark ayah",
                            tint = if (isBookmarked) QuranifyColors.Primary else QuranifyColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Tafsir Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f))
                            .clickable { onTafsirClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Tafsir for verse ${ayah.ayahNumber}",
                            tint = QuranifyColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Share Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f))
                            .clickable { onShareClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share ayah",
                            tint = QuranifyColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Ayah Text with Tajweed Coloring and Rosette
            val arabicAnnotated = buildAnnotatedString {
                val baseColor = QuranifyColors.TextPrimary
                val tajweedColor = Color(0xFF95D3BA) // tertiary fixed dim / emerald
                val ayahRosette = " ۝${toArabicDigits(ayah.ayahNumber)} "

                if (isTajweed && ayah.tajweedPart != null) {
                    if (ayah.tajweedPrefix) {
                        withStyle(SpanStyle(color = QuranifyColors.Primary, fontWeight = FontWeight.SemiBold)) {
                            append(ayah.tajweedPart)
                        }
                        withStyle(SpanStyle(color = baseColor)) {
                            append(ayah.textUthmani)
                        }
                    } else {
                        withStyle(SpanStyle(color = baseColor)) {
                            append(ayah.textUthmani)
                        }
                        withStyle(SpanStyle(color = tajweedColor, fontWeight = FontWeight.SemiBold)) {
                            append(ayah.tajweedPart)
                        }
                    }
                } else {
                    withStyle(SpanStyle(color = baseColor)) {
                        append(ayah.textUthmani)
                        if (ayah.tajweedPart != null) {
                            append(ayah.tajweedPart)
                        }
                    }
                }

                // Decorative Ayah End Rosette
                withStyle(SpanStyle(color = QuranifyColors.Primary, fontWeight = FontWeight.Bold, fontSize = (arabicFontSize * 0.75f).sp)) {
                    append(ayahRosette)
                }
            }

            Text(
                text = arabicAnnotated,
                fontSize = arabicFontSize.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.End,
                lineHeight = (arabicFontSize * 2.1f).sp,
                modifier = Modifier.fillMaxWidth()
            )

            // English Translation
            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = ayah.translation,
                    fontSize = 14.sp,
                    color = if (isActive) QuranifyColors.TextPrimary else QuranifyColors.TextSecondary,
                    lineHeight = 22.sp
                )

                if (ayah.footnote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ayah.footnote,
                        fontSize = 11.sp,
                        color = QuranifyColors.TextTertiary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Reading Mode Bottom Utilities (Apple-style Quick Tools capsule)
 * Features Hifz Repeat loop counter, Tafsir modal trigger, and Page/Stream view toggle.
 */
@Composable
private fun MushafBottomToolsCapsule(
    repeatLabel: String,
    onCycleRepeat: () -> Unit,
    onOpenTafsir: () -> Unit,
    isPageView: Boolean,
    onTogglePageView: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.92f))
                .border(1.dp, QuranifyColors.OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Repeat Cycle Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (repeatLabel.contains("Off")) QuranifyColors.SurfaceContainer
                            else QuranifyColors.Primary.copy(alpha = 0.15f)
                        )
                        .clickable { onCycleRepeat() }
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Audio Repeat Loop",
                        tint = if (repeatLabel.contains("Off")) QuranifyColors.TextSecondary else QuranifyColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = repeatLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (repeatLabel.contains("Off")) QuranifyColors.TextSecondary else QuranifyColors.TextPrimary
                    )
                }

                // Tafsir Modal Trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(QuranifyColors.SurfaceContainer)
                        .clickable { onOpenTafsir() }
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Open Tafsir",
                        tint = Color(0xFF95D3BA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tafsir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuranifyColors.TextPrimary
                    )
                }

                // Page View Mode Toggle (Stream vs Mushaf Page)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(QuranifyColors.Primary)
                        .clickable { onTogglePageView() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPageView) Icons.Default.ViewAgenda else Icons.Default.Layers,
                        contentDescription = if (isPageView) "Switch to Ayah stream" else "Switch to Mushaf page",
                        tint = QuranifyColors.OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Traditional Mushaf Page View mode with ornamental frame and continuous text.
 */
@Composable
private fun MushafPageViewCard(
    ayahs: List<MushafAyahData>,
    activeAyah: Int,
    arabicFontSize: Int,
    isTajweed: Boolean,
    onSelectAyah: (MushafAyahData) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.9f))
            .border(2.dp, QuranifyColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جُزْء ١٥",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextSecondary
                )
                Text(
                    text = "سُورَةُ الكَهْفِ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.Primary
                )
                Text(
                    text = "صَفْحَة ٢٩٣",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextSecondary
                )
            }

            HorizontalDivider(color = QuranifyColors.OutlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            // Continuous Uthmani Arabic Text
            val pageText = buildAnnotatedString {
                ayahs.forEach { ayah ->
                    val isSelected = ayah.ayahNumber == activeAyah
                    val color = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextPrimary

                    if (isTajweed && ayah.tajweedPart != null) {
                        if (ayah.tajweedPrefix) {
                            withStyle(SpanStyle(color = Color(0xFF95D3BA), fontWeight = FontWeight.SemiBold)) {
                                append(ayah.tajweedPart)
                            }
                            withStyle(SpanStyle(color = color)) {
                                append(ayah.textUthmani)
                            }
                        } else {
                            withStyle(SpanStyle(color = color)) {
                                append(ayah.textUthmani)
                            }
                            withStyle(SpanStyle(color = Color(0xFF95D3BA), fontWeight = FontWeight.SemiBold)) {
                                append(ayah.tajweedPart)
                            }
                        }
                    } else {
                        withStyle(SpanStyle(color = color)) {
                            append(ayah.textUthmani)
                            if (ayah.tajweedPart != null) {
                                append(ayah.tajweedPart)
                            }
                        }
                    }

                    withStyle(SpanStyle(color = QuranifyColors.Primary, fontWeight = FontWeight.Bold)) {
                        append(" ۝${toArabicDigits(ayah.ayahNumber)} ")
                    }
                }
            }

            Text(
                text = pageText,
                fontSize = (arabicFontSize - 2).sp,
                lineHeight = ((arabicFontSize - 2) * 2.1f).sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = QuranifyColors.OutlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

            // Page Footer
            Text(
                text = "— ٢٩٣ —",
                fontSize = 12.sp,
                color = QuranifyColors.TextTertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Tafsir Sheet Dialog for deep study of the active Ayah.
 */
@Composable
private fun TafsirDialog(
    ayah: MushafAyahData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tafsir Ibn Kathir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = QuranifyColors.Primary
                )
                Text(
                    text = "Ayah ${ayah.ayahNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuranifyColors.TextSecondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = ayah.textUthmani + (ayah.tajweedPart ?: "") + " ۝${toArabicDigits(ayah.ayahNumber)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuranifyColors.TextPrimary,
                    textAlign = TextAlign.End,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = QuranifyColors.OutlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = ayah.tafsirSnippet,
                    fontSize = 14.sp,
                    color = QuranifyColors.TextSecondary,
                    lineHeight = 21.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = QuranifyColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = QuranifyColors.SurfaceHigh,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Converts integer digits to classic Arabic numerals (١, ٢, ٣...)
 */
private fun toArabicDigits(number: Int): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { digits[it - '0'] }.joinToString("")
}
