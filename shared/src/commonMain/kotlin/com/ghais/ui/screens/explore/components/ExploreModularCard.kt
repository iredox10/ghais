package com.ghais.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import com.ghais.data.seed.GhaisAssets
import com.ghais.data.seed.toTrackItem
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.playlists.MoodPlaylist
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

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
 * True-grayscale filter — artwork stays recognisable while remaining strictly
 * monochrome. Mirrors the Home `NoirArtworkWell` pattern. Instantiated once so
 * recompositions never re-allocate it.
 */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Data model representing a modular explore collection or playlist item.
 *
 * NOTE (Noir Glass): [accentColor] is retained for signature compatibility but
 * is intentionally ignored at render time — the card renders zero hue.
 */
data class ExploreCardItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // e.g. "FOCUS", "SLEEP", "WORK", "HEALING"
    val avatarUrl: String,
    val surahCountText: String, // e.g. "6 Surahs • Tartil"
    val accentColor: Color = Color.White, // Legacy compat — ignored (Noir = zero hue)
    val tracks: List<TrackItem> = emptyList(),
    val surahIds: List<Int> = emptyList()
)

/**
 * ExploreModularCard — strict Noir Glass monochrome.
 *
 * Depth recipe (matches NoirCard / NoirHeroCard + Home artwork wells):
 * - Fill: cardFillSoft resting, cardFillActive while playing.
 * - Hairlines: BorderCard resting / SpecularTop while playing + topSpecular + sheen.
 * - Artwork: grayscale (saturation-0) + 35% black scrim inside a clay well,
 *   monogram fallback when there is no artwork.
 * - Play affordance: chrome disc while playing, smoked well otherwise.
 * - Text ladder: 100 / 62 / 38 / 24 %. Zero hue.
 *
 * Public signatures and play/navigation callbacks are preserved.
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
 *
 * NOTE: [accentColor] is retained for compatibility and ignored (zero hue).
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
    accentColor: Color = Color.White,
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
    accentColor: Color = Color.White,
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
    accentColor: Color = Color.White,
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
 *
 * Noir shell: gradient fill (soft resting / active while playing) + 1px ghost
 * border (BorderCard resting / SpecularTop while playing) + bright TOP-ONLY
 * specular hairline + diagonal sheen. No flat greys, no hue.
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
    accentColor: Color = Color.White,
    avatarShape: Shape = RoundedCornerShape(22.dp),
    isPlaying: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    val cardShape = if (variant == ExploreCardVariant.HERO_WIDE) GhaisShapes.cardLarge else GhaisShapes.cardNoir
    val fill = if (isPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (isPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(fill)
            .border(1.dp, border, cardShape)
            .topSpecular(inset = if (variant == ExploreCardVariant.HERO_WIDE) 30.dp else 26.dp)
            .noirClickable(onClick)
    ) {
        // Diagonal glass sheen swept across the card.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
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
        // Chunky Oversized Avatar (84.dp) — Noir clay well.
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
            // Top Row: Category badge, Surah count chip & Play Disc
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

                ExplorePlayButton(
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    size = 42.dp,
                    onClick = onPlayClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline Title — 100% ladder.
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Descriptive text with 2-line clamp — 62% ladder.
            Text(
                text = description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                color = GhaisNoir.TextSecondary,
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
        // Top row: Category Badge (left) and Play Disc (right)
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

        // Prominent Chunky Avatar (76.dp) centered — Noir clay well.
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

        // Headline Title — 100% ladder.
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = GhaisNoir.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subheadline Descriptive text with 2-line clamp — 62% ladder.
        Text(
            text = description,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.5.sp,
            color = GhaisNoir.TextSecondary,
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
 * Noir artwork well: grayscale image inside a clay well with a 35% black scrim
 * so it reads as an engraved tile. Falls back to a monogram when there is no
 * artwork. The rim brightens to SpecularTop while playing — state reads through
 * fill elevation and chromium, never hue.
 *
 * NOTE: [accentColor] is retained for compatibility and ignored.
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
    val monogram = remember(title) { title.firstOrNull()?.uppercase() ?: "Q" }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(avatarShape)
                .background(GhaisNoir.wellFill())
                .border(
                    width = 1.dp,
                    color = if (isPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                    shape = avatarShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    colorFilter = NoirGrayscale,
                    modifier = Modifier.fillMaxSize()
                )
                // Darkening scrim: keeps the plate recessed instead of glowing.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                Text(
                    text = monogram,
                    color = GhaisNoir.TextPrimary,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Category badge pill (e.g. "FOCUS", "SLEEP", "WORK", "HEALING").
 *
 * Monochrome informational pill: Fill2 wash + ghost rim + 38% label.
 * NOTE: [accentColor] is retained for compatibility and ignored.
 */
