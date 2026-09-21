package com.quranify.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.quranify.data.repository.QuranDataRepository
import com.quranify.data.repository.UserUsageRepository
import com.quranify.data.repository.resolveFollowedQari
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.screens.player.NowPlayingScreen
import kotlin.time.Clock

private const val HISTORY_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

private val PureBlack = Color(0xFF000000)
private val MutedGrey = Color(0xFF9A9AA0)
private val LinkBlue = Color(0xFF4C8DFF)
private val DarkCard = Color(0xFF1C1C1E)

private data class HistorySurahGroup(
    val key: String,
    val surahId: Int,
    val reciterSlug: String,
    val title: String,
    val reciterName: String,
    val coverUrl: String,
    val lastPlayedMs: Long,
    val playsCount: Int,
    val positionMs: Long,
    val durationMs: Long,
)

private fun relativeTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "recently"
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diffMs = (nowMs - timestampMs).coerceAtLeast(0L)
    val minutes = diffMs / 60_000L
    if (minutes < 1) return "just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    return "${days}d ago"
}

object HistoryScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        val history by UserUsageRepository.history.collectAsState()

        fun replaySurah(group: HistorySurahGroup) {
            val reciter = QuranDataRepository.getReciterBySlug(group.reciterSlug)
            val surahs = QuranDataRepository.getSurahs()
            val allTracks = surahs.map { s ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = s.id,
                    surahNameEn = s.nameEn,
                    surahNameAr = s.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(s.id),
                    durationMs = s.ayahsCount * 15_000L
                )
            }
            val startIndex = allTracks.indexOfFirst { it.surahId == group.surahId }.coerceAtLeast(0)
            val currentTrack = AudioEngine.currentTrack.value
            val isCurrent = currentTrack != null &&
                currentTrack.surahId == group.surahId &&
                currentTrack.reciterSlug == reciter.slug
            if (isCurrent && AudioEngine.isPlaying.value) {
                navigator.push(NowPlayingScreen())
            } else if (isCurrent) {
                AudioEngine.resume()
                navigator.push(NowPlayingScreen())
            } else {
                val resumeAt = if (group.durationMs > 0L && group.positionMs >= group.durationMs - 5_000L) {
                    0L
                } else {
                    group.positionMs.coerceAtLeast(0L)
                }
                AudioEngine.playQueue(allTracks, startIndex = startIndex, startPositionMs = resumeAt)
                navigator.push(NowPlayingScreen())
            }
        }

        val groups = remember(history) {
            val cutoff = Clock.System.now().toEpochMilliseconds() - HISTORY_WINDOW_MS
            history.filter { it.lastPlayedTimestampMs == 0L || it.lastPlayedTimestampMs >= cutoff }
                .groupBy {
                    // Canonicalize the reciter so slug variants ("mishary" vs "alafasy")
                    // collapse into one entry instead of duplicates.
                    val canonical = resolveFollowedQari(it.reciterSlug)?.reciter?.slug
                        ?: it.reciterSlug.trim().lowercase().replace("_", "-")
                    "${it.surahId}|$canonical"
                }
                .mapNotNull { (key, perSurah) ->
                    if (perSurah.isEmpty()) return@mapNotNull null
                    val latest = perSurah.maxByOrNull { it.lastPlayedTimestampMs } ?: return@mapNotNull null
                    val resolvedName = resolveFollowedQari(latest.reciterSlug)?.reciter?.nameEn
                    HistorySurahGroup(
                        key = key,
                        surahId = latest.surahId,
                        reciterSlug = latest.reciterSlug,
                        title = latest.title.ifBlank { "Surah ${latest.surahId}" },
                        reciterName = resolvedName
                            ?: latest.subtitle.substringBefore("•").trim().ifEmpty { latest.reciterSlug },
                        coverUrl = latest.coverUrl,
                        lastPlayedMs = perSurah.maxOf { it.lastPlayedTimestampMs },
                        playsCount = perSurah.size,
                        positionMs = latest.positionMs,
                        durationMs = latest.durationMs,
                    )
                }
                .sortedByDescending { it.lastPlayedMs }
        }

        val filtered = remember(groups, searchQuery) {
            if (searchQuery.isBlank()) groups
            else groups.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.reciterName.contains(searchQuery, ignoreCase = true)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LinkBlue.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "History",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filtered.size} surahs • last 30 days",
                            color = MutedGrey,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MutedGrey,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search history...",
                                    color = MutedGrey,
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
                        Text(
                            text = "Nothing here yet — your listening history from the last 30 days will appear here.",
                            color = MutedGrey,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 112.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { it.key }
                        ) { group ->
                            val photoUrl = remember(group.key) {
                                resolveFollowedQari(group.reciterSlug)?.photoUrl
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .clickable { replaySurah(group) }
                                    .padding(12.dp)
                            ) {
                                if (group.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = group.coverUrl,
                                        contentDescription = group.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(DarkCard)
                                    )
                                } else if (photoUrl != null) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = group.reciterName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(DarkCard)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color.White.copy(alpha = 0.10f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = group.title.take(1),
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.title,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${group.reciterName} • ${group.playsCount} plays",
                                        color = MutedGrey,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = relativeTime(group.lastPlayedMs),
                                        color = MutedGrey,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Replay",
                                        tint = LinkBlue,
                                        modifier = Modifier.size(24.dp)
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
