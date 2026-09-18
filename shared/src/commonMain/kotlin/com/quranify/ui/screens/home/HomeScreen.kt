package com.quranify.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.quranify.data.seed.StitchAssets
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.screens.home.components.CuratedForPeaceSection
import com.quranify.ui.screens.home.components.DhikrStreakCard
import com.quranify.ui.screens.home.components.JumpBackInSection
import com.quranify.ui.screens.home.components.RecitationByMoodSection
import com.quranify.ui.screens.home.components.VerifiedRecitersSection
import com.quranify.ui.theme.QuranifyColors

// Trending Purple & Pitch Black Palette Tokens
private val PitchBlackBg = Color(0xFF0B0C0E)
private val ElectricViolet = Color(0xFFA855F7)
private val TrendingPurple = Color(0xFF8B5CF6)

object HomeScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 0u,
                title = "Home",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBg)
        ) {
            // Ambient Top Aura: Electric Violet / Trending Purple fading smoothly into deep pitch black
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.22f),
                                TrendingPurple.copy(alpha = 0.10f),
                                PitchBlackBg.copy(alpha = 0.0f)
                            ),
                            center = Offset(300f, -50f),
                            radius = 500f
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item {
                    HomeTopBar()
                }
                item {
                    DhikrStreakCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                item {
                    JumpBackInSection(
                        onCardClick = { title ->
                            AudioEngine.playTrack(
                                TrackItem(
                                    reciterSlug = "mishary_alafasy",
                                    reciterName = "Mishary Alafasy",
                                    surahId = 67,
                                    surahNameEn = title,
                                    surahNameAr = "الملك",
                                    ayahNo = 1,
                                    audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5241.mp3",
                                    durationMs = 240000L
                                )
                            )
                        }
                    )
                }
                item {
                    VerifiedRecitersSection()
                }
                item {
                    CuratedForPeaceSection(
                        onPlayClick = { playlistId ->
                            AudioEngine.playTrack(
                                TrackItem(
                                    reciterSlug = "mishary_alafasy",
                                    reciterName = "Mishary Alafasy",
                                    surahId = 1,
                                    surahNameEn = "Surah Al-Fatiha",
                                    surahNameAr = "الفاتحة",
                                    ayahNo = 1,
                                    audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3",
                                    durationMs = 45000L
                                )
                            )
                        }
                    )
                }
                item {
                    RecitationByMoodSection()
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: App Logo Badge + Greeting & Screen Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Frosted black glass logo container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .border(
                        width = 1.dp,
                        color = Color(0x33A855F7),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = StitchAssets.LogoUrl,
                    contentDescription = "Quranify Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "ASSALAMU ALAIKUM",
                    color = ElectricViolet,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Home",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Right: Frosted Search Icon Pill + Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Frosted search circular pill
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape
                    )
                    .clickable { /* TODO: Search */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Profile avatar with subtle purple glowing ring
            AsyncImage(
                model = StitchAssets.ProfileAvatarUrl,
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, ElectricViolet.copy(alpha = 0.40f), CircleShape)
                    .background(QuranifyColors.SurfaceHigh)
            )
        }
    }
}
