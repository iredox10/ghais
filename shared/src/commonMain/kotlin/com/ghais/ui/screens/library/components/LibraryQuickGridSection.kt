package com.ghais.ui.screens.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.seed.QuranDataRepository
import com.ghais.data.seed.GhaisAssets
import com.ghais.data.seed.toTrackItem
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisColors

/**
 * 2-column Quick Cards row matching Stitch specs:
 * 1) "Liked Verses & Duas" with amber theme, pin icon, and saved count.
 * 2) "Offline Surahs" with emerald theme, pin icon, and downloaded stats.
 */
@Composable
fun LibraryQuickGridSection(
    onLikedVersesClick: () -> Unit = {},
    onDownloadedClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Live offline count (keys are "$reciterSlug/$surahId").
    val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
    val offlineCount = downloadedKeys.size
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Liked Verses & Duas
        Box(
            modifier = Modifier
                .weight(1f)
                .height(124.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GhaisColors.SurfaceContainer)
                .drawBehind {
                    // Subtle amber glow in bottom right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GhaisColors.Secondary.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width, size.height),
                            radius = 65.dp.toPx()
                        )
                    )
                }
                .clickable { onLikedVersesClick() }
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GhaisColors.Secondary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = GhaisColors.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = GhaisColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Liked Verses & Duas",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(GhaisColors.Secondary, CircleShape)
                        )
                        Text(
                            text = "142 verses saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisColors.Secondary
                        )
                    }
                }
            }
        }

        // Card 2: Offline Surahs
        Box(
            modifier = Modifier
                .weight(1f)
                .height(124.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GhaisColors.SurfaceContainer)
                .drawBehind {
                    // Subtle emerald glow in bottom right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GhaisColors.Primary.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width, size.height),
                            radius = 65.dp.toPx()
                        )
                    )
                }
                .clickable { onDownloadedClick() }
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GhaisColors.Primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Downloaded",
                            tint = GhaisColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = GhaisColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Offline Surahs",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = GhaisColors.Primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$offlineCount surahs offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisColors.Primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hifz Goal quick card matching Stitch design:
 * Surah 67 Al-Mulk, memorization progress, and mic review action.
 */
@Composable
fun HifzGoalCard(
    surahNumber: Int = 67,
    surahName: String = "Surah Al-Mulk",
    memorizedAyahs: Int = 18,
    totalAyahs: Int = 30,
    onReviewClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GhaisColors.SurfaceLow)
            .clickable { onReviewClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Surah number box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GhaisColors.SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$surahNumber",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisColors.Secondary,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "SURAH",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = GhaisColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = surahName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisColors.TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    GhaisColors.Secondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Hifz Goal",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisColors.Secondary
                            )
                        }
                    }
                    Text(
                        text = "$memorizedAyahs of $totalAyahs Ayahs memorized • Review today",
                        fontSize = 11.5.sp,
                        color = GhaisColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onReviewClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GhaisColors.SurfaceHigh)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Review",
                    tint = GhaisColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Data model for custom playlists shelf items.
 */
data class LibraryPlaylistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val trackCount: Int = 0,
    val type: String = "Playlist",
    val badge: String? = null,
    val extra: String? = null,
    val isDownloaded: Boolean = false,
    val isReciter: Boolean = false,
    val verified: Boolean = false,
    val coverUrl: String? = null,
    val iconVector: ImageVector? = null,
    val iconColor: Color? = null,
    val categories: List<String> = listOf("All", "Playlists"),
    /** Set for resolved offline surahs: "$reciterSlug/$surahId" download key parts. */
    val reciterSlug: String? = null,
    val surahNumber: Int? = null
)

/**
 * Resolves live [QuranDownloads.downloadedKeys] ("$reciterSlug/$surahId") into
 * display items via [QuranDataRepository] lookup. Keys that don't resolve to a
 * known reciter + surah are skipped.
 */
