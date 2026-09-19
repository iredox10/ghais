package com.quranify.ui.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.quranify.data.seed.CuratedPlaylist
import com.quranify.data.seed.StitchAssets
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.curated.AllCuratedPlaylistsScreen
import com.quranify.ui.screens.curated.CuratedPlaylistDetailScreen
import com.quranify.ui.screens.player.NowPlayingScreen

private val TrendingElectricPurple = Color(0xFFA855F7)
private val TrendingPurpleStart = Color(0xFF8B5CF6)
private val MutedGreyText = Color(0xFF9CA3AF)

@Composable
fun CuratedForPeaceSection(
    modifier: Modifier = Modifier,
    playlists: List<CuratedPlaylist> = StitchAssets.CuratedForPeace,
    onSeeAllClick: (() -> Unit)? = null,
    onPlaylistClick: ((String) -> Unit)? = null,
    onPlayClick: ((String) -> Unit)? = null
) {
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    // Animated equalizer bars indicator in Trending Electric Purple (#A855F7)
    val infiniteTransition = rememberInfiniteTransition(label = "CuratedEqualizer")
    val eq1Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val eq2Height by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 580, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar2"
    )
    val eq3Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar3"
    )

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Curated for Peace",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Selected recitations for your daily routine",
                    fontSize = 12.sp,
                    color = MutedGreyText
                )
            }
            
            // Equalizer Bars and See All Button in Trending Purple (#A855F7)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated Equalizer Bars Indicator
                Row(
                    modifier = Modifier.height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(eq1Height.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TrendingElectricPurple)
                    )
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(eq2Height.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TrendingElectricPurple)
                    )
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(eq3Height.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TrendingElectricPurple)
                    )
                }

                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    color = TrendingElectricPurple,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (onSeeAllClick != null) {
                                onSeeAllClick()
                            } else {
                                rootNavigator?.push(AllCuratedPlaylistsScreen())
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = {
                        if (onPlaylistClick != null) {
                            onPlaylistClick(playlist.id)
                        } else {
                            rootNavigator?.push(CuratedPlaylistDetailScreen(playlist.id))
                        }
                    },
                    onPlayClick = {
                        if (onPlayClick != null) {
                            onPlayClick(playlist.id)
                        } else {
                            playCuratedPlaylist(playlist)
                            rootNavigator?.push(NowPlayingScreen())
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: CuratedPlaylist,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(144.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF16161A))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            TrendingElectricPurple.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient shade overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
            
            // Bottom Action Bar inside card (Tag + Play button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category tag pill: Frosted dark glass with purple/white border
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xCC111415))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    TrendingElectricPurple.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = playlist.tag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Floating play button: Trending Purple gradient circular button (#8B5CF6 to #A855F7) with pure white play icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    TrendingPurpleStart,
                                    TrendingElectricPurple
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Titles: Pure white
        Text(
            text = playlist.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Descriptions: Muted grey
        Text(
            text = playlist.subtitle,
            color = MutedGreyText,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

/**
 * Direct playback of curated playlist using AudioEngine and real Quranic audio streams.
 */
private fun playCuratedPlaylist(playlist: CuratedPlaylist) {
    val track = when (playlist.id) {
        "deep-focus-study", "deep-focus-and-study" -> TrackItem(
            reciterSlug = "al-husary",
            reciterName = "Mahmoud Khalil Al-Husary",
            surahId = 67,
            surahNameEn = "Al-Mulk",
            surahNameAr = "الملك",
            ayahNo = 1,
            audioUrl = "https://server13.mp3quran.net/husr/067.mp3",
            durationMs = 445000L
        )
        "heart-soothing" -> TrackItem(
            reciterSlug = "mishary",
            reciterName = "Mishary Rashid Alafasy",
            surahId = 55,
            surahNameEn = "Ar-Rahman",
            surahNameAr = "الرحمن",
            ayahNo = 1,
            audioUrl = "https://server8.mp3quran.net/afs/055.mp3",
            durationMs = 680000L
        )
        "morning-adhkar", "morning-adhkar-and-barakah" -> TrackItem(
            reciterSlug = "mishary",
            reciterName = "Mishary Rashid Alafasy",
            surahId = 1,
            surahNameEn = "Al-Fatihah",
            surahNameAr = "الفاتحة",
            ayahNo = 1,
            audioUrl = "https://server8.mp3quran.net/afs/001.mp3",
            durationMs = 52000L
        )
        "bedtime-sakinah" -> TrackItem(
            reciterSlug = "al-husary",
            reciterName = "Mahmoud Khalil Al-Husary",
            surahId = 112,
            surahNameEn = "Al-Ikhlas",
            surahNameAr = "الإخلاص",
            ayahNo = 1,
            audioUrl = "https://server13.mp3quran.net/husr/112.mp3",
            durationMs = 30000L
        )
        else -> TrackItem(
            reciterSlug = "mishary",
            reciterName = "Mishary Rashid Alafasy",
            surahId = 1,
            surahNameEn = playlist.title,
            surahNameAr = "الفاتحة",
            ayahNo = 1,
            audioUrl = "https://server8.mp3quran.net/afs/001.mp3",
            durationMs = 60000L
        )
    }
    AudioEngine.playTrack(track)
}
