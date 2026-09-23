package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

data class RegionRecitersScreen(val nation: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val followedSlugs by FollowStore.followedSlugs.collectAsState()

        val reciters = remember(nation) {
            QuranData.RECITERS.filter { it.country == nation }
        }
        val filtered = remember(reciters, searchQuery) {
            if (searchQuery.isBlank()) reciters
            else reciters.filter {
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                    it.nameAr.contains(searchQuery, ignoreCase = true)
            }
        }

        NoirScreenRoot {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar — back well + nation title + mono count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                            .noirClickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = nation,
                            color = GhaisNoir.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filtered.size} reciters",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Region chip strip — single active chrome pill (selected region).
                // Single-region contract preserved: no multi-region switching.
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "region_$nation") {
                        RegionChromePill(
                            text = nation,
                            active = true,
                            onClick = {}
                        )
                    }
                    item(key = "region_count") {
                        Box(
                            modifier = Modifier
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${filtered.size}",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Engraved search pill (carved-in recessed surface)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.insetFill())
                        .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = GhaisNoir.TextPrimary,
                            fontSize = 14.sp
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search $nation reciters...",
                                    color = GhaisNoir.TextDisabled,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No reciters found for \"$searchQuery\"" else "No reciters in $nation",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try a different spelling",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "header_$nation") {
                            NoirSectionHeader(
                                label = "Reciters",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(
                            items = filtered,
                            key = { it.slug }
                        ) { reciter ->
                            val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug
                            val activelyPlaying = isCurrentReciter && isPlaying

                            // NoirListRow recipe: soft card fill + ghost border +
                            // bright TOP-ONLY specular, monochrome state via fill.
                            var rowModifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(
                                    if (activelyPlaying) GhaisNoir.cardFillActive()
                                    else GhaisNoir.cardFillSoft()
                                )
                                .border(
                                    1.dp,
                                    if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    GhaisShapes.row
                                )
                                .topSpecular(inset = 22.dp)
                            rowModifier = rowModifier.noirClickable {
                                navigator.push(ReciterProfileScreen(reciter.slug))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = rowModifier.padding(12.dp)
                            ) {
                                // Grayscale artwork well — engraved tile, dimmed scrim
                                val photo = photoForSlug(reciter.slug)
                                if (photo != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(GhaisNoir.wellFill())
                                            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = photo,
                                            contentDescription = reciter.nameEn,
                                            contentScale = ContentScale.Crop,
                                            colorFilter = NoirGrayscale,
                                            modifier = Modifier.matchParentSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.35f))
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(GhaisNoir.wellFill())
                                            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reciter.nameEn.take(1),
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reciter.nameEn,
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${reciter.riwayah} • ${reciter.style}",
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = reciter.nameAr,
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Play / Pause — chrome disc when active, clay well otherwise
                                if (activelyPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(GhaisNoir.chromeFill())
                                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            .clickable { AudioEngine.pause() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Pause,
                                            contentDescription = "Pause",
                                            tint = GhaisNoir.OnChrome,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(GhaisNoir.wellFill())
                                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                            .clickable {
                                                if (isCurrentReciter) {
                                                    AudioEngine.resume()
                                                } else {
                                                    val surahs = QuranDataRepository.getSurahsForReciter(reciter)
                                                        .filter { reciter.isSurahAvailable(it.id) }
                                                    val tracks = surahs.map { surah ->
                                                        TrackItem(
                                                            reciterSlug = reciter.slug,
                                                            reciterName = reciter.nameEn,
                                                            surahId = surah.id,
                                                            surahNameEn = surah.nameEn,
                                                            surahNameAr = surah.nameAr,
                                                            ayahNo = 0,
                                                            audioUrl = reciter.getFullSurahUrl(surah.id),
                                                            textUthmani = "",
                                                            durationMs = surah.ayahsCount * 15_000L
                                                        )
                                                    }
                                                    if (tracks.isNotEmpty()) {
                                                        AudioEngine.playQueue(tracks, startIndex = 0)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = GhaisNoir.TextPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Follow toggle — monochrome only, no emerald
                                val isFollowing = reciter.slug in followedSlugs
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFollowing) GhaisNoir.Fill4 else GhaisNoir.Fill1
                                        )
                                        .border(
                                            1.dp,
                                            if (isFollowing) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                                            CircleShape
                                        )
                                        .clickable { FollowStore.toggle(reciter.slug) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                                        contentDescription = if (isFollowing) "Following" else "Follow",
                                        tint = if (isFollowing) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
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
}

/**
 * Chrome pill for the region strip — white chrome gradient when active,
 * ghost wash otherwise. Monochrome only.
 */
@Composable
private fun RegionChromePill(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    if (active) {
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.chromeFill())
                .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                .noirClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = GhaisNoir.OnChrome,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                .noirClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