private fun resolveDownloadedItems(keys: Set<String>): List<LibraryPlaylistItem> {
    return keys.mapNotNull { key ->
        val slug = key.substringBeforeLast("/", missingDelimiterValue = "")
        val surahNumber = key.substringAfterLast("/").toIntOrNull()
        if (slug.isEmpty() || surahNumber == null) return@mapNotNull null
        val reciter = QuranDataRepository.getReciterBySlug(slug) ?: return@mapNotNull null
        val track = reciter.recitations.firstOrNull { it.surahNumber == surahNumber }
            ?: return@mapNotNull null
        LibraryPlaylistItem(
            id = key,
            title = track.surahNameEn,
            subtitle = reciter.nameEn,
            trackCount = 1,
            type = "Offline",
            isDownloaded = true,
            reciterSlug = reciter.slug,
            surahNumber = track.surahNumber,
            categories = listOf("All", "Downloaded")
        )
    }.sortedWith(compareBy({ it.subtitle }, { it.surahNumber ?: 0 }))
}

/**
 * Custom playlists and library items shelf matching Stitch specs.
 * Supports dynamic filtering by category ("All", "Playlists", "Downloaded", "Reciters", etc.).
 */
@Composable
fun LibraryPlaylistsSection(
    selectedFilter: String = "All",
    onPlaylistClick: (String) -> Unit = {},
    onReciterClick: (String) -> Unit = {},
    onMoreClick: (LibraryPlaylistItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allItems = remember {
        listOf(
            LibraryPlaylistItem(
                id = "tahajjud",
                title = "Tahajjud & Night Qiyam",
                subtitle = "18 recitations",
                trackCount = 18,
                type = "Playlist",
                extra = "Updated yesterday",
                coverUrl = GhaisAssets.LibraryTahajjudCover,
                categories = listOf("All", "Playlists")
            ),
            LibraryPlaylistItem(
                id = "morning_adhkar",
                title = "Morning Adhkar & Protection",
                subtitle = "12 tracks",
                trackCount = 12,
                type = "Playlist",
                isDownloaded = true,
                coverUrl = GhaisAssets.LibraryMorningCover,
                categories = listOf("All", "Playlists", "Downloaded")
            ),
            LibraryPlaylistItem(
                id = "mishary",
                title = "Mishary Rashid Alafasy",
                subtitle = "114 Surahs",
                type = "Reciter",
                badge = "Following",
                isReciter = true,
                verified = true,
                coverUrl = GhaisAssets.LibraryMisharyAvatar,
                categories = listOf("All", "Reciters")
            ),
            LibraryPlaylistItem(
                id = "juz_amma",
                title = "Juz Amma for Kids & Learning",
                subtitle = "37 Surahs",
                trackCount = 37,
                type = "Playlist",
                extra = "Word-by-word active",
                iconVector = Icons.Default.School,
                iconColor = GhaisColors.Primary,
                categories = listOf("All", "Playlists")
            ),
            LibraryPlaylistItem(
                id = "calm_sleep",
                title = "Calm Sleep Recitations",
                subtitle = "8 recitations",
                trackCount = 8,
                type = "Playlist",
                isDownloaded = true,
                iconVector = Icons.Default.Bedtime,
                iconColor = GhaisColors.Secondary,
                categories = listOf("All", "Playlists", "Downloaded")
            )
        )
    }

    val filteredItems = remember(selectedFilter) {
        when (selectedFilter) {
            "All" -> allItems
            "Playlists" -> allItems.filter { it.categories.contains("Playlists") }
            // "Downloaded" renders live offline items (downloadedItems) instead of mocks.
            "Downloaded" -> emptyList()
            "Reciters" -> allItems.filter { it.categories.contains("Reciters") }
            "Saved Verses" -> emptyList()
            "Hifz Goals" -> emptyList()
            else -> allItems.filter { it.categories.contains(selectedFilter) }
        }
    }

    // Live offline surahs resolved from QuranDownloads keys; unresolvable keys skipped.
    val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
    val downloadedItems = remember(downloadedKeys) { resolveDownloadedItems(downloadedKeys) }
    val isDownloadedFilter = selectedFilter == "Downloaded"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Shelf header with Recents & Grid toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Sort",
                    tint = GhaisColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Recents",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisColors.TextPrimary
                )
            }

            IconButton(
                onClick = { /* Grid view toggle */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Grid View",
                    tint = GhaisColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Shelf item rows: the Downloaded filter lists live offline surahs
        // (tap plays offline, trailing icon deletes); other filters keep mock behavior.
        if (isDownloadedFilter) {
            if (downloadedItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = GhaisColors.TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No downloads yet — open a reciter and tap Download",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                downloadedItems.forEach { item ->
                    DownloadedSurahRow(item = item)
                }
            }
        } else {
            filteredItems.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (item.isReciter) {
                            onReciterClick(item.id)
                        } else {
                            onPlaylistClick(item.id)
                        }
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Item thumbnail / avatar
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isReciter) {
                            // Circular reciter portrait with verified badge
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(GhaisColors.SurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.coverUrl != null) {
                                    AsyncImage(
                                        model = item.coverUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            if (item.verified) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(GhaisColors.Secondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Verified",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        } else if (item.coverUrl != null) {
                            // Playlist cover image
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GhaisColors.SurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            // Icon placeholder
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GhaisColors.SurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.iconVector != null) {
                                    Icon(
                                        imageVector = item.iconVector,
                                        contentDescription = item.title,
                                        tint = item.iconColor ?: GhaisColors.Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & meta details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (item.badge != null) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            GhaisColors.SurfaceHighest,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GhaisColors.Primary
                                    )
                                }
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = GhaisColors.TextTertiary
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            GhaisColors.SurfaceHighest,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GhaisColors.TextSecondary
                                    )
                                }
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = GhaisColors.TextTertiary
                                )
                            }

                            Text(
                                text = item.subtitle,
                                fontSize = 11.5.sp,
                                color = GhaisColors.TextSecondary
                            )

                            if (item.extra != null) {
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = GhaisColors.TextTertiary
                                )
                                Text(
                                    text = item.extra,
                                    fontSize = 11.5.sp,
                                    color = GhaisColors.Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (item.isDownloaded) {
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = GhaisColors.TextTertiary
                                )
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "Downloaded",
                                    tint = GhaisColors.Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { onMoreClick(item) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = GhaisColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        }
    }
}

