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
import com.ghais.data.seed.EveryAyahReciter
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirHeroCard
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

        fun playReciterAyahMode(reciter: EveryAyahReciter, surahId: Int = 1) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
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

                        Spacer(Modifier.height(16.dp))

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
