package com.quranify.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors

@Composable
fun AyahOfTheDayCard(
    modifier: Modifier = Modifier,
    onTafsirClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        QuranifyColors.SurfaceHigh,
                        QuranifyColors.SurfaceContainer,
                        QuranifyColors.Background
                    )
                )
            )
            .border(
                width = 1.dp,
                color = QuranifyColors.OutlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ AYAH OF THE DAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.Primary,
                    letterSpacing = 1.5.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Surah Ar-Rahman • 55:13",
                        fontSize = 10.sp,
                        color = QuranifyColors.Secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Arabic Verse
            Text(
                text = "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
                fontSize = 26.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Bold,
                color = QuranifyColors.TextPrimary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Translation
            Text(
                text = "“Which of the favors of your Lord will you deny?”",
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = QuranifyColors.TextSecondary,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Primary Button
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(QuranifyColors.Primary)
                        .clickable {
                            AudioEngine.playTrack(
                                TrackItem(
                                    reciterSlug = "mishary-rashid-alafasy",
                                    reciterName = "Mishary Rashid Alafasy",
                                    surahId = 55,
                                    surahNameEn = "Ar-Rahman",
                                    surahNameAr = "الرحمن",
                                    ayahNo = 13,
                                    audioUrl = "https://everyayah.com/data/Alafasy_128kbps/055013.mp3",
                                    textUthmani = "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
                                    durationMs = 18000L
                                )
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▶ Listen (0:18)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.OnPrimary
                    )
                }
                
                // Action icon buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onTafsirClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = QuranifyColors.TextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Tafsir",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = QuranifyColors.TextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHighest.copy(alpha = 0.5f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = QuranifyColors.TextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