/**
 * Offline surah row for the Downloaded filter: same shelf row styling as
 * [LibraryPlaylistsSection]. Tapping plays offline ([AudioEngine] auto-uses the
 * local file for downloaded keys); the trailing icon deletes the download.
 */
@Composable
private fun DownloadedSurahRow(
    item: LibraryPlaylistItem,
    modifier: Modifier = Modifier
) {
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    fun playOffline() {
        val slug = item.reciterSlug ?: return
        val surahNumber = item.surahNumber ?: return
        val reciter = QuranDataRepository.getReciterBySlug(slug) ?: return
        val track = reciter.recitations.firstOrNull { it.surahNumber == surahNumber } ?: return
        AudioEngine.playTrack(track.toTrackItem(reciter))
        rootNavigator?.push(NowPlayingScreen())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { playOffline() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Surah number thumbnail, matching the HifzGoalCard number-box style.
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GhaisColors.SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.surahNumber ?: "–"}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhaisColors.Primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & meta details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                GhaisColors.SurfaceHighest,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.type,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisColors.TextSecondary
                        )
                    }
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = GhaisColors.TextTertiary
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 11.5.sp,
                        color = GhaisColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = GhaisColors.TextTertiary
                    )
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = GhaisColors.Primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = {
                val slug = item.reciterSlug ?: return@IconButton
                val surahNumber = item.surahNumber ?: return@IconButton
                QuranDownloads.delete(slug, surahNumber)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete download",
                tint = GhaisColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Hadith inspirational quote footer matching Stitch specs:
 * "The best of you are those who learn the Quran and teach it." - Sahih al-Bukhari 5027
 */
@Composable
fun LibraryHadithFooter(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = null,
            tint = GhaisColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "\"The best of you are those who learn the Quran and teach it.\"",
            fontSize = 12.5.sp,
            fontStyle = FontStyle.Italic,
            color = GhaisColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Sahih al-Bukhari 5027",
            fontSize = 10.5.sp,
            color = GhaisColors.TextTertiary
        )
    }
}
