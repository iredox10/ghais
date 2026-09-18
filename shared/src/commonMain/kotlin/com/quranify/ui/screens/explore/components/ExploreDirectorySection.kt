package com.quranify.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.data.seed.QuranData
import com.quranify.domain.model.Surah
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors

@Composable
fun ExploreDirectorySection(
    surahs: List<Surah> = QuranData.SURAHS,
    onSurahClick: (Surah) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        surahs.forEach { surah ->
            SurahDirectoryItem(
                surah = surah,
                onClick = { onSurahClick(surah) },
                onPlayClick = {
                    val track = TrackItem(
                        reciterSlug = "mishary",
                        reciterName = "Sheikh Mishary Rashid Alafasy",
                        surahId = surah.id,
                        surahNameEn = surah.nameEn,
                        surahNameAr = surah.nameAr,
                        ayahNo = 1,
                        audioUrl = "https://server8.mp3quran.net/afs/${surah.id.toString().padStart(3, '0')}.mp3",
                        durationMs = 300000L
                    )
                    AudioEngine.playTrack(track)
                }
            )
        }
    }
}

@Composable
fun SurahDirectoryItem(
    surah: Surah,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Index Circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${surah.id}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = surah.nameEn,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(QuranifyColors.SurfaceHigh, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = surah.revelationType.replaceFirstChar { it.uppercase() },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = QuranifyColors.TextSecondary
                        )
                    }
                }
                Text(
                    text = "${surah.meaning} • ${surah.ayahsCount} Verses",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Arabic Calligraphy + Play Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = surah.nameAr,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = QuranifyColors.Primary,
                modifier = Modifier.padding(end = 12.dp)
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.Primary.copy(alpha = 0.15f))
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = QuranifyColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
