package com.ghais.ui.screens.explore.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.CuratedPlaylist
import com.ghais.data.seed.DetailedCuratedPlaylist
import com.ghais.data.seed.DetailedReciter
import com.ghais.data.seed.StitchAssets
import com.ghais.data.seed.toTrackItem
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.screens.playlists.MoodPlaylist

/**
 * Variants supported by [ExploreModularCard]:
 * - [HERO_WIDE]: Spans 2 grid columns (or full width). Horizontal layout with 84.dp chunky avatar.
 * - [GRID]: Spans 1 grid column. Compact vertical stack with 76.dp chunky avatar.
 */
enum class ExploreCardVariant {
    HERO_WIDE,
    GRID
}

/**
 * Data model representing a modular explore collection or playlist item.
 */
data class ExploreCardItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // e.g. "FOCUS", "SLEEP", "WORK", "HEALING"
    val avatarUrl: String,
    val surahCountText: String, // e.g. "6 Surahs • Tartil"
    val accentColor: Color = Color(0xFFD4A853), // Halo & badge tint
    val tracks: List<TrackItem> = emptyList(),
    val surahIds: List<Int> = emptyList()
)

/**
 * ExploreModularCard:
 *
 * iOS HIG Apple-style Modular Card featuring:
 * - Prominent chunky/oversized avatar (76.dp - 84.dp) with luminous halo border and ambient aura.
 * - Glassmorphism: Frosted specular gradient border, diagonal sheen, and specular top hairline.
 * - Dark Minimalist UI: Layered dark surfaces (#141418), soft off-white titles, muted metadata.
 * - Frosted glass play button that immediately starts full-surah queue playback.
 * - Category pill badge and Surah count chip.
 * - Supports Hero Wide (span 2) and Grid (span 1) variants.
 */
@Composable
fun ExploreModularCard(
    item: ExploreCardItem,
    modifier: Modifier = Modifier,
    variant: ExploreCardVariant = ExploreCardVariant.GRID,
    avatarShape: Shape = RoundedCornerShape(22.dp),
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: (() -> Unit)? = null
) {
    // Observe global AudioEngine state to dynamically reflect playback status
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isEnginePlaying by AudioEngine.isPlaying.collectAsState()

    val isCurrentTrackInThisItem = remember(currentTrack, item) {
        val curr = currentTrack
        if (curr == null) false
        else if (item.tracks.isNotEmpty()) item.tracks.any { it.audioUrl == curr.audioUrl }
        else if (item.surahIds.isNotEmpty()) curr.surahId in item.surahIds
        else false
    }

    val activelyPlaying = isPlaying || (isEnginePlaying && isCurrentTrackInThisItem)

    val handlePlayClick: () -> Unit = {
        if (onPlayClick != null) {
            onPlayClick()
        } else {
            if (activelyPlaying) {
                AudioEngine.pause()
            } else {
                val tracksToPlay = when {
                    item.tracks.isNotEmpty() -> item.tracks
                    item.surahIds.isNotEmpty() -> buildTracksFromSurahIds(item.surahIds)
                    else -> emptyList()
                }
                if (tracksToPlay.isNotEmpty()) {
                    AudioEngine.playQueue(tracksToPlay, startIndex = 0)
                }
            }
        }
    }

    ExploreModularCardContent(
        title = item.title,
        description = item.description,
        category = item.category,
        avatarUrl = item.avatarUrl,
        surahCountText = item.surahCountText,
        modifier = modifier,
        variant = variant,
        accentColor = item.accentColor,
        avatarShape = avatarShape,
        isPlaying = activelyPlaying,
        onClick = onClick,
        onPlayClick = handlePlayClick
    )
}

/**
 * Direct parameter overload for [ExploreModularCard].
 */
