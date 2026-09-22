package com.ghais.ui.screens.reciters

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Phase 4 — Noir Reciters.
 *
 * Strict Noir Glass monochrome on the shared canvas (glow zone -> #050506 +
 * ambient glow + film grain). Order, grouping, search filter and navigation
 * targets are unchanged; only the surface language changed: zero accent hue,
 * alpha-white glass cards, top-only specular hairlines, chrome pills and
 * grayscale artwork. State reads through fill elevation, weight and opacity.
 */
class RecitersScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Reciters",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        // Push onto the ROOT navigator: the local one here is a TabNavigator,
        // which can only render Tabs (pushing a plain Screen crashes in CurrentTab).
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current
        var searchQuery by remember { mutableStateOf("") }

        val groups = remember(searchQuery) {
            QuranData.RECITERS
                .filter {
                    searchQuery.isBlank() ||
                        it.nameEn.contains(searchQuery, ignoreCase = true) ||
                        it.nameAr.contains(searchQuery, ignoreCase = true) ||
                        it.country.contains(searchQuery, ignoreCase = true)
                }
                .groupBy { it.country.ifBlank { "Other" } }
                .entries
                .sortedByDescending { (_, reciters) -> reciters.size }
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp)
            ) {
                // ---------------------------------------------------------
                // Editorial header + engraved search
                // ---------------------------------------------------------
                item {
                    NoirRecitersHeader(
                        totalReciters = QuranData.RECITERS.size,
                        nationCount = groups.size
                    )
                    Spacer(Modifier.height(16.dp))
                    NoirRecitersSearch(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                if (groups.isEmpty()) {
                    item {
                        NoirRecitersEmpty(query = searchQuery)
                    }
                } else {
                    groups.forEachIndexed { index, (nation, reciters) ->
                        item(key = "noir_reel_$nation") {
                            NoirNationReel(
                                nation = nation,
                                reciters = reciters,
                                photoFor = ::photoForSlug,
                                onSeeAll = { rootNavigator?.push(RegionRecitersScreen(nation)) },
                                onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) },
                                onPlayReciter = { reciter ->
                                    val surahs = QuranDataRepository.getSurahsForReciter(reciter)
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
                            )
                            if (index < groups.size - 1) {
                                Spacer(Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(GhaisNoir.BorderGhost)
                                )
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private val NoirReelCardShape = RoundedCornerShape(28.dp)
private val NoirAvatarShape = RoundedCornerShape(24.dp)

/**
 * Editorial two-line headline ("Browse" light / "Reciters" bold) matching the
 * Home "Return to / Your Quran" treatment, with ghost count chips.
 */
@Composable
private fun NoirRecitersHeader(
    totalReciters: Int,
    nationCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Browse",
            style = GhaisTypography.displayEditorial,
            maxLines = 1
        )
        Text(
            text = "Reciters",
            style = GhaisTypography.displayEditorialBold,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Voices of the revelation",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoirRecitersChip(
                text = if (totalReciters == 1) "1 reciter" else "$totalReciters reciters"
            )
            NoirRecitersChip(
                text = if (nationCount == 1) "1 nation" else "$nationCount nations"
            )
        }
    }
}

/** Non-interactive ghost count chip — informational, no press affordance. */
@Composable
private fun NoirRecitersChip(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Engraved search field — carved-in recessed surface (dark inset fill +
 * ~5% rim), search glyph at 38%, typed text at 100%, hint at 24%.
 */
@Composable
private fun NoirRecitersSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            singleLine = true,
            textStyle = TextStyle(
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp
            ),
            decorationBox = { inner ->
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search reciters…",
                        color = GhaisNoir.TextDisabled,
                        fontSize = 15.sp
                    )
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoirNationReel(
    nation: String,
    reciters: List<Reciter>,
    photoFor: (String) -> String?,
    onSeeAll: () -> Unit,
    onReciter: (String) -> Unit,
    onPlayReciter: (Reciter) -> Unit = {}
) {
    val followedSlugs by FollowStore.followedSlugs.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        NoirSectionHeader(
            label = "$nation • ${reciters.size}",
            actionLabel = "See all",
            onAction = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = reciters,
                key = { it.slug }
            ) { reciter ->
                NoirReciterCard(
                    reciter = reciter,
                    photoUrl = photoFor(reciter.slug),
                    onClick = { onReciter(reciter.slug) },
                    onPlayClick = { onPlayReciter(reciter) },
                    isFollowing = reciter.slug in followedSlugs,
                    onFollowClick = { FollowStore.toggle(reciter.slug) }
                )
            }
        }
    }
}

/**
 * Noir reel card: soft glass fill + 1px ghost border + top-only specular.
 * Active (now-playing) state reads through stronger wash + bright hairline,
 * never hue. Artwork is grayscale under a darkening scrim; monogram fallback
 * when there is no photo.
 */
@Composable
private fun NoirReciterCard(
    reciter: Reciter,
    photoUrl: String?,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {}
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug
    val activelyPlaying = isCurrentReciter && isPlaying

    val fill = if (activelyPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(254.dp)
            .clip(NoirReelCardShape)
            .background(fill)
            .border(1.dp, border, NoirReelCardShape)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        // Sheen sweep + bottom scrim for depth (monochrome only).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.32f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(NoirAvatarShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, NoirAvatarShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = reciter.nameEn,
                            contentScale = ContentScale.Crop,
                            colorFilter = NoirReciterGrayscale,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Darkening scrim: keeps the plate recessed, never glowing.
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(NoirAvatarShape)
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, NoirAvatarShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reciter.nameEn.take(1),
                            color = GhaisNoir.TextPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play / pause pill: chrome when actively playing, smoked glass otherwise.
                val playModifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (activelyPlaying) Modifier.background(GhaisNoir.chromeFill())
                        else Modifier.background(Color.Black.copy(alpha = 0.70f))
                    )
                Box(
                    modifier = playModifier
                        .border(
                            1.dp,
                            if (activelyPlaying) Color.White.copy(alpha = 0.35f)
                            else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable {
                            if (activelyPlaying) {
                                AudioEngine.pause()
                            } else if (isCurrentReciter) {
                                AudioEngine.resume()
                            } else {
                                onPlayClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (activelyPlaying) "Pause" else "Play",
                        tint = if (activelyPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Follow badge — clay well; following reads via bright glyph +
                // chromium dot, unfollowed via 38% glyph. No hue anywhere.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                        .noirClickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                            contentDescription = if (isFollowing) "Following" else "Follow",
                            tint = if (isFollowing) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (isFollowing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GhaisNoir.chromeFill())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 44.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${reciter.riwayah} • ${reciter.style}",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Ghost empty state — muted mosque well + tertiary copy, no hue. */
@Composable
private fun NoirRecitersEmpty(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirReelCardShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirReelCardShape)
            .topSpecular()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(GhaisNoir.wellFill(), GhaisShapes.well)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mosque,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No reciters found",
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (query.isBlank()) "Try a different nation" else "Nothing matches “$query”",
            color = GhaisNoir.TextTertiary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
