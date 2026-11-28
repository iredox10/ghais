package com.ghais.ui.screens.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirEditorialHeader
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.bidiIsolate
import kotlinx.coroutines.launch

private enum class DownloadCategory(val label: String) {
    ALL("All"),
    RECITATIONS("Recitations"),
    MEMORIZE("Memorize"),
}

private data class SurahDownloadItem(
    val slug: String,
    val surahId: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val totalAyahs: Int,
    val hasFullSurah: Boolean,
    val downloadedAyahs: List<Int>,
    val progress: Float?,
) {
    val isMemorizeBundle: Boolean get() = downloadedAyahs.isNotEmpty()
    val rawKeys: List<String>
        get() = buildList {
            if (hasFullSurah) add("$slug/$surahId")
            downloadedAyahs.forEach { add("$slug/$surahId/$it") }
        }
}

private data class QariDownloadSection(
    val slug: String,
    val name: String,
    val photoUrl: String?,
    val items: List<SurahDownloadItem>,
)

private data class FailedSurahGroup(
    val slug: String,
    val surahId: Int,
    val surahNameEn: String,
    val reciterName: String,
    val keys: List<String>,
)

/**
 * Offline Downloads Manager:
 * - Groups Hifz ayah-by-ayah downloads into a single Surah bundle card instead
 *   of listing hundreds of individual ayah files.
 * - Provides a category switcher (All / Recitations / Memorize) so users can
 *   switch to Memorize and launch offline Hifz Ayah-by-Ayah playback directly.
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

        var storageSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(downloadedKeys) {
            storageSizeBytes = QuranDownloads.storageBytes()
        }

        var selectedCategory by remember { mutableStateOf(DownloadCategory.ALL) }
        var searchQuery by remember { mutableStateOf("") }
        var showClearConfirm by remember { mutableStateOf(false) }
        var pendingDeleteItem by remember { mutableStateOf<SurahDownloadItem?>(null) }
        var pendingDeleteFailed by remember { mutableStateOf<FailedSurahGroup?>(null) }

        val allItems = remember(downloadedKeys, progress) {
            buildSurahDownloadItems(downloadedKeys, progress)
        }
        val recitationCount = remember(allItems) { allItems.count { it.hasFullSurah } }
        val memorizeCount = remember(allItems) { allItems.count { it.isMemorizeBundle } }

        val filteredItems = remember(allItems, selectedCategory, searchQuery) {
            val byCategory = when (selectedCategory) {
                DownloadCategory.ALL -> allItems
                DownloadCategory.RECITATIONS -> allItems.filter { it.hasFullSurah }
                DownloadCategory.MEMORIZE -> allItems.filter { it.isMemorizeBundle }
            }
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) return@remember byCategory
            val surahRef = q.substringBefore(":").toIntOrNull()
            byCategory.filter { item ->
                val reciterName = reciterDisplayName(item.slug).lowercase()
                item.surahNameEn.lowercase().contains(q) ||
                    item.surahNameAr.contains(searchQuery.trim()) ||
                    reciterName.contains(q) ||
                    item.slug.lowercase().contains(q) ||
                    item.surahId.toString() == q ||
                    (surahRef != null && item.surahId == surahRef)
            }
        }

        val qariSections = remember(filteredItems) {
            filteredItems.groupBy { it.slug }
                .map { (slug, items) ->
                    val reciter = QuranDataRepository.getBrowseReciterBySlug(slug)
                    QariDownloadSection(
                        slug = slug,
                        name = reciter?.nameEn?.takeIf { it.isNotBlank() }
                            ?: slug.ifBlank { "Unknown reciter" },
                        photoUrl = reciter?.imageUrl,
                        items = items.sortedBy { it.surahId },
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

        val failedGroups = remember(failedKeys, downloadedKeys) {
            failedKeys.filterNot { it in downloadedKeys }
                .groupBy {
                    val parts = it.split("/")
                    (parts.getOrNull(0).orEmpty()) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                }
                .map { (pair, keys) ->
                    val (slug, surahId) = pair
                    val surah = QuranDataRepository.getSurahById(surahId)
                    val surahName = surah?.nameEn?.ifBlank { null }
                        ?: if (surahId > 0) "Surah $surahId" else "Unknown surah"
                    FailedSurahGroup(
                        slug = slug,
                        surahId = surahId,
                        surahNameEn = surahName,
                        reciterName = reciterDisplayName(slug),
                        keys = keys.sorted(),
                    )
                }
                .sortedWith(compareBy({ it.reciterName.lowercase() }, { it.surahId }))
        }

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxSize()) {
                NoirEditorialHeader(
                    eyebrow = "Offline Library",
                    title = "Downloads",
                    onBack = { navigator.pop() },
                    backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    badgeText = "${allItems.size} · ${formatDownloadBytes(storageSizeBytes)}"
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
                ) {
                    // Plum Aura Hero Summary Card
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(GhaisNoir.plumFill())
                                    .background(GhaisNoir.sheen())
                                    .border(
                                        1.dp,
                                        GhaisNoir.SpecularTop.copy(alpha = 0.28f),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.14f))
                                                .border(
                                                    1.dp,
                                                    GhaisNoir.SpecularTop.copy(alpha = 0.3f),
                                                    RoundedCornerShape(16.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Offline library",
                                                tint = GhaisNoir.Lavender,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "OFFLINE LIBRARY",
                                                color = GhaisNoir.Lavender,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.3.sp
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (allItems.isEmpty()) "No offline surahs yet"
                                                else "${allItems.size} surah${if (allItems.size == 1) "" else "s"} ready",
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$recitationCount Recitations · $memorizeCount Memorize",
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(GhaisShapes.pill)
                                                .background(Color.Black.copy(alpha = 0.24f))
                                                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = formatDownloadBytes(storageSizeBytes),
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (allItems.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                modifier = Modifier
                                                    .clip(GhaisShapes.pill)
                                                    .background(Color.White.copy(alpha = 0.14f))
                                                    .border(
                                                        1.dp,
                                                        GhaisNoir.SpecularTop.copy(alpha = 0.28f),
                                                        GhaisShapes.pill
                                                    )
                                                    .noirClickable { showClearConfirm = true }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = GhaisNoir.TextPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Clear all",
                                                    color = GhaisNoir.TextPrimary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Category Switcher: All | Recitations | Memorize
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                DownloadCategory.entries.forEach { cat ->
                                    val selected = selectedCategory == cat
                                    val count = when (cat) {
                                        DownloadCategory.ALL -> allItems.size
                                        DownloadCategory.RECITATIONS -> recitationCount
                                        DownloadCategory.MEMORIZE -> memorizeCount
                                    }
                                    val icon = when (cat) {
                                        DownloadCategory.ALL -> Icons.Filled.Download
                                        DownloadCategory.RECITATIONS -> Icons.Filled.Headphones
                                        DownloadCategory.MEMORIZE -> Icons.Filled.AutoStories
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(GhaisShapes.pill)
                                            .then(
                                                if (selected) Modifier.background(GhaisNoir.lavenderFill())
                                                else Modifier
                                                    .background(GhaisNoir.cardFillSoft())
                                                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                                            )
                                            .noirClickable { selectedCategory = cat }
                                            .padding(vertical = 9.dp, horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (selected) GhaisNoir.LavenderInk else GhaisNoir.Lavender,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (count > 0) "${cat.label} ($count)" else cat.label,
                                            color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search bar for downloaded surahs, reciters, or numbers
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.insetFill())
                                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search downloads",
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 13.sp
                                        ),
                                        cursorBrush = SolidColor(GhaisNoir.Lavender),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Search surah, reciter, or number...",
                                                    color = GhaisNoir.TextTertiary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(GhaisNoir.Fill2)
                                                .noirClickable { searchQuery = "" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear search",
                                                tint = GhaisNoir.TextSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (qariSections.isEmpty() && failedGroups.isEmpty()) {
                        item {
                            NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IconWell(
                                        icon = when {
                                            searchQuery.isNotBlank() -> Icons.Default.Search
                                            selectedCategory == DownloadCategory.MEMORIZE -> Icons.Default.AutoStories
                                            else -> Icons.Default.Download
                                        },
                                        size = 58.dp,
                                        iconSize = 26.dp,
                                        contentDescription = null,
                                        tint = GhaisNoir.Lavender
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = when {
                                            searchQuery.isNotBlank() -> "No matching downloads"
                                            selectedCategory == DownloadCategory.MEMORIZE -> "No Hifz downloads yet"
                                            selectedCategory == DownloadCategory.RECITATIONS -> "No recitation downloads yet"
                                            else -> "No downloads yet"
                                        },
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = when {
                                            searchQuery.isNotBlank() -> "Try another surah name, number, or reciter."
                                            selectedCategory == DownloadCategory.MEMORIZE -> "Download a surah from Memorize to practice ayah-by-ayah offline."
                                            else -> "Download a surah to listen without an internet connection."
                                        },
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(GhaisShapes.pill)
                                                .background(GhaisNoir.lavenderFill())
                                                .noirClickable { searchQuery = "" }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Clear search",
                                                color = GhaisNoir.LavenderInk,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        qariSections.forEach { section ->
                            item(key = "grphdr_${section.slug}", contentType = "qari_header") {
                                val canPlaySection = if (selectedCategory == DownloadCategory.MEMORIZE) {
                                    section.items.any { it.isMemorizeBundle }
                                } else {
                                    section.items.any { it.hasFullSurah } || section.items.any { it.isMemorizeBundle }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    NoirReciterAvatar(
                                        photoUrl = section.photoUrl,
                                        nameEn = section.name,
                                        size = 34.dp,
                                        shape = CircleShape,
                                        monogramSize = 13.sp,
                                        slug = section.slug
                                    )
                                    Text(
                                        text = section.name,
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (canPlaySection) {
                                        Box(
                                            modifier = Modifier
                                                .clip(GhaisShapes.pill)
                                                .background(GhaisNoir.Lavender.copy(alpha = 0.14f))
                                                .border(
                                                    1.dp,
                                                    GhaisNoir.Lavender.copy(alpha = 0.30f),
                                                    GhaisShapes.pill
                                                )
                                                .noirClickable {
                                                    playQariSection(section, selectedCategory)
                                                    navigator.push(NowPlayingScreen())
                                                }
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = if (selectedCategory == DownloadCategory.MEMORIZE) "Memorize" else "Listen",
                                                color = GhaisNoir.Lavender,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            items(
                                items = section.items,
                                key = { "dl_${section.slug}_${it.surahId}" },
                                contentType = { "download_row" }
                            ) { item ->
                                val playAsMemorize = selectedCategory == DownloadCategory.MEMORIZE ||
                                    (!item.hasFullSurah && item.isMemorizeBundle)
                                val cardShape = RoundedCornerShape(18.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(cardShape)
                                        .background(
                                            if (playAsMemorize) GhaisNoir.tintFill()
                                            else GhaisNoir.cardFillSoft()
                                        )
                                        .border(
                                            1.dp,
                                            if (playAsMemorize) GhaisNoir.Lavender.copy(alpha = 0.25f)
                                            else GhaisNoir.BorderGhost,
                                            cardShape
                                        )
                                        .noirClickable {
                                            playSurahDownload(item, preferMemorize = playAsMemorize)
                                            navigator.push(NowPlayingScreen())
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(GhaisNoir.wellFill())
                                                .border(
                                                    1.dp,
                                                    GhaisNoir.BorderCard,
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.surahId.toString().padStart(2, '0'),
                                                color = if (playAsMemorize) GhaisNoir.Lavender else GhaisNoir.TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Text(
                                                    text = item.surahNameEn,
                                                    color = GhaisNoir.TextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (item.surahNameAr.isNotBlank()) {
                                                    Text(
                                                        text = bidiIsolate(item.surahNameAr),
                                                        fontFamily = GhaisTypography.quranFont,
                                                        fontSize = 14.sp,
                                                        color = GhaisNoir.TextSecondary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            val subtitle = buildItemSubtitle(item, playAsMemorize)
                                            Text(
                                                text = subtitle,
                                                color = if (playAsMemorize) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                                                fontSize = 11.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(GhaisNoir.Fill1)
                                                .noirClickable { pendingDeleteItem = item },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete download",
                                                tint = GhaisNoir.TextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (playAsMemorize) GhaisNoir.lavenderFill()
                                                    else GhaisNoir.chromeFill()
                                                )
                                                .noirClickable {
                                                    playSurahDownload(item, preferMemorize = playAsMemorize)
                                                    navigator.push(NowPlayingScreen())
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play ${item.surahNameEn}",
                                                tint = if (playAsMemorize) GhaisNoir.LavenderInk else GhaisNoir.OnChrome,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    item.progress?.let { p ->
                                        if (p in 0f..1f) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            NoirSegmentedProgress(
                                                progress = p,
                                                trackHeight = 4.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (failedGroups.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NoirSectionHeader(
                                        label = "Failed",
                                        actionLabel = "${failedGroups.size} to retry",
                                        onAction = {}
                                    )
                                }
                            }
                            items(failedGroups, key = { "fail_${it.slug}_${it.surahId}" }) { grp ->
                                val cardShape = RoundedCornerShape(16.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(cardShape)
                                        .background(GhaisNoir.cardFillSoft())
                                        .border(1.dp, GhaisNoir.BorderGhost, cardShape)
                                        .noirClickable {
                                            scope.launch {
                                                grp.keys.forEach { retryDownloadKey(it) }
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = GhaisNoir.Lavender,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = grp.surahNameEn,
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${grp.reciterName} • ${grp.keys.size} item${if (grp.keys.size == 1) "" else "s"} failed • Tap to retry",
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GhaisNoir.Fill1)
                                            .noirClickable { pendingDeleteFailed = grp },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove failed download",
                                            tint = GhaisNoir.TextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                text = { Text("This removes every downloaded recitation and Hifz bundle from this device.") },
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

        pendingDeleteItem?.let { item ->
            AlertDialog(
                onDismissRequest = { pendingDeleteItem = null },
                title = { Text("Delete download?") },
                text = { Text("This removes \"${item.surahNameEn}\" (${reciterDisplayName(item.slug)}) from this device.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeleteItem = null
                            scope.launch { deleteSurahItem(item) }
                        }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteItem = null }) { Text("Cancel") }
                }
            )
        }

        pendingDeleteFailed?.let { grp ->
            AlertDialog(
                onDismissRequest = { pendingDeleteFailed = null },
                title = { Text("Remove failed download?") },
                text = { Text("This clears the failed download entries for \"${grp.surahNameEn}\".") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDeleteFailed = null
                            scope.launch { grp.keys.forEach { deleteDownloadKey(it) } }
                        }
                    ) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteFailed = null }) { Text("Cancel") }
                }
            )
        }
    }
}

private fun buildSurahDownloadItems(
    downloadedKeys: Set<String>,
    progress: Map<String, Float>,
): List<SurahDownloadItem> {
    val allActiveKeys = (downloadedKeys + progress.keys).toSet()
    val grouped = allActiveKeys.groupBy { key ->
        val parts = key.split("/")
        parts.getOrNull(0).orEmpty() to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    return grouped.mapNotNull { (pair, keys) ->
        val (slug, surahId) = pair
        if (slug.isBlank() || surahId <= 0) return@mapNotNull null
        val surah = QuranDataRepository.getSurahById(surahId)
        val surahNameEn = surah?.nameEn?.ifBlank { null }
            ?: surah?.transliteration?.ifBlank { null }
            ?: "Surah $surahId"
        val surahNameAr = surah?.nameAr.orEmpty()
        val totalAyahs = surah?.ayahsCount ?: 0

        val hasFullSurah = "$slug/$surahId" in downloadedKeys
        val downloadedAyahs = keys.mapNotNull { k ->
            if (k !in downloadedKeys) return@mapNotNull null
            k.split("/").getOrNull(2)?.toIntOrNull()?.takeIf { it > 0 }
        }.distinct().sorted()

        val activeProgressValues = keys.mapNotNull { progress[it] }.filter { it >= 0f }
        val itemProgress = if (activeProgressValues.isEmpty() && keys.any { it in progress }) {
            0f
        } else if (activeProgressValues.isNotEmpty() && (!hasFullSurah || downloadedAyahs.size < totalAyahs)) {
            val done = downloadedAyahs.size + (if (hasFullSurah) 1 else 0)
            val total = (totalAyahs + 1).coerceAtLeast(1)
            (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }

        if (!hasFullSurah && downloadedAyahs.isEmpty() && itemProgress == null) {
            return@mapNotNull null
        }

        SurahDownloadItem(
            slug = slug,
            surahId = surahId,
            surahNameEn = surahNameEn,
            surahNameAr = surahNameAr,
            totalAyahs = totalAyahs,
            hasFullSurah = hasFullSurah,
            downloadedAyahs = downloadedAyahs,
            progress = itemProgress,
        )
    }
}

private fun buildItemSubtitle(item: SurahDownloadItem, playAsMemorize: Boolean): String {
    item.progress?.let { p ->
        val pct = (p * 100).toInt()
        return if (item.totalAyahs > 0) {
            "Downloading • ${item.downloadedAyahs.size}/${item.totalAyahs} Ayahs ($pct%)"
        } else {
            "Downloading $pct%"
        }
    }
    return if (playAsMemorize) {
        val total = item.totalAyahs.coerceAtLeast(item.downloadedAyahs.size)
        "Hifz • ${item.downloadedAyahs.size}/$total Ayahs offline"
    } else if (item.isMemorizeBundle) {
        "Full Surah + Hifz (${item.downloadedAyahs.size} Ayahs)"
    } else {
        "Full Surah • Offline"
    }
}

private fun reciterDisplayName(slug: String): String =
    QuranDataRepository.getBrowseReciterBySlug(slug)?.nameEn
        ?.takeIf { it.isNotBlank() } ?: slug.ifBlank { "Unknown reciter" }

private fun playSurahDownload(item: SurahDownloadItem, preferMemorize: Boolean) {
    if (item.surahId <= 0) return
    if (preferMemorize) {
        val startAyah = item.downloadedAyahs.firstOrNull() ?: 1
        AudioEngine.playSurahInAyahMode(item.surahId, item.slug, startAyah)
        return
    }
    val tracks = ScheduleTracks.buildOfflineTracks(
        QuranDownloads.downloadedKeys.value,
        QuranDataRepository.getReciters(),
        QuranDataRepository.getSurahs()
    )
    if (tracks.isEmpty()) return
    val startIndex = tracks.indexOfFirst {
        it.reciterSlug == item.slug && it.surahId == item.surahId
    }.takeIf { it >= 0 } ?: 0
    AudioEngine.playQueue(tracks, startIndex = startIndex)
}

private fun playQariSection(section: QariDownloadSection, category: DownloadCategory) {
    if (category == DownloadCategory.MEMORIZE) {
        val firstMem = section.items.firstOrNull { it.isMemorizeBundle } ?: return
        playSurahDownload(firstMem, preferMemorize = true)
        return
    }
    val keys = QuranDownloads.downloadedKeys.value
    val qariKeys = keys.filter { it.substringBefore("/") == section.slug }.toSet()
    if (qariKeys.any { it.split("/").size == 2 }) {
        val tracks = ScheduleTracks.buildOfflineTracks(
            keys,
            QuranDataRepository.getReciters(),
            QuranDataRepository.getSurahs(),
            qariKeys
        )
        if (tracks.isNotEmpty()) {
            AudioEngine.playQueue(tracks, startIndex = 0)
            return
        }
    }
    section.items.firstOrNull()?.let { first ->
        playSurahDownload(first, preferMemorize = first.isMemorizeBundle)
    }
}

private fun deleteSurahItem(item: SurahDownloadItem) {
    if (item.totalAyahs > 0 && item.isMemorizeBundle) {
        QuranDownloads.deleteSurahBundle(item.slug, item.surahId, item.totalAyahs)
    }
    item.downloadedAyahs.forEach { ayahNo ->
        QuranDownloads.deleteAyah(item.slug, item.surahId, ayahNo)
    }
    if (item.hasFullSurah) {
        QuranDownloads.delete(item.slug, item.surahId)
    }
}

private fun deleteDownloadKey(key: String) {
    val parts = key.split("/")
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
