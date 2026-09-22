package com.ghais.ui.screens.mushaf

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
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

/**
 * Strict Noir Glass Mushaf — true-black canvas (glow zone -> #050506 +
 * ambient glow + film grain), grayscale hero banner, ayah blocks in the
 * NoirListRow recipe (engraved number well, chrome/ghost affordances).
 * Zero hue — tajweed distinction reads through weight only, state reads
 * through fill elevation, chromium and opacity.
 *
 * The Mushaf page is dark paper (near-black #0E0E10, NOT pure white) for
 * eye comfort. Arabic/uthmani stays at 100% white on all dark wells —
 * never dimmed below 85%. Tab signature, audio/bookmark/tafsir state and
 * navigation targets are unchanged.
 */
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

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp)
            ) {
                // 1. Top floating control capsule (noir glass).
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

                // 2. Surah ornamentation header banner (grayscale hero).
                item {
                    SurahHeaderBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. Ayah reading stream OR classic dark-paper page view.
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

                // 4. Reading-mode bottom utilities capsule.
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

            // Tafsir modal sheet dialog (noir).
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
 * Top floating control capsule in noir glass: ghost surah selector, engraved
 * Juz/Page indicator, ghost preference wells, chromium audio disc.
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
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(32.dp))
            .topSpecular(inset = 30.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah selector — ghost pill + noir dropdown.
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(20.dp))
                        .noirClickable(onClick = onToggleSurahMenu)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Surah Selector",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedSurah,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Dropdown",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isSurahMenuOpen,
                    onDismissRequest = onToggleSurahMenu,
                    modifier = Modifier.background(Color(0xFF141416))
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
                                    color = if (surah == selectedSurah) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                    fontWeight = if (surah == selectedSurah) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { onSelectSurah(surah) }
                        )
                    }
                }
            }

            // Juz / Page indicator — engraved inset.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(GhaisNoir.insetFill())
                    .border(1.dp, GhaisNoir.InsetBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "JUZ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextTertiary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "15",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary
                    )
                    Text(
                        text = " • ",
                        fontSize = 10.sp,
                        color = GhaisNoir.TextTertiary
                    )
                    Text(
                        text = "P.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextTertiary
                    )
                    Text(
                        text = "293",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary
                    )
                }
            }

            // Quick preferences: font-size well, translation + tajweed ghosts, chrome audio disc.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Font size well.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                        .noirClickable(onClick = onCycleFontSize),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font size: $arabicFontSize",
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${arabicFontSize}pt",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextTertiary,
                            lineHeight = 9.sp
                        )
                    }
                }

                // 2. Translation ghost toggle — active = elevated fill + primary label.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (showTranslation) GhaisNoir.Fill4 else GhaisNoir.Fill2)
                        .border(
                            1.dp,
                            if (showTranslation) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                            CircleShape
                        )
                        .noirClickable(onClick = onToggleTranslation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Toggle Translation",
                        tint = if (showTranslation) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 3. Tajweed ghost toggle.
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isTajweed) GhaisNoir.Fill4 else GhaisNoir.Fill2)
                        .border(
                            1.dp,
                            if (isTajweed) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                            RoundedCornerShape(18.dp)
                        )
                        .noirClickable(onClick = onToggleTajweed)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Tajweed",
                            tint = if (isTajweed) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tajweed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTajweed) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
                        )
                    }
                }

                // 4. Audio disc — chromium while playing, clay well otherwise.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAudioPlaying) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isAudioPlaying) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable(onClick = onToggleAudio),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = if (isAudioPlaying) "Pause Recitation" else "Play Recitation",
                        tint = if (isAudioPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Grayscale surah banner: monochrome sacred-geometry backdrop, ghost info
 * pills, engraved rosette with uthmani at 100% white, luminous Bismillah.
 */
@Composable
private fun SurahHeaderBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GhaisNoir.cardFill())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(22.dp))
            .topSpecular(inset = 30.dp)
            .padding(18.dp)
    ) {
        // Monochrome sacred-geometry backdrop — white line-work only.
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r1 = size.minDimension * 0.35f
            val r2 = size.minDimension * 0.46f

            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r1,
                center = center,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = r2,
                center = center,
                style = Stroke(
                    width = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            )

            // 8-pointed star motif.
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
                color = Color.White.copy(alpha = 0.05f),
                style = Stroke(width = 1.5f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Info pills — ghost washes, monochrome glyphs.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoirInfoPill(icon = Icons.Default.Place, text = "Meccan")
                NoirInfoPill(icon = Icons.Default.FilterList, text = "110 Verses")
                NoirInfoPill(icon = Icons.Default.Star, text = "Friday Sunnah", emphasized = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Surah rosette — engraved well, uthmani at 100% white.
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
                                    GhaisNoir.BorderCard,
                                    GhaisNoir.SpecularTop.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(GhaisNoir.insetFill())
                        .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سُورَةُ الكَهْفِ",
                            fontSize = 32.sp,
                            lineHeight = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                        Text(
                            text = "THE CAVE",
                            fontSize = 10.sp,
                            letterSpacing = 2.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextTertiary
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
                                    GhaisNoir.SpecularTop.copy(alpha = 0.5f),
                                    GhaisNoir.BorderCard,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bismillah — uthmani at 100% white, translation secondary.
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.TextPrimary,
                lineHeight = 48.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = GhaisNoir.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Ghost info pill — informational, zero hue. */
@Composable
private fun NoirInfoPill(icon: ImageVector, text: String, emphasized: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (emphasized) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasized) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary
        )
    }
}

/**
 * Ayah block in the NoirListRow recipe: soft glass fill (active = stronger
 * wash) + card border + top-only specular, chromium edge strip while live,
 * engraved number well, chrome/ghost action cluster.
 *
 * Tajweed distinction is weight-only (SemiBold vs Normal) at 100% white —
 * zero hue, never below the 85% legibility floor.
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
            .background(if (isActive) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .topSpecular(inset = 24.dp)
            .clickable { onCardClick() }
    ) {
        // Chromium edge strip while this ayah is live (monochrome, not emerald).
        if (isActive) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(4.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(GhaisNoir.chromeFill())
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header row: engraved index + live badge + ghost/chrome actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ayah.ayahNumber.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                    }

                    if (isActive) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GhaisNoir.Fill4)
                                .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Waveform",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "RECITING NOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.TextPrimary,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Play / pause disc — chromium while live, clay well otherwise.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isActive) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                                CircleShape
                            )
                            .noirClickable(onClick = onPlayClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isActive) "Pause verse audio" else "Play verse audio",
                            tint = if (isActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    GhostCircleButton(
                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark ayah",
                        tint = if (isBookmarked) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        onClick = onBookmarkClick
                    )
                    GhostCircleButton(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Tafsir for verse ${ayah.ayahNumber}",
                        onClick = onTafsirClick
                    )
                    GhostCircleButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share ayah",
                        onClick = onShareClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Uthmani text — 100% white; tajweed = SemiBold at 100% (weight only).
            val arabicAnnotated = buildAnnotatedString {
                val base = GhaisNoir.TextPrimary
                val ayahRosette = " ۝${toArabicDigits(ayah.ayahNumber)} "

                if (isTajweed && ayah.tajweedPart != null) {
                    if (ayah.tajweedPrefix) {
                        withStyle(SpanStyle(color = base, fontWeight = FontWeight.SemiBold)) {
                            append(ayah.tajweedPart)
                        }
                        withStyle(SpanStyle(color = base)) {
                            append(ayah.textUthmani)
                        }
                    } else {
                        withStyle(SpanStyle(color = base)) {
                            append(ayah.textUthmani)
                        }
                        withStyle(SpanStyle(color = base, fontWeight = FontWeight.SemiBold)) {
                            append(ayah.tajweedPart)
                        }
                    }
                } else {
                    withStyle(SpanStyle(color = base)) {
                        append(ayah.textUthmani)
                        if (ayah.tajweedPart != null) {
                            append(ayah.tajweedPart)
                        }
                    }
                }

                withStyle(
                    SpanStyle(
                        color = GhaisNoir.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = (arabicFontSize * 0.75f).sp
                    )
                ) {
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

            if (showTranslation) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = ayah.translation,
                    fontSize = 14.sp,
                    color = if (isActive) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                    lineHeight = 22.sp
                )

                if (ayah.footnote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ayah.footnote,
                        fontSize = 11.sp,
                        color = GhaisNoir.TextTertiary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/** Ghost circular micro-action — Fill2 wash + hairline, monochrome glyph. */
@Composable
private fun GhostCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = GhaisNoir.TextTertiary
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Reading-mode bottom utilities capsule in noir glass: ghost repeat + tafsir
 * pills, chromium page/stream toggle. State reads through fill elevation.
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
                .background(GhaisNoir.cardFillSoft())
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(32.dp))
                .topSpecular(inset = 30.dp)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Repeat cycle pill — elevated when looping.
                val repeatOff = repeatLabel.contains("Off")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (repeatOff) GhaisNoir.Fill2 else GhaisNoir.Fill4)
                        .border(
                            1.dp,
                            if (repeatOff) GhaisNoir.BorderGhost else GhaisNoir.SpecularTop,
                            RoundedCornerShape(20.dp)
                        )
                        .noirClickable(onClick = onCycleRepeat)
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Audio Repeat Loop",
                        tint = if (repeatOff) GhaisNoir.TextTertiary else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = repeatLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (repeatOff) GhaisNoir.TextTertiary else GhaisNoir.TextPrimary
                    )
                }

                // Tafsir trigger — ghost pill.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(20.dp))
                        .noirClickable(onClick = onOpenTafsir)
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Open Tafsir",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tafsir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary
                    )
                }

                // Page/stream toggle — chromium disc.
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .noirClickable(onClick = onTogglePageView),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPageView) Icons.Default.ViewAgenda else Icons.Default.Layers,
                        contentDescription = if (isPageView) "Switch to Ayah stream" else "Switch to Mushaf page",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Near-black dark paper for the Mushaf page — eye comfort, no pure white. */
