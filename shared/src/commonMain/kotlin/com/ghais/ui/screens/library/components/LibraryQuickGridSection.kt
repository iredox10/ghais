package com.ghais.ui.screens.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.seed.QuranDataRepository
import com.ghais.data.seed.GhaisAssets
import com.ghais.data.seed.toTrackItem
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 4 — Noir Library quick grids, shelves and rows.
 *
 * Strict Noir Glass monochrome: soft glass tiles + 1px card borders +
 * top-only specular hairlines, clay icon-wells, chrome/ghost pills and
 * [NoirListRow] shelf rows. Data models, filter logic and navigation targets
 * are unchanged; state reads through fill elevation, weight and opacity —
 * zero hue anywhere.
 */

/**
 * 2-column Quick Cards row:
 * 1) "Liked Verses & Duas" with saved count.
 * 2) "Offline Surahs" with live downloaded stats.
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
    val quickShape = RoundedCornerShape(18.dp)
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
                .clip(quickShape)
                .background(GhaisNoir.cardFillSoft())
                .border(1.dp, GhaisNoir.BorderCard, quickShape)
                .topSpecular(inset = 18.dp)
                .noirClickable { onLikedVersesClick() }
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
                    IconWell(icon = Icons.Default.Favorite, size = 38.dp, iconSize = 20.dp)
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Liked Verses & Duas",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary,
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
                                .background(GhaisNoir.TextSecondary, CircleShape)
                        )
                        Text(
                            text = "142 verses saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextSecondary
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
                .clip(quickShape)
                .background(GhaisNoir.cardFillSoft())
                .border(1.dp, GhaisNoir.BorderCard, quickShape)
                .topSpecular(inset = 18.dp)
                .noirClickable { onDownloadedClick() }
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
                    IconWell(icon = Icons.Default.CloudDownload, size = 38.dp, iconSize = 20.dp)
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Offline Surahs",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary,
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
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$offlineCount surahs offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hifz Goal quick card:
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
    val hifzShape = RoundedCornerShape(18.dp)
    val progress = if (totalAyahs > 0) memorizedAyahs.toFloat() / totalAyahs.toFloat() else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(hifzShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, hifzShape)
            .topSpecular(inset = 22.dp)
            .noirClickable { onReviewClick() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Surah number box (recessed plate).
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$surahNumber",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.TextPrimary,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "SURAH",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = GhaisNoir.TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = surahName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GhaisNoir.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.Fill2)
                                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "HIFZ GOAL",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = GhaisNoir.TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "$memorizedAyahs of $totalAyahs Ayahs memorized • Review today",
                            fontSize = 11.5.sp,
                            color = GhaisNoir.TextSecondary,
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
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Review",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monochrome memorization meter (chrome fill on engraved track).
            NoirSegmentedProgress(progress = progress)
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
 * Custom playlists and library items shelf.
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
                iconColor = null,
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
                iconColor = null,
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
        // Shelf header — bright label + ghost count (Noir section rhythm).
        NoirSectionHeader(
            label = if (isDownloadedFilter) {
                "Offline • ${downloadedItems.size}"
            } else {
                "Recents • ${filteredItems.size}"
            }
        )
        Spacer(modifier = Modifier.height(2.dp))

        // Shelf item rows: the Downloaded filter lists live offline surahs
        // (tap plays offline, trailing icon deletes); other filters keep mock behavior.
        if (isDownloadedFilter) {
            if (downloadedItems.isEmpty()) {
                NoirLibraryShelveEmpty()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    downloadedItems.forEach { item ->
                        DownloadedSurahRow(item = item)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredItems.forEach { item ->
                    NoirListRow(
                        title = item.title,
                        subtitle = libraryRowSubtitle(item),
                        icon = item.iconVector
                            ?: if (item.isReciter) Icons.Default.Person else Icons.Default.QueueMusic,
                        onClick = {
                            if (item.isReciter) {
                                onReciterClick(item.id)
                            } else {
                                onPlaylistClick(item.id)
                            }
                        },
                        trailing = {
                            IconButton(
                                onClick = { onMoreClick(item) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Single-line Noir subtitle preserving badge/type, meta, extra and offline state. */
private fun libraryRowSubtitle(item: LibraryPlaylistItem): String {
    val head = item.badge ?: item.type
    val base = "$head • ${item.subtitle}"
    val withExtra = if (item.extra != null) "$base • ${item.extra}" else base
    return if (item.isDownloaded) "$withExtra • Offline" else withExtra
}

/** Ghost empty state for the Downloaded filter — muted well + tertiary copy. */
@Composable
private fun NoirLibraryShelveEmpty(modifier: Modifier = Modifier) {
    val emptyShape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(emptyShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, emptyShape)
            .topSpecular(inset = 22.dp)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconWell(icon = Icons.Default.CloudDownload, size = 48.dp, iconSize = 24.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No downloads yet — open a reciter and tap Download",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GhaisNoir.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Offline surah row for the Downloaded filter as a [NoirListRow]: tapping
 * plays offline ([AudioEngine] auto-uses the local file for downloaded keys);
 * the trailing icon deletes the download.
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

    NoirListRow(
        title = item.title,
        subtitle = "Offline • ${item.subtitle}",
        icon = Icons.Default.DownloadDone,
        modifier = modifier,
        onClick = { playOffline() },
        trailing = {
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
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

/**
 * Hadith inspirational quote footer:
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
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "\"The best of you are those who learn the Quran and teach it.\"",
            fontSize = 12.5.sp,
            fontStyle = FontStyle.Italic,
            color = GhaisNoir.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Sahih al-Bukhari 5027",
            fontSize = 10.5.sp,
            color = GhaisNoir.TextTertiary
        )
    }
}
