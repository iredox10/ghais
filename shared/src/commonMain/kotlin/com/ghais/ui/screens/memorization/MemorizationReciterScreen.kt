package com.ghais.ui.screens.memorization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Screen displaying an EveryAyah reciter's catalog optimized for memorization (Hifz).
 * Selecting any Surah launches authentic Ayah-by-Ayah recitation with real-time text
 * display and direct access to NowPlayingScreen with Ayah mode toggling.
 */
data class MemorizationReciterScreen(val reciterSlug: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val reciter: Reciter = remember(reciterSlug) {
            QuranDataRepository.getReciterBySlug(reciterSlug)
        }
        val everyAyahMeta = remember(reciterSlug) {
            EveryAyahReciters.findBySlug(reciterSlug)
        }
        val allSurahs: List<Surah> = remember {
            QuranDataRepository.getSurahs()
        }

        var selectedFilter by remember { mutableStateOf("All") }
        val filterOptions = listOf("All", "Juz 'Amma", "Essential Hifz", "Meccan", "Medinan")

        val filteredSurahs = remember(selectedFilter, allSurahs) {
            when (selectedFilter) {
                "Juz 'Amma" -> allSurahs.filter { it.id in 78..114 }
                "Essential Hifz" -> allSurahs.filter { it.id in listOf(1, 18, 36, 55, 56, 67, 112, 113, 114) }
                "Meccan" -> allSurahs.filter { it.revelationType.equals("Meccan", ignoreCase = true) }
                "Medinan" -> allSurahs.filter { it.revelationType.equals("Medinan", ignoreCase = true) }
                else -> allSurahs
            }
        }

        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isAyahMode by AudioEngine.isAyahMode.collectAsState()

        fun playSurahInAyahMode(surahId: Int) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator.push(NowPlayingScreen())
        }

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = { navigator.pop() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Fill2)
                                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    // Header Hero Section
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "VERIFIED EVERYAYAH QARI",
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.2.sp
                                    )
                                }

                                if (everyAyahMeta?.isTeacher == true) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(GhaisNoir.Fill3)
                                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "TEACHER / MU'ALLIM",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Memorize with",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = reciter.nameEn,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Hero Card with Avatar & Actions
                            NoirHeroCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NoirReciterAvatar(
                                        photoUrl = photoForSlug(reciter.slug),
                                        nameEn = reciter.nameEn,
                                        size = 80.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ring = true
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reciter.nameAr,
                                            style = GhaisTypography.arabicBody,
                                            fontFamily = GhaisTypography.quranFont,
                                            fontSize = 20.sp,
                                            color = GhaisNoir.TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${everyAyahMeta?.style ?: reciter.style} • ${everyAyahMeta?.tempo ?: "Steady"}",
                                            color = GhaisNoir.TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = everyAyahMeta?.audioFolder ?: reciter.audioFolder,
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (!everyAyahMeta?.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = everyAyahMeta.description,
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ChromePillButton(
                                        text = "Start Ayah Playback",
                                        leadingIcon = Icons.Filled.PlayArrow,
                                        modifier = Modifier.weight(1f),
                                        onClick = { playSurahInAyahMode(1) }
                                    )

                                    GhostPillButton(
                                        text = "Juz 'Amma",
                                        onClick = { playSurahInAyahMode(78) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Filter Chips Row
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(filterOptions) { filter ->
                                    val isSelected = selectedFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else GhaisNoir.Fill2)
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else GhaisNoir.BorderCard,
                                                CircleShape
                                            )
                                            .noirClickable { selectedFilter = filter }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = filter,
                                            color = if (isSelected) Color.Black else GhaisNoir.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            NoirSectionHeader(
                                label = "Surahs for Memorization",
                                actionLabel = "${filteredSurahs.size} Surahs",
                                onAction = {}
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Surahs List
                    items(
                        items = filteredSurahs,
                        key = { it.id }
                    ) { surah ->
                        val isPlayingThisSurah = currentTrack?.surahId == surah.id &&
                            currentTrack?.reciterSlug == reciter.slug &&
                            isAyahMode

                        NoirListRow(
                            title = "${surah.id}. ${surah.nameEn}",
                            subtitle = "${surah.nameAr} • ${surah.ayahsCount} Ayahs • ${surah.revelationType}",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            chevron = false,
                            onClick = { playSurahInAyahMode(surah.id) },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlayingThisSurah) Color.White else GhaisNoir.Fill2)
                                        .border(
                                            1.dp,
                                            if (isPlayingThisSurah) Color.White else GhaisNoir.BorderCard,
                                            CircleShape
                                        )
                                        .noirClickable { playSurahInAyahMode(surah.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play Surah ${surah.nameEn}",
                                        tint = if (isPlayingThisSurah) Color.Black else GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