@Composable
fun ExploreModularCard(
    title: String,
    description: String,
    category: String,
    avatarUrl: String,
    surahCountText: String,
    modifier: Modifier = Modifier,
    variant: ExploreCardVariant = ExploreCardVariant.GRID,
    accentColor: Color = Color(0xFFD4A853),
    avatarShape: Shape = RoundedCornerShape(22.dp),
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    ExploreModularCardContent(
        title = title,
        description = description,
        category = category,
        avatarUrl = avatarUrl,
        surahCountText = surahCountText,
        modifier = modifier,
        variant = variant,
        accentColor = accentColor,
        avatarShape = avatarShape,
        isPlaying = isPlaying,
        onClick = onClick,
        onPlayClick = onPlayClick
    )
}

/**
 * Convenience composable for the Hero Wide (span 2) variant.
 */
@Composable
fun ExploreHeroModularCard(
    title: String,
    description: String,
    category: String,
    avatarUrl: String,
    surahCountText: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFD4A853),
    avatarShape: Shape = RoundedCornerShape(22.dp),
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    ExploreModularCard(
        title = title,
        description = description,
        category = category,
        avatarUrl = avatarUrl,
        surahCountText = surahCountText,
        modifier = modifier,
        variant = ExploreCardVariant.HERO_WIDE,
        accentColor = accentColor,
        avatarShape = avatarShape,
        isPlaying = isPlaying,
        onClick = onClick,
        onPlayClick = onPlayClick
    )
}

/**
 * Convenience composable for the Grid (span 1) variant.
 */
@Composable
fun ExploreGridModularCard(
    title: String,
    description: String,
    category: String,
    avatarUrl: String,
    surahCountText: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFD4A853),
    avatarShape: Shape = RoundedCornerShape(22.dp),
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    ExploreModularCard(
        title = title,
        description = description,
        category = category,
        avatarUrl = avatarUrl,
        surahCountText = surahCountText,
        modifier = modifier,
        variant = ExploreCardVariant.GRID,
        accentColor = accentColor,
        avatarShape = avatarShape,
        isPlaying = isPlaying,
        onClick = onClick,
        onPlayClick = onPlayClick
    )
}

/**
 * Internal rendering implementation for both Hero Wide and Grid card variants.
 */
@Composable
private fun ExploreModularCardContent(
    title: String,
    description: String,
    category: String,
    avatarUrl: String,
    surahCountText: String,
    modifier: Modifier = Modifier,
    variant: ExploreCardVariant = ExploreCardVariant.GRID,
    accentColor: Color = Color(0xFFD4A853),
    avatarShape: Shape = RoundedCornerShape(22.dp),
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(24.dp)

    // Dark Minimalist + Translucent Base Layer with Specular Border
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xFF141418))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = cardShape
            )
            .clickable(onClick = onClick)
    ) {
        // Subtle Frosted Glass Translucent Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.035f))
        )

        // Subtle Diagonal Light Sheen Overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
        )

        // Top Specular Hairline Inside Card Edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 24.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Content Layout according to Variant
        when (variant) {
            ExploreCardVariant.HERO_WIDE -> {
                HeroWideLayout(
                    title = title,
                    description = description,
                    category = category,
                    avatarUrl = avatarUrl,
                    surahCountText = surahCountText,
                    accentColor = accentColor,
                    avatarShape = avatarShape,
                    isPlaying = isPlaying,
                    onPlayClick = onPlayClick
                )
            }
            ExploreCardVariant.GRID -> {
                GridLayout(
                    title = title,
                    description = description,
                    category = category,
                    avatarUrl = avatarUrl,
                    surahCountText = surahCountText,
                    accentColor = accentColor,
                    avatarShape = avatarShape,
                    isPlaying = isPlaying,
                    onPlayClick = onPlayClick
                )
            }
        }
    }
}

/**
 * Hero Wide (span 2) horizontal layout with 84.dp chunky avatar.
 */
