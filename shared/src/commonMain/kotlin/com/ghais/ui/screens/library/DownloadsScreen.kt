package com.ghais.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.ScheduleTracks
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.theme.GhaisNoir
import kotlinx.coroutines.launch

/**
 * Downloads manager: every offline key from [QuranDownloads.downloadedKeys]
 * with per-item delete, plus [QuranDownloads.failedKeys] with retry.
 *
 * Key shapes (see DownloadKeys): `"<slug>/<surahId>"` for full surahs and
 * `"<slug>/<surahId>/<ayahNo>"` for single-ayah clips.
 */
class DownloadsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
        val progress by QuranDownloads.progress.collectAsState()
        val failedKeys by QuranDownloads.failedKeys.collectAsState()

        // Same data sources as the Settings "Downloaded" row.
        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }

        var showClearConfirm by remember { mutableStateOf(false) }

        val downloaded = remember(downloadedKeys) { downloadedKeys.sorted() }
        // A finished key is never shown as failed, even if a stale entry lingers.
        val failed = remember(failedKeys, downloadedKeys) {
            failedKeys.filterNot { it in downloadedKeys }.sorted()
        }

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    NoirNavBar(
                        title = "Downloads",
                        backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onBack = { navigator.pop() },
                        titleAlignLeft = true
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconWell(
                                            icon = Icons.Default.Download,
                                            size = 64.dp,
                                            iconSize = 28.dp,
                                            contentDescription = "Downloads"
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "OFFLINE",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (downloaded.isEmpty()) "Your offline recitations"
                                                else "${downloaded.size} offline item${if (downloaded.size == 1) "" else "s"}",
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Listen without a connection.",
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        NoirStatChip(
                                            text = if (downloaded.isEmpty()) "0 downloaded"
                                            else "${downloaded.size} downloaded"
                                        )
                                        NoirStatChip(text = formatDownloadBytes(storageSizeBytes))
                                    }

                                    if (downloaded.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        ChromePillButton(
                                            text = "Clear all",
                                            onClick = { showClearConfirm = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = Icons.Default.DeleteOutline
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    if (downloaded.isEmpty() && failed.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        IconWell(
                                            icon = Icons.Default.Download,
                                            size = 64.dp,
                                            iconSize = 28.dp,
                                            contentDescription = null,
                                            tint = GhaisNoir.TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No downloads yet",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Download a surah to listen offline.",
                                            color = GhaisNoir.TextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (downloaded.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    NoirSectionHeader(
                                        label = "Downloaded",
                                        actionLabel = "${downloaded.size} items",
                                        onAction = {}
                                    )
                                }
                            }
                            items(downloaded, key = { "dl_$it" }) { key ->
                                val entry = remember(key) { parseDownloadKey(key) }
                                val itemProgress = progress[key]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                ) {
                                    NoirListRow(
                                        title = entry.title,
                                        subtitle = downloadSubtitle(entry, itemProgress),
                                        icon = Icons.Default.Download,
                                        chevron = false,
                                        onClick = {
                                            playDownloaded(entry)
                                            navigator.push(NowPlayingScreen())
                                        },
                                        trailing = {
                                            DownloadRowTrailing(
                                                onDelete = {
                                                    scope.launch { deleteDownloadKey(key) }
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (failed.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NoirSectionHeader(
                                        label = "Failed",
                                        actionLabel = "${failed.size} to retry",
                                        onAction = {}
                                    )
                                }
                            }
                            items(failed, key = { "fail_$it" }) { key ->
                                val entry = remember(key) { parseDownloadKey(key) }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                ) {
                                    NoirListRow(
                                        title = entry.title,
                                        subtitle = "${entry.subtitle} • Tap retry",
                                        icon = Icons.Default.Refresh,
                                        chevron = false,
                                        onClick = { scope.launch { retryDownloadKey(key) } },
                                        trailing = {
                                            RetryRowTrailing(
                                                onRetry = { scope.launch { retryDownloadKey(key) } },
                                                onDelete = { scope.launch { deleteDownloadKey(key) } }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear all downloads?") },
                text = { Text("This removes every downloaded recitation from this device.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearConfirm = false
                            scope.launch { QuranDownloads.clearAll() }
                        }
                    ) { Text("Clear all") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

private data class DownloadEntry(
    val slug: String,
    val surahId: Int,
    val ayahNo: Int?,
    val title: String,
    val subtitle: String
)

private fun parseDownloadKey(key: String): DownloadEntry {
    val parts = key.split("/")
    val slug = parts.getOrNull(0).orEmpty()
    val surahId = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val ayahNo = parts.getOrNull(2)?.toIntOrNull()

    val surah = QuranDataRepository.getSurahById(surahId)
    val surahName = surah?.nameEn?.ifBlank { null }
        ?: surah?.transliteration?.ifBlank { null }
        ?: if (surahId > 0) "Surah $surahId" else "Unknown surah"
    val reciterName = QuranDataRepository.getBrowseReciterBySlug(slug)?.nameEn
        ?.takeIf { it.isNotBlank() } ?: slug.ifBlank { "Unknown reciter" }

    return if (ayahNo != null && ayahNo > 0) {
        DownloadEntry(
            slug = slug,
            surahId = surahId,
            ayahNo = ayahNo,
            title = "$surahName • Ayah $ayahNo",
            subtitle = reciterName
        )
    } else {
        DownloadEntry(
            slug = slug,
            surahId = surahId,
            ayahNo = null,
            title = surahName,
            subtitle = reciterName
        )
    }
}

private fun downloadSubtitle(entry: DownloadEntry, itemProgress: Float?): String {
    if (itemProgress != null) {
        val pct = if (itemProgress < 0f) "…" else "${(itemProgress * 100).toInt()}%"
        return "${entry.subtitle} • Downloading $pct"
    }
    return if (entry.ayahNo != null) "${entry.subtitle} • Ayah clip" else entry.subtitle
}

/**
 * Plays a downloaded entry. The engine resolves the downloaded file first
 * (offline-first), so the tapped item always plays local audio — even with
 * no network.
 *
 * Up Next is the user's offline shelf: every downloaded full surah, so the
 * queue shown while playing a download is the offline list — not the
 * reciter's streaming catalogue.
 */
private fun playDownloaded(entry: DownloadEntry) {
    if (entry.surahId <= 0) return
    // Single-ayah clip: ayah-mode playback starting at the clipped verse.
    if (entry.ayahNo != null && entry.ayahNo > 0) {
        AudioEngine.playSurahInAyahMode(entry.surahId, entry.slug, entry.ayahNo)
        return
    }
    val tracks = ScheduleTracks.buildOfflineTracks(
        QuranDownloads.downloadedKeys.value,
        QuranDataRepository.getReciters(),
        QuranDataRepository.getSurahs()
    )
    if (tracks.isEmpty()) return
    val startIndex = tracks.indexOfFirst {
        it.reciterSlug == entry.slug && it.surahId == entry.surahId
    }.takeIf { it >= 0 } ?: 0
    AudioEngine.playQueue(tracks, startIndex = startIndex)
}

private fun deleteDownloadKey(key: String) {    val parts = key.split("/")
    val slug = parts.getOrNull(0).orEmpty()
    val surahId = parts.getOrNull(1)?.toIntOrNull() ?: return
    val ayahNo = parts.getOrNull(2)?.toIntOrNull()
    if (ayahNo != null) {
        QuranDownloads.deleteAyah(slug, surahId, ayahNo)
    } else {
        QuranDownloads.delete(slug, surahId)
    }
}

private fun retryDownloadKey(key: String) {
    val parts = key.split("/")
    val slug = parts.getOrNull(0).orEmpty()
    val surahId = parts.getOrNull(1)?.toIntOrNull() ?: return
    val ayahNo = parts.getOrNull(2)?.toIntOrNull()
    val reciter = QuranDataRepository.getBrowseReciterBySlug(slug)
        ?: QuranDataRepository.getReciters().firstOrNull { it.slug == slug }
        ?: return
    if (ayahNo != null) {
        QuranDownloads.downloadAyah(slug, surahId, ayahNo, reciter.getAyahAudioUrl(surahId, ayahNo))
    } else {
        QuranDownloads.download(slug, surahId, reciter.getFullSurahUrl(surahId))
    }
}

private fun formatDownloadBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${oneDecimalDownload(kb)} KB"
    return "${oneDecimalDownload(kb / 1024.0)} MB"
}

private fun oneDecimalDownload(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}

@Composable
private fun RowScope.DownloadRowTrailing(onDelete: () -> Unit) {
    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Delete download",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun RowScope.RetryRowTrailing(
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
        Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Remove failed download",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(GhaisNoir.wellFill(), CircleShape)
            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            .noirClickable(onRetry),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Retry download",
            tint = GhaisNoir.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
