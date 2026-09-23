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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.EveryAyahReciter
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.UNKNOWN_DURATION_MS
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Memorization (Hifz & Tajweed) Tab.
 *
 * Dedicated tab showcasing reciters verified to have authentic, individual Ayah audio
 * recordings from EveryAyah. Allows users to listen to Ayah-by-Ayah recitations, repeat
 * verses for memorization, and seamlessly switch to NowPlayingScreen with the Ayah mode toggle.
 */
object MemorizationScreen : Tab {

    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Hifz",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current

        var searchQuery by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }

        val categories = listOf("All", "Teacher (Mu'allim)", "Murattal", "Mujawwad", "Haramain", "Slow Tempo")

        val reciters = remember(searchQuery, selectedCategory) {
            EveryAyahReciters.ALL.filter { reciter ->
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim().lowercase()
                    reciter.nameEn.lowercase().contains(q) ||
                    reciter.nameAr.contains(q) ||
                    reciter.country.lowercase().contains(q) ||
                    reciter.style.lowercase().contains(q)
                }

                val matchesCategory = when (selectedCategory) {
                    "Teacher (Mu'allim)" -> reciter.isTeacher || reciter.style.contains("Teacher", ignoreCase = true)
                    "Murattal" -> reciter.style.contains("Murattal", ignoreCase = true)
                    "Mujawwad" -> reciter.style.contains("Mujawwad", ignoreCase = true)
                    "Haramain" -> reciter.style.contains("Haramain", ignoreCase = true)
                    "Slow Tempo" -> reciter.tempo.contains("Slow", ignoreCase = true) || reciter.tempo.contains("Measured", ignoreCase = true)
                    else -> true
                }

                matchesQuery && matchesCategory
            }
        }

        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isAyahMode by AudioEngine.isAyahMode.collectAsState()

        val dailyGoalCount by HifzMasteryStore.dailyGoalCount.collectAsState()
        val todayReviewedCount by HifzMasteryStore.todayReviewedCount.collectAsState()
        val streakDays by HifzMasteryStore.streakDays.collectAsState()
        val totalMasteredCount by HifzMasteryStore.totalMasteredCount.collectAsState()
        val weakAyahs by HifzMasteryStore.weakAyahs.collectAsState()

        fun playReciterAyahMode(reciter: EveryAyahReciter, surahId: Int = 1) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator?.push(NowPlayingScreen())
        }

        fun reviewWeakAyahs() {
            val weak = weakAyahs.ifEmpty {
                listOf(1 to 6, 1 to 7, 112 to 4, 113 to 3, 18 to 4)
            }
            val targetSlug = currentTrack?.reciterSlug ?: "husary-muallim"
            val reciter = QuranDataRepository.getReciterBySlug(targetSlug)

            val weakTracks = weak.map { (sId, aNo) ->
                val surah = QuranDataRepository.getSurahById(sId)
                val surahNameEn = surah?.nameEn ?: "Surah $sId"
                val surahNameAr = surah?.nameAr ?: ""
                val ayahVerse = QuranAyahRepository.getAyahImmediate(sId, aNo)
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn.ifBlank { "Al-Husary (Teacher)" },
                    surahId = sId,
                    surahNameEn = surahNameEn,
                    surahNameAr = surahNameAr,
                    ayahNo = aNo,
                    audioUrl = reciter.getAyahAudioUrl(sId, aNo),
                    textUthmani = ayahVerse.textUthmani,
                    durationMs = UNKNOWN_DURATION_MS
                )
            }

            AudioEngine.playQueue(tracks = weakTracks, startIndex = 0)
            rootNavigator?.push(NowPlayingScreen())
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp)
            ) {
                // Header & Title
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MEMORIZATION (HIFZ)",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.3.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(GhaisNoir.Fill2)
                                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${EveryAyahReciters.ALL.size} Verified Qaris",
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Learn & Memorize",
                            style = GhaisTypography.displayEditorial,
                            maxLines = 1
                        )
                        Text(
                            text = "Ayah by Ayah",
                            style = GhaisTypography.displayEditorialBold,
                            maxLines = 1
                        )

                        Spacer(Modifier.height(18.dp))

                        // 1. Header Card: Daily Hifz Goal, Streak Counter, Mastered Count
                        HifzGoalHeaderCard(
                            dailyGoalCurrent = todayReviewedCount,
                            dailyGoalTotal = dailyGoalCount,
                            streakDays = streakDays,
                            totalMastered = totalMasteredCount
                        )

                        Spacer(Modifier.height(16.dp))

                        // 2. Quick Actions
                        NoirSectionHeader(
                            label = "Quick Actions",
                            actionLabel = if (weakAyahs.isNotEmpty()) "${weakAyahs.size} weak ayahs" else "5 seed ayahs",
                            onAction = { reviewWeakAyahs() }
                        )

                        Spacer(Modifier.height(8.dp))

                        // Quick Action A: "Review Weak Ayahs" button
                        ReviewWeakAyahsActionCard(
                            weakCount = weakAyahs.size.takeIf { it > 0 } ?: 5,
                            onClick = { reviewWeakAyahs() }
                        )

                        Spacer(Modifier.height(12.dp))

                        // Quick Action B: "Hifz Practice" banner
                        HifzPracticeBanner(
                            onStartPractice = {
                                val teacherQari = EveryAyahReciters.ALL.firstOrNull { it.slug == "husary-muallim" }
                                    ?: EveryAyahReciters.ALL.first()
                                playReciterAyahMode(teacherQari, 1)
                            }
                        )

                        Spacer(Modifier.height(18.dp))

                        // Engraved Search Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search EveryAyah reciters, styles, countries...",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Categories Horizontal Scroll
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = selectedCategory == category
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else GhaisNoir.Fill2)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.White else GhaisNoir.BorderCard,
                                            CircleShape
                                        )
                                        .noirClickable { selectedCategory = category }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = category,
                                            color = if (isSelected) Color.Black else GhaisNoir.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Featured Teacher Edition Hero Plate (Only when query is empty)
                        if (searchQuery.isBlank() && selectedCategory == "All") {
                            val teacherQari = EveryAyahReciters.ALL.firstOrNull { it.slug == "husary-muallim" }
                            if (teacherQari != null) {
                                NoirHeroCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(GhaisNoir.Fill3)
                                                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.School,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TEACHER / MU'ALLIM MODE",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Al-Husary (Teacher)",
                                                style = GhaisTypography.displayEditorialBold,
                                                fontSize = 18.sp,
                                                color = GhaisNoir.TextPrimary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Pauses after each verse for student repetition.",
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    ChromePillButton(
                                        text = "Start Al-Fatihah with Teacher",
                                        leadingIcon = Icons.Filled.PlayArrow,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { playReciterAyahMode(teacherQari, 1) }
                                    )
                                }

                                Spacer(Modifier.height(20.dp))
                            }
                        }

                        // Section Header
                        NoirSectionHeader(
                            label = "EveryAyah Reciters",
                            actionLabel = "${reciters.size} available",
                            onAction = {}
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Reciters List
                if (reciters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No EveryAyah reciters found matching \"$searchQuery\"",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = reciters,
                        key = { it.slug }
                    ) { reciter ->
                        val isPlayingThisReciter = currentTrack?.reciterSlug == reciter.slug && isAyahMode

                        MemorizationReciterCard(
                            reciter = reciter,
                            isPlaying = isPlayingThisReciter,
                            onPlayClick = { playReciterAyahMode(reciter, 1) },
                            onCardClick = { rootNavigator?.push(MemorizationReciterScreen(reciter.slug)) }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * Noir Carbon Glass card for an EveryAyah reciter in the Memorization tab.
 */
@Composable
private fun MemorizationReciterCard(
    reciter: EveryAyahReciter,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirReciterAvatar(
                photoUrl = photoForSlug(reciter.slug),
                nameEn = reciter.nameEn,
                size = 58.dp,
                shape = RoundedCornerShape(16.dp),
                ring = isPlaying
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = reciter.nameEn,
                        color = if (isPlaying) Color.White else GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (reciter.isTeacher) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill3)
                                .border(0.5.dp, GhaisNoir.BorderCard, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TEACHER",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = reciter.nameAr,
                    style = GhaisTypography.arabicBody,
                    fontFamily = GhaisTypography.quranFont,
                    fontSize = 15.sp,
                    color = GhaisNoir.TextSecondary
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${reciter.style} • ${reciter.tempo}",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Quick Play Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color.White else GhaisNoir.Fill2)
                    .border(
                        1.dp,
                        if (isPlaying) Color.White else GhaisNoir.BorderCard,
                        CircleShape
                    )
                    .noirClickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${reciter.nameEn} in Ayah Mode",
                    tint = if (isPlaying) Color.Black else GhaisNoir.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HifzGoalHeaderCard(
    dailyGoalCurrent: Int,
    dailyGoalTotal: Int,
    streakDays: Int,
    totalMastered: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (dailyGoalTotal > 0) {
        (dailyGoalCurrent.toFloat() / dailyGoalTotal.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percent = (progress * 100).toInt()

    NoirHeroCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Section Tag & Streak Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "DAILY HIFZ PROGRESS",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                // Streak counter: "🔥 4 Day Streak"
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$streakDays Day Streak",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Middle: Daily Hifz Goal text & Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Daily Goal: $dailyGoalCurrent / $dailyGoalTotal Ayahs",
                        style = GhaisTypography.headlineSmall.copy(
                            color = GhaisNoir.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(Modifier.height(3.dp))
                    val remaining = (dailyGoalTotal - dailyGoalCurrent).coerceAtLeast(0)
                    Text(
                        text = if (remaining > 0) "$remaining more to complete today's target" else "Target completed for today!",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GhaisNoir.Fill3)
                        .border(0.5.dp, GhaisNoir.BorderGhost, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percent%",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Engraved Segmented Progress Bar
            NoirSegmentedProgress(
                progress = progress,
                trackHeight = 8.dp
            )

            Spacer(Modifier.height(14.dp))

            // Separator hairline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GhaisNoir.BorderGhost)
            )

            Spacer(Modifier.height(12.dp))

            // Bottom Metrics: Total Mastered count and Retention rate
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
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "$totalMastered Verses Mastered",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Retention: 98.4%",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ReviewWeakAyahsActionCard(
    weakCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoirCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconWell(
                    icon = Icons.Filled.Repeat,
                    size = 46.dp,
                    iconSize = 22.dp,
                    tint = GhaisNoir.TextPrimary
                )
                if (weakCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .background(GhaisNoir.chromeFill(), CircleShape)
                            .border(1.5.dp, GhaisNoir.NoirBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weakCount.toString(),
                            color = GhaisNoir.OnChrome,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Review Weak Ayahs",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill3)
                            .border(0.5.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REVIEW_NEEDED",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (weakCount > 0) {
                        "$weakCount verses need repetition • Tap to listen & review"
                    } else {
                        "All verses up to date • Excellent retention!"
                    },
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Weak Ayahs",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HifzPracticeBanner(
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoirHeroCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onStartPractice
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconWell(
                    icon = Icons.Filled.School,
                    size = 52.dp,
                    iconSize = 26.dp,
                    tint = GhaisNoir.TextPrimary
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "INTERACTIVE MODE",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Hifz Practice",
                        style = GhaisTypography.displayEditorialBold,
                        fontSize = 19.sp,
                        color = GhaisNoir.TextPrimary
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Continuous verse loop with teacher pauses for immediate recitation & retention.",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            ChromePillButton(
                text = "Start Hifz Practice Session",
                leadingIcon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartPractice
            )
        }
    }
}