@Composable
private fun HeroWideLayout(
    title: String,
    description: String,
    category: String,
    avatarUrl: String,
    surahCountText: String,
    accentColor: Color,
    avatarShape: Shape,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chunky Oversized Avatar (84.dp) with Halo
        ExploreCardAvatar(
            avatarUrl = avatarUrl,
            title = title,
            size = 84.dp,
            accentColor = accentColor,
            avatarShape = avatarShape,
            isPlaying = isPlaying
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Content & Action details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Top Row: Category badge, Surah count chip & Frosted Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (category.isNotBlank()) {
                        ExploreCategoryBadge(
                            category = category,
                            accentColor = accentColor
                        )
                    }

                    if (surahCountText.isNotBlank()) {
                        ExploreSurahCountChip(text = surahCountText)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Frosted Glass Play Button
                ExplorePlayButton(
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    size = 42.dp,
                    onClick = onPlayClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline Title in bold 19sp
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = Color(0xFFF3F4F6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Descriptive text with 2-line clamp
            Text(
                text = description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                color = Color(0xFF9CA3AF),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Grid (span 1) vertical layout with 76.dp chunky avatar.
 */
@Composable
private fun GridLayout(
    title: String,
    description: String,
    category: String,
    avatarUrl: String,
    surahCountText: String,
    accentColor: Color,
    avatarShape: Shape,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top row: Category Badge (left) and Frosted Glass Play Button (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category.isNotBlank()) {
                ExploreCategoryBadge(
                    category = category,
                    accentColor = accentColor
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            ExplorePlayButton(
                isPlaying = isPlaying,
                accentColor = accentColor,
                size = 38.dp,
                onClick = onPlayClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prominent Chunky Avatar (76.dp) centered
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ExploreCardAvatar(
                avatarUrl = avatarUrl,
                title = title,
                size = 76.dp,
                accentColor = accentColor,
                avatarShape = avatarShape,
                isPlaying = isPlaying
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Headline Title in bold 17sp
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = Color(0xFFF3F4F6),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subheadline Descriptive text with 2-line clamp
        Text(
            text = description,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.5.sp,
            color = Color(0xFF9CA3AF),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Surah Count Chip at bottom
        if (surahCountText.isNotBlank()) {
            ExploreSurahCountChip(text = surahCountText)
        }
    }
}

/**
 * Chunky / Oversized Avatar with luminous glowing halo border.
 * When actively playing, the halo gently breathes with a luminous animation.
 */
@Composable
private fun ExploreCardAvatar(
    avatarUrl: String,
    title: String,
    size: Dp,
    accentColor: Color,
    avatarShape: Shape,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Subtle breathing halo pulse when actively playing
    val haloPulseAlpha = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "haloPulseTransition")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "haloPulseAlpha"
        )
        animatedAlpha
    } else {
        0.55f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer ambient glow aura
        Box(
            modifier = Modifier
                .size(size)
                .clip(avatarShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = haloPulseAlpha * 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Avatar Image Container with Glowing Halo Border
        Box(
            modifier = Modifier
                .size(size - 2.dp)
                .clip(avatarShape)
                .background(Color(0xFF191B22))
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = haloPulseAlpha),
                            accentColor.copy(alpha = haloPulseAlpha * 0.30f)
                        )
                    ),
                    shape = avatarShape
                )
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Elegant fallback placeholder with subtle gradient and book icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF222733),
                                    Color(0xFF141720)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoStories,
                        contentDescription = title,
                        tint = accentColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(size * 0.42f)
                    )
                }
            }
        }
    }
}

/**
 * Category badge pill (e.g. "FOCUS", "SLEEP", "WORK", "HEALING").
 */
@Composable
private fun ExploreCategoryBadge(
    category: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.uppercase(),
            color = accentColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
    }
}

/**
 * Surah count chip (e.g. "6 Surahs • Tartil").
 */
@Composable
private fun ExploreSurahCountChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFD1D5DB),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Frosted Glass Play Button that triggers immediate full-surah queue playback.
 */
@Composable
private fun ExplorePlayButton(
    isPlaying: Boolean,
    accentColor: Color,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF20232B).copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isPlaying) accentColor.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.40f),
                        if (isPlaying) accentColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = if (isPlaying) accentColor else Color(0xFFF3F4F6),
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * Helper to build [TrackItem]s for full-surah queue playback given a list of surah numbers.
 */
