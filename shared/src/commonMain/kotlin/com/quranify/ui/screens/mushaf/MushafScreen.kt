package com.quranify.ui.screens.mushaf

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors

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
        var playingAyah by remember { mutableStateOf(1) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Floating Mushaf Control Capsule
            item {
                MushafControlCapsule(
                    isTajweed = isTajweedActive,
                    onToggleTajweed = { isTajweedActive = !isTajweedActive }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Surah Ornamentation Header Banner
            item {
                SurahHeaderBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ayah Reading Stream
            item {
                AyahBlock(
                    ayahNumber = 1,
                    arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا ۜ",
                    translation = "[All] praise is due to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.",
                    isActive = playingAyah == 1,
                    onPlayClick = {
                        playingAyah = 1
                        AudioEngine.playTrack(
                            TrackItem(
                                reciterSlug = "mishary",
                                reciterName = "Sheikh Mishary Rashid Alafasy",
                                surahId = 18,
                                surahNameEn = "Al-Kahf",
                                surahNameAr = "الكهف",
                                ayahNo = 1,
                                audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018001.mp3",
                                textUthmani = "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا ۜ",
                                durationMs = 15000L
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                AyahBlock(
                    ayahNumber = 2,
                    arabicText = "قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا",
                    translation = "[He has made it] straight, to warn of severe punishment from Him and to give good tidings to the believers who do righteous deeds that they will have a good reward.",
                    isActive = playingAyah == 2,
                    onPlayClick = {
                        playingAyah = 2
                        AudioEngine.playTrack(
                            TrackItem(
                                reciterSlug = "mishary",
                                reciterName = "Sheikh Mishary Rashid Alafasy",
                                surahId = 18,
                                surahNameEn = "Al-Kahf",
                                surahNameAr = "الكهف",
                                ayahNo = 2,
                                audioUrl = "https://everyayah.com/data/Alafasy_128kbps/018002.mp3",
                                textUthmani = "قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا",
                                durationMs = 20000L
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                AyahBlock(
                    ayahNumber = 3,
                    arabicText = "مَّاكِثِينَ فِيهِ أَبَدًا",
                    translation = "In which they will remain forever.",
                    isActive = playingAyah == 3,
                    onPlayClick = { playingAyah = 3 }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                AyahBlock(
                    ayahNumber = 4,
                    arabicText = "وَيُنذِرَ الَّذِينَ قَالُوا اتَّخَذَ اللَّهُ وَلَدًا",
                    translation = "And to warn those who say, 'Allah has taken a son.'",
                    isActive = playingAyah == 4,
                    onPlayClick = { playingAyah = 4 }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                AyahBlock(
                    ayahNumber = 5,
                    arabicText = "مَّا لَهُم بِهِ مِنْ عِلْمٍ وَلَا لِآبَائِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَاهِهِمْ ۚ إِن يَقُولُونَ إِلَّا كَذِبًا",
                    translation = "They have no knowledge of it, nor had their fathers. Grave is the word that comes out of their mouths; they speak not except a lie.",
                    isActive = playingAyah == 5,
                    onPlayClick = { playingAyah = 5 }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MushafControlCapsule(
    isTajweed: Boolean,
    onToggleTajweed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(QuranifyColors.SurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah Dropdown Capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(QuranifyColors.SurfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = "Surah",
                    tint = QuranifyColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Al-Kahf (18)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuranifyColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Dropdown",
                    tint = QuranifyColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Juz / Page
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(QuranifyColors.Background)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "JUZ 15 • P.293",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.Primary
                )
            }

            // Tajweed Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isTajweed) QuranifyColors.Primary.copy(alpha = 0.2f) else QuranifyColors.SurfaceContainer
                    )
                    .clickable { onToggleTajweed() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Tajweed",
                        tint = if (isTajweed) QuranifyColors.Primary else QuranifyColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tajweed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTajweed) QuranifyColors.Primary else QuranifyColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahHeaderBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        QuranifyColors.SurfaceHigh,
                        QuranifyColors.SurfaceContainer,
                        QuranifyColors.Background
                    )
                )
            )
            .border(1.dp, QuranifyColors.OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Info Pills Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(QuranifyColors.SurfaceHigh, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Meccan",
                        fontSize = 10.sp,
                        color = QuranifyColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(QuranifyColors.SurfaceHigh, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "110 Verses",
                        fontSize = 10.sp,
                        color = QuranifyColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(QuranifyColors.Secondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Friday Sunnah",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.Secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Surah Title Rosette
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(QuranifyColors.OutlineVariant))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(QuranifyColors.Background)
                        .border(1.dp, QuranifyColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سُورَةُ الكَهْفِ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuranifyColors.Primary
                        )
                        Text(
                            text = "THE CAVE",
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp,
                            color = QuranifyColors.TextSecondary
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).height(1.dp).background(QuranifyColors.OutlineVariant))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Luminous Bismillah
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = QuranifyColors.Primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                fontSize = 11.sp,
                color = QuranifyColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AyahBlock(
    ayahNumber: Int,
    arabicText: String,
    translation: String,
    isActive: Boolean = false,
    onPlayClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActive) QuranifyColors.SurfaceContainer else QuranifyColors.SurfaceLow
            )
            .border(
                1.dp,
                if (isActive) QuranifyColors.Primary.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isActive) {
                // Specular highlight strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(QuranifyColors.Primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // Arabic Verse
                Text(
                    text = "$arabicText  ﴿$ayahNumber﴾",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary,
                    textAlign = TextAlign.End,
                    lineHeight = 38.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Translation
                Text(
                    text = translation,
                    fontSize = 13.sp,
                    color = QuranifyColors.TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isActive) QuranifyColors.Primary else QuranifyColors.SurfaceHigh
                            )
                            .clickable { onPlayClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isActive) Color(0xFF003824) else QuranifyColors.TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isActive) "Reciting" else "Play",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color(0xFF003824) else QuranifyColors.TextPrimary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Tafsir",
                                tint = QuranifyColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = QuranifyColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = QuranifyColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