private val NoirDarkPaper = Color(0xFF0E0E10)

/**
 * Traditional Mushaf page view on dark paper (near-black, not pure white):
 * engraved header, continuous uthmani at 100% white, hairline rules.
 * Tajweed distinction is weight-only at 100% white — zero hue.
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
            .background(NoirDarkPaper)
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(20.dp))
            .topSpecular(inset = 28.dp)
            .noirClickable(onClick = {})
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page header — engraved, monochrome.
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
                    color = GhaisNoir.TextSecondary
                )
                Text(
                    text = "سُورَةُ الكَهْفِ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextPrimary
                )
                Text(
                    text = "صَفْحَة ٢٩٣",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisNoir.TextSecondary
                )
            }

            HorizontalDivider(color = GhaisNoir.BorderGhost, thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            // Continuous uthmani — selected ayah bold, rest normal, all 100% white.
            val pageText = buildAnnotatedString {
                ayahs.forEach { ayah ->
                    val isSelected = ayah.ayahNumber == activeAyah
                    val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

                    if (isTajweed && ayah.tajweedPart != null) {
                        if (ayah.tajweedPrefix) {
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Bold)) {
                                append(ayah.tajweedPart)
                            }
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = weight)) {
                                append(ayah.textUthmani)
                            }
                        } else {
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = weight)) {
                                append(ayah.textUthmani)
                            }
                            withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Bold)) {
                                append(ayah.tajweedPart)
                            }
                        }
                    } else {
                        withStyle(SpanStyle(color = GhaisNoir.TextPrimary, fontWeight = weight)) {
                            append(ayah.textUthmani)
                            if (ayah.tajweedPart != null) {
                                append(ayah.tajweedPart)
                            }
                        }
                    }

                    withStyle(
                        SpanStyle(
                            color = GhaisNoir.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(" ۝${toArabicDigits(ayah.ayahNumber)} ")
                    }
                }
            }

            Text(
                text = pageText,
                fontSize = (arabicFontSize - 2).sp,
                lineHeight = ((arabicFontSize - 2) * 2.1f).sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .noirClickable(onClick = {
                        ayahs.find { it.ayahNumber == activeAyah }?.let(onSelectAyah)
                    })
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = GhaisNoir.BorderGhost, thickness = 1.dp)

            // Page footer.
            Text(
                text = "— ٢٩٣ —",
                fontSize = 12.sp,
                color = GhaisNoir.TextTertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Tafsir sheet dialog in noir glass: dark card, 100%-white quote Arabic,
 * ghost rule, secondary body, white Close affordance.
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
                    color = GhaisNoir.TextPrimary
                )
                Text(
                    text = "Ayah ${ayah.ayahNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextTertiary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = ayah.textUthmani + (ayah.tajweedPart ?: "") + " ۝${toArabicDigits(ayah.ayahNumber)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextPrimary,
                    textAlign = TextAlign.End,
                    lineHeight = 34.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GhaisNoir.BorderGhost)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = ayah.tafsirSnippet,
                    fontSize = 14.sp,
                    color = GhaisNoir.TextSecondary,
                    lineHeight = 21.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GhaisNoir.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF131316),
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
