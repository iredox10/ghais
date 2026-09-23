package com.ghais.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.graphics.SolidColor
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
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.time.Clock

private const val HISTORY_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

/** True-grayscale filter — thumbs stay recognisable while strictly monochrome. */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

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

/**
 * Noir Glass — strict monochrome redesign.
 *
 * Canvas #050506 via [NoirScreenRoot] (glow zone -> absolute black + grain).
 * Engraved [NoirInsetField] search (white cursor, 24% hint, ghost clear),
 * history rows in the NoirListRow language with grayscale thumbs + 35% scrim,
 * ghost-well empty state, ghost circular replay affordance. Zero hue — state
 * reads through fill elevation, weight and opacity.
 *
 * Signature, grouping/filter/replay logic and navigation preserved.
 */
object HistoryScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        val history by UserUsageRepository.history.collectAsState()

        fun replaySurah(group: HistorySurahGroup) {
            val reciter = QuranDataRepository.getReciterBySlug(group.reciterSlug)
            // Never queue a surah this reciter never recorded (known-404 URL):
            // restrict the queue to available surahs only.
            val surahs = QuranDataRepository.getSurahs().filter { reciter.isSurahAvailable(it.id) }
            if (surahs.isEmpty()) return // "not recorded by this reciter" — do nothing.
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
            val exactIndex = allTracks.indexOfFirst { it.surahId == group.surahId }
            // Replayed surah unavailable — start from the nearest available one
            // (absolute id distance, tie-break forward so listening keeps moving ahead).
            val startIndex = if (exactIndex >= 0) {
                exactIndex
            } else {
                surahs.indices.minWithOrNull(
                    compareBy(
                        { kotlin.math.abs(surahs[it].id - group.surahId) },
                        { if (surahs[it].id >= group.surahId) 0 else 1 },
                        { surahs[it].id }
                    )
                ) ?: 0
            }
            val redirected = exactIndex < 0
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
                // A redirected start belongs to a different surah, so its saved
                // position does not apply — start from the beginning instead.
                val resumeAt = if (redirected) {
                    0L
                } else if (group.durationMs > 0L && group.positionMs >= group.durationMs - 5_000L) {
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

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                // Top bar — IconWell back + title + monochrome count chip.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.noirClickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        IconWell(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            size = 40.dp,
                            iconSize = 20.dp,
                            contentDescription = "Back"
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = "History",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filtered.size} surahs • last 30 days",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${filtered.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search — engraved inset, white cursor, 24% hint, ghost clear.
                NoirInsetField(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search history...",
                                    color = GhaisNoir.TextDisabled,
                                    fontSize = 13.5.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                    .background(GhaisNoir.Fill2, CircleShape)
                                    .noirClickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = GhaisNoir.TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (filtered.isEmpty()) {
                    // Ghost-well empty state — clay well + 100% headline + 62% hint.
                    NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconWell(
                                icon = Icons.Default.History,
                                size = 56.dp,
                                iconSize = 26.dp,
                                contentDescription = null,
                                tint = GhaisNoir.TextTertiary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matches in history" else "No history yet",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your listening history from the last 30 days will appear here.",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { it.key }
                        ) { group ->
                            HistoryNoirRow(
                                group = group,
                                onReplay = { replaySurah(group) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * History row in the NoirListRow language: soft card fill + 1px card border +
 * 22% top-only specular, grayscale thumb (desaturated + 35% black scrim,
 * monogram fallback), dual text, 38% timestamp, ghost circular replay
 * affordance with white glyph + ghost chevron.
 */
@Composable
private fun HistoryNoirRow(
    group: HistorySurahGroup,
    onReplay: () -> Unit
) {
    val photoUrl = remember(group.key) {
        resolveFollowedQari(group.reciterSlug)?.photoUrl
    }
    val thumbUrl = group.coverUrl.takeIf { it.isNotBlank() } ?: photoUrl

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onReplay)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbUrl != null) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = group.title,
                    contentScale = ContentScale.Crop,
                    colorFilter = NoirGrayscale,
                    modifier = Modifier.fillMaxSize()
                )
                // Darkening scrim keeps the plate recessed instead of glowing.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                Text(
                    text = group.title.take(1).uppercase(),
                    color = GhaisNoir.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${group.reciterName} • ${group.playsCount} plays",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = relativeTime(group.lastPlayedMs),
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape)
                        .noirClickable(onReplay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Replay",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
