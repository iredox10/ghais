package com.quranify.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.data.seed.QuranData
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.screens.explore.components.SurahDirectoryItem
import com.quranify.ui.screens.surah.SurahDetailScreen
import com.quranify.ui.theme.QuranifyColors

data class JuzInfo(
    val id: Int,
    val nameAr: String,
    val startVerse: String,
    val surahsSummary: String
)

object SurahsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        var bookmarkedSurahs by remember { mutableStateOf(setOf(1, 18, 36, 55, 67)) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "All Surahs",
                            color = QuranifyColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = QuranifyColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QuranifyColors.Background)
                )
            },
            containerColor = QuranifyColors.Background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuranData.SURAHS) { surah ->
                    val isBookmarked = bookmarkedSurahs.contains(surah.id)
                    SurahDirectoryItem(
                        surah = surah,
                        isBookmarked = isBookmarked,
                        onClick = { navigator.push(SurahDetailScreen(surah.id)) },
                        onPlayClick = {
                            val track = TrackItem(
                                reciterSlug = "mishary",
                                reciterName = "Sheikh Mishary Rashid Alafasy",
                                surahId = surah.id,
                                surahNameEn = surah.nameEn,
                                surahNameAr = surah.nameAr,
                                ayahNo = 0,
                                audioUrl = "https://server8.mp3quran.net/afs/${surah.id.toString().padStart(3, '0')}.mp3",
                                durationMs = 300000L
                            )
                            AudioEngine.playTrack(track)
                            rootNavigator.push(NowPlayingScreen())
                        },
                        onBookmarkClick = {
                            bookmarkedSurahs = if (isBookmarked) {
                                bookmarkedSurahs - surah.id
                            } else {
                                bookmarkedSurahs + surah.id
                            }
                        }
                    )
                }
            }
        }
    }
}

object JuzBrowserScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val juzList = listOf(
            JuzInfo(1, "الجزء الأول", "Al-Fatihah 1 - Al-Baqarah 141", "Surahs 1 - 2"),
            JuzInfo(2, "الجزء الثاني", "Al-Baqarah 142 - Al-Baqarah 252", "Surah 2"),
            JuzInfo(3, "الجزء الثالث", "Al-Baqarah 253 - Ali 'Imran 92", "Surahs 2 - 3"),
            JuzInfo(4, "الجزء الرابع", "Ali 'Imran 93 - An-Nisa 23", "Surahs 3 - 4"),
            JuzInfo(5, "الجزء الخامس", "An-Nisa 24 - An-Nisa 147", "Surah 4"),
            JuzInfo(6, "الجزء السادس", "An-Nisa 148 - Al-Ma'idah 81", "Surahs 4 - 5"),
            JuzInfo(7, "الجزء السابع", "Al-Ma'idah 82 - Al-An'am 110", "Surahs 5 - 6"),
            JuzInfo(8, "الجزء الثامن", "Al-An'am 111 - Al-A'raf 87", "Surahs 6 - 7"),
            JuzInfo(9, "الجزء التاسع", "Al-A'raf 88 - Al-Anfal 40", "Surahs 7 - 8"),
            JuzInfo(10, "الجزء العاشر", "Al-Anfal 41 - At-Tawbah 92", "Surahs 8 - 9"),
            JuzInfo(11, "الجزء الحادي عشر", "At-Tawbah 93 - Hud 5", "Surahs 9 - 11"),
            JuzInfo(12, "الجزء الثاني عشر", "Hud 6 - Yusuf 52", "Surahs 11 - 12"),
            JuzInfo(13, "الجزء الثالث عشر", "Yusuf 53 - Ibrahim 52", "Surahs 12 - 14"),
            JuzInfo(14, "الجزء الرابع عشر", "Al-Hijr 1 - An-Nahl 128", "Surahs 15 - 16"),
            JuzInfo(15, "الجزء الخامس عشر", "Al-Isra 1 - Al-Kahf 74", "Surahs 17 - 18"),
            JuzInfo(16, "الجزء السادس عشر", "Al-Kahf 75 - Ta-Ha 135", "Surahs 18 - 20"),
            JuzInfo(17, "الجزء السابع عشر", "Al-Anbiya 1 - Al-Hajj 78", "Surahs 21 - 22"),
            JuzInfo(18, "الجزء الثامن عشر", "Al-Mu'minun 1 - Al-Furqan 20", "Surahs 23 - 25"),
            JuzInfo(19, "الجزء التاسع عشر", "Al-Furqan 21 - An-Naml 55", "Surahs 25 - 27"),
            JuzInfo(20, "الجزء العشرون", "An-Naml 56 - Al-Ankabut 45", "Surahs 27 - 29"),
            JuzInfo(21, "الجزء الحادي والعشرون", "Al-Ankabut 46 - Al-Ahzab 30", "Surahs 29 - 33"),
            JuzInfo(22, "الجزء الثاني والعشرون", "Al-Ahzab 31 - Ya-Sin 27", "Surahs 33 - 36"),
            JuzInfo(23, "الجزء الثالث والعشرون", "Ya-Sin 28 - Az-Zumar 31", "Surahs 36 - 39"),
            JuzInfo(24, "الجزء الرابع والعشرون", "Az-Zumar 32 - Fussilat 46", "Surahs 39 - 41"),
            JuzInfo(25, "الجزء الخامس والعشرون", "Fussilat 47 - Al-Jathiyah 37", "Surahs 41 - 45"),
            JuzInfo(26, "الجزء السادس والعشرون", "Al-Ahqaf 1 - Adh-Dhariyat 30", "Surahs 46 - 51"),
            JuzInfo(27, "الجزء السابع والعشرون", "Adh-Dhariyat 31 - Al-Hadid 29", "Surahs 51 - 57"),
            JuzInfo(28, "الجزء الثامن والعشرون", "Al-Mujadila 1 - At-Tahrim 12", "Surahs 58 - 66"),
            JuzInfo(29, "تبارك", "Al-Mulk 1 - Al-Mursalat 50", "Surahs 67 - 77"),
            JuzInfo(30, "عمّ", "An-Naba 1 - An-Nas 6", "Surahs 78 - 114")
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Juz Index",
                            color = QuranifyColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = QuranifyColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QuranifyColors.Background)
                )
            },
            containerColor = QuranifyColors.Background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(juzList) { juz ->
                    val isProminent = juz.id == 1 || juz.id == 29 || juz.id == 30
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(QuranifyColors.SurfaceLow.copy(alpha = 0.7f))
                            .border(
                                1.dp,
                                if (isProminent) QuranifyColors.Primary.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                val startSurahId = when (juz.id) {
                                    1 -> 1
                                    2 -> 2
                                    3 -> 2
                                    4 -> 3
                                    5 -> 4
                                    6 -> 4
                                    7 -> 5
                                    8 -> 6
                                    9 -> 7
                                    10 -> 8
                                    11 -> 9
                                    12 -> 11
                                    13 -> 12
                                    14 -> 15
                                    15 -> 17
                                    16 -> 18
                                    17 -> 21
                                    18 -> 23
                                    19 -> 25
                                    20 -> 27
                                    21 -> 29
                                    22 -> 33
                                    23 -> 36
                                    24 -> 39
                                    25 -> 41
                                    26 -> 46
                                    27 -> 51
                                    28 -> 58
                                    29 -> 67
                                    30 -> 78
                                    else -> 1
                                }
                                navigator.push(SurahDetailScreen(startSurahId))
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isProminent) {
                                            Modifier
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                .border(
                                                    1.dp,
                                                    QuranifyColors.Primary.copy(alpha = 0.35f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                        } else {
                                            Modifier
                                                .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.7f))
                                                .border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.06f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = juz.id.toString().padStart(2, '0'),
                                    fontSize = 13.sp,
                                    fontWeight = if (isProminent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isProminent) QuranifyColors.Primary else QuranifyColors.TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Juz ${juz.id}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QuranifyColors.TextPrimary
                                )
                                Text(
                                    text = juz.startVerse,
                                    fontSize = 12.sp,
                                    color = QuranifyColors.TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = juz.nameAr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProminent) QuranifyColors.Primary else QuranifyColors.TextPrimary.copy(alpha = 0.9f),
                                textAlign = TextAlign.Right
                            )

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.8f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                    .clickable {
                                        val track = TrackItem(
                                            reciterSlug = "mishary",
                                            reciterName = "Sheikh Mishary Rashid Alafasy",
                                            surahId = if (juz.id == 30) 78 else if (juz.id == 29) 67 else 1,
                                            surahNameEn = "Juz ${juz.id}",
                                            surahNameAr = juz.nameAr,
                                            ayahNo = 0,
                                            audioUrl = "https://server8.mp3quran.net/afs/${if (juz.id == 30) "078" else if (juz.id == 29) "067" else "001"}.mp3",
                                            durationMs = 300000L
                                        )
                                        AudioEngine.playTrack(track)
                                        rootNavigator.push(NowPlayingScreen())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Juz ${juz.id}",
                                    tint = QuranifyColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