@Composable
private fun ExploreCategoryBadge(
    category: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.uppercase(),
            color = GhaisNoir.TextTertiary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
    }
}

/**
 * Surah count chip (e.g. "6 Surahs • Tartil").
 *
 * Monochrome informational chip: Fill2 wash + ghost rim + 62% label.
 */
@Composable
private fun ExploreSurahCountChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Noir play affordance: chromium disc while playing, smoked well otherwise.
 * Fill carries the state — never hue.
 *
 * NOTE: [accentColor] is retained for compatibility and ignored.
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
            .clip(GhaisShapes.well)
            .background(
                if (isPlaying) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                GhaisShapes.well
            )
            .border(
                width = 1.dp,
                color = if (isPlaying) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                shape = GhaisShapes.well
            )
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = if (isPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
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
 *
 * Noir: accent is always monochrome (legacy field kept for compatibility).
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

    // Noir Glass: zero hue — state reads through fill elevation, not color.
    val accent = Color.White

    val avatarUrl = when (id) {
        "study-focus" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "mindful-hifz" }?.coverUrl ?: GhaisAssets.LibraryMorningCover
        "focus-work" -> GhaisAssets.AllCuratedPlaylists.firstOrNull()?.coverUrl ?: GhaisAssets.LibraryMorningCover
        "sleep-mode" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "tahajjud-night-qiyam" }?.coverUrl ?: GhaisAssets.LibraryTahajjudCover
        "heart-soothing" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "anxiety-relief-inshirah" }?.coverUrl ?: GhaisAssets.LibraryMorningCover
        "duaa-ruqia" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "ayat-ash-shifa" }?.coverUrl ?: GhaisAssets.LibraryTahajjudCover
        "tahajjud" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "tahajjud-night-qiyam" }?.coverUrl ?: GhaisAssets.LibraryTahajjudCover
        "sunrise-barakah" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "sunrise-barakah" }?.coverUrl ?: GhaisAssets.LibraryMorningCover
        "emotional" -> GhaisAssets.AllCuratedPlaylists.getOrNull(1)?.coverUrl ?: GhaisAssets.LibraryMorningCover
        "most-beautiful" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "garden-of-tranquility" }?.coverUrl ?: GhaisAssets.NowPlayingVinylArtUrl
        "favourites" -> GhaisAssets.ProfileAvatarUrl
        else -> GhaisAssets.LibraryMorningCover
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
 *
 * Noir: accent is always monochrome (legacy field kept for compatibility).
 */
fun DetailedCuratedPlaylist.toExploreCardItem(): ExploreCardItem {
    val categoryUpper = tag.ifEmpty { "CURATED" }.uppercase()
    val accent = Color.White

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
 *
 * Noir: accent is always monochrome (legacy field kept for compatibility).
 */
fun CuratedPlaylist.toExploreCardItem(): ExploreCardItem {
    val categoryUpper = (if (category.isNotEmpty()) category else tag).uppercase()
    val accent = Color.White

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
 *
 * Noir: accent is always monochrome (legacy field kept for compatibility).
 */
fun DetailedReciter.toExploreCardItem(): ExploreCardItem {
    return ExploreCardItem(
        id = slug,
        title = nameEn,
        description = bio,
        category = style.uppercase(),
        avatarUrl = photoUrl,
        surahCountText = "${recitations.size} Surahs • $riwayah",
        accentColor = Color.White,
        tracks = recitations.map { it.toTrackItem(this) }
    )
}