fun buildTracksFromSurahIds(
    surahIds: List<Int>,
    reciterSlug: String = "mishary"
): List<TrackItem> {
    val reciter = QuranDataRepository.getReciterBySlug(reciterSlug)
    return surahIds.mapNotNull { surahId ->
        val surah = QuranDataRepository.getSurahById(surahId) ?: return@mapNotNull null
        TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = 0,
            audioUrl = reciter.getFullSurahUrl(surah.id),
            textUthmani = "",
            durationMs = 0L
        )
    }
}

// =============================================================================
// Extension Mappers for domain and seed models
// =============================================================================

/**
 * Map [MoodPlaylist] to [ExploreCardItem].
 */
fun MoodPlaylist.toExploreCardItem(): ExploreCardItem {
    val category = when (id) {
        "study-focus" -> "FOCUS"
        "focus-work" -> "WORK"
        "sleep-mode" -> "SLEEP"
        "heart-soothing" -> "PEACE"
        "duaa-ruqia" -> "HEALING"
        "tahajjud" -> "NIGHT"
        "sunrise-barakah" -> "BARAKAH"
        "emotional" -> "REFLECTION"
        "most-beautiful" -> "TRANQUILITY"
        "favourites" -> "DEVOTION"
        else -> "QURAN"
    }

    val accent = when (id) {
        "study-focus" -> Color(0xFF4EDEA3) // Vivid Emerald
        "focus-work" -> Color(0xFF6EE7B7)  // Mint Glow
        "sleep-mode" -> Color(0xFFC084FC)  // Lavender
        "heart-soothing" -> Color(0xFF38BDF8) // Sky Blue
        "duaa-ruqia" -> Color(0xFF34D399)   // Sage Emerald
        "tahajjud" -> Color(0xFFA855F7)     // Royal Violet
        "sunrise-barakah" -> Color(0xFFF2B880) // Champagne Gold
        "emotional" -> Color(0xFFFB7185)    // Rose
        "most-beautiful" -> Color(0xFFFBBF24) // Amber
        "favourites" -> Color(0xFFD4A853)   // Gold
        else -> Color(0xFFD4A853)
    }

    val avatarUrl = when (id) {
        "study-focus" -> StitchAssets.AllCuratedPlaylists.find { it.id == "mindful-hifz" }?.coverUrl ?: StitchAssets.LibraryMorningCover
        "focus-work" -> StitchAssets.AllCuratedPlaylists.firstOrNull()?.coverUrl ?: StitchAssets.LibraryMorningCover
        "sleep-mode" -> StitchAssets.AllCuratedPlaylists.find { it.id == "tahajjud-night-qiyam" }?.coverUrl ?: StitchAssets.LibraryTahajjudCover
        "heart-soothing" -> StitchAssets.AllCuratedPlaylists.find { it.id == "anxiety-relief-inshirah" }?.coverUrl ?: StitchAssets.LibraryMorningCover
        "duaa-ruqia" -> StitchAssets.AllCuratedPlaylists.find { it.id == "ayat-ash-shifa" }?.coverUrl ?: StitchAssets.LibraryTahajjudCover
        "tahajjud" -> StitchAssets.AllCuratedPlaylists.find { it.id == "tahajjud-night-qiyam" }?.coverUrl ?: StitchAssets.LibraryTahajjudCover
        "sunrise-barakah" -> StitchAssets.AllCuratedPlaylists.find { it.id == "sunrise-barakah" }?.coverUrl ?: StitchAssets.LibraryMorningCover
        "emotional" -> StitchAssets.AllCuratedPlaylists.getOrNull(1)?.coverUrl ?: StitchAssets.LibraryMorningCover
        "most-beautiful" -> StitchAssets.AllCuratedPlaylists.find { it.id == "garden-of-tranquility" }?.coverUrl ?: StitchAssets.NowPlayingVinylArtUrl
        "favourites" -> StitchAssets.ProfileAvatarUrl
        else -> StitchAssets.LibraryMorningCover
    }

    val style = when (id) {
        "study-focus" -> "Tartil"
        "focus-work" -> "Flow"
        "sleep-mode" -> "Calm"
        "heart-soothing" -> "Solace"
        "duaa-ruqia" -> "Shifa"
        "tahajjud" -> "Qiyam"
        "sunrise-barakah" -> "Barakah"
        "emotional" -> "Mujawwad"
        "most-beautiful" -> "Murattal"
        "favourites" -> "Curated"
        else -> "Recitation"
    }

    return ExploreCardItem(
        id = id,
        title = title,
        description = description,
        category = category,
        avatarUrl = avatarUrl,
        surahCountText = "${surahIds.size} Surahs • $style",
        accentColor = accent,
        surahIds = surahIds
    )
}

/**
 * Determines whether a playlist should span across 2 columns as a Hero Wide card.
 */
fun MoodPlaylist.isWideCard(): Boolean = id == "study-focus" || id == "sleep-mode"

/**
 * Bento card wrapper that renders [MoodPlaylist] using [ExploreModularCard].
 */
@Composable
fun ExploreBentoCard(
    playlist: MoodPlaylist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isWide = playlist.isWideCard()
    val variant = if (isWide) ExploreCardVariant.HERO_WIDE else ExploreCardVariant.GRID
    ExploreModularCard(
        item = playlist.toExploreCardItem(),
        modifier = modifier,
        variant = variant,
        onClick = onClick
    )
}

/**
 * Map [DetailedCuratedPlaylist] to [ExploreCardItem].
 */
fun DetailedCuratedPlaylist.toExploreCardItem(): ExploreCardItem {
    val categoryUpper = tag.ifEmpty { "CURATED" }.uppercase()
    val accent = when (categoryUpper) {
        "FOCUS" -> Color(0xFF6EE7B7)
        "SLEEP", "NIGHT" -> Color(0xFFC084FC)
        "HEALING" -> Color(0xFF38BDF8)
        "MORNING", "BARAKAH" -> Color(0xFFF2B880)
        "PEACE", "TRANQUILITY" -> Color(0xFF4EDEA3)
        else -> Color(0xFFD4A853)
    }

    return ExploreCardItem(
        id = id,
        title = title,
        description = description.ifEmpty { subtitle },
        category = categoryUpper,
        avatarUrl = coverUrl,
        surahCountText = "${tracks.size} Surahs • ${tag.ifEmpty { "Curated" }}",
        accentColor = accent,
        tracks = tracks.map { it.toTrackItem() }
    )
}

/**
 * Map [CuratedPlaylist] to [ExploreCardItem].
 */
fun CuratedPlaylist.toExploreCardItem(): ExploreCardItem {
    val categoryUpper = (if (category.isNotEmpty()) category else tag).uppercase()
    val accent = when (categoryUpper) {
        "FOCUS" -> Color(0xFF6EE7B7)
        "SLEEP", "NIGHT" -> Color(0xFFC084FC)
        "HEALING" -> Color(0xFF38BDF8)
        "MORNING", "BARAKAH" -> Color(0xFFF2B880)
        "PEACE", "TRANQUILITY" -> Color(0xFF4EDEA3)
        else -> Color(0xFFD4A853)
    }

    return ExploreCardItem(
        id = id,
        title = title,
        description = subtitle.ifEmpty { title },
        category = categoryUpper,
        avatarUrl = coverUrl,
        surahCountText = "$trackCount Surahs • $tag",
        accentColor = accent
    )
}

/**
 * Map [DetailedReciter] to [ExploreCardItem].
 */
fun DetailedReciter.toExploreCardItem(): ExploreCardItem {
    return ExploreCardItem(
        id = slug,
        title = nameEn,
        description = bio,
        category = style.uppercase(),
        avatarUrl = photoUrl,
        surahCountText = "${recitations.size} Surahs • $riwayah",
        accentColor = Color(0xFFD4A853),
        tracks = recitations.map { it.toTrackItem(this) }
    )
}
