package com.ghais.ui.screens.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.math.roundToInt

/**
 * Noir Glass queue sheet: CanvasTop chrome + Scrim, IconWell header,
 * queue rows in the NoirListRow language (soft card fill + ghost border +
 * top-only specular). The active track reads through fill elevation
 * (cardFillActive + specular-bright rim + bold white title) — zero hue.
 *
 * Queue / audio logic (collects, skipToIndex, removeFromQueue, clear,
 * duration resolution) is preserved verbatim. Reordering is drag-based: a
 * 32dp handle per row, preview shifts drawn in-place, one engine move on
 * drop (see the grip comment block for the full ruleset).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val queue by AudioEngine.queue.collectAsState()
    val stationLabel by AudioEngine.stationLabel.collectAsState()
    val currentIndex by AudioEngine.currentIndex.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isAyahMode by AudioEngine.isAyahMode.collectAsState()
    val engineDurationMs by AudioEngine.durationMs.collectAsState()
    var query by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val normalizedQuery = query.trim().lowercase()
    val filteredQueue = queue.withIndex().filter { indexed ->
        val item = indexed.value
        normalizedQuery.isBlank() || listOf(
            item.surahNameEn,
            item.surahNameAr,
            item.reciterName,
            item.surahId.toString(),
            QuranDataRepository.getSurahById(item.surahId)?.ayahsCount?.toString().orEmpty()
        ).any { it.isNotBlank() && it.lowercase().contains(normalizedQuery) }
    }
    fun animateDismiss() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    // ── Drag-to-reorder state ────────────────────────────────────────────
    // Queue rows are uniform-height (fixed 40dp well, one title line, one
    // subtitle line), so the whole model is one measured stride plus the
    // finger's offset. The engine is touched ONCE, on drag end, with the
    // final (from, to) pair — QueueManager.move walks currentIndex the same
    // way the removed chevrons did — so mid-drag the queue can never
    // desync under a playing track, and one gesture never stacks moves.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    // Row height + the LazyColumn spacing (8.dp) — measured on first item.
    var rowStridePx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val listSpacingPx = with(density) { 8.dp.toPx() }

    // Integer-grid derivation: shift targets (and recomposition) only change
    // when the finger crosses a row boundary, not per pixel — the dragged
    // row itself tracks the finger from raw offset inside its graphics layer.
    val dragTarget by remember {
        derivedStateOf {
            val from = dragIndex ?: return@derivedStateOf -1
            val crossed = if (rowStridePx > 0f) (dragOffsetPx / rowStridePx).roundToInt() else 0
            (from + crossed).coerceIn(0, queue.size - 1)
        }
    }
    // Dragging needs a contiguous queue for the index arithmetic to hold:
    // while a search filter is on, the visible slice skips rows, so grips
    // hide and the existing tap-to-play stays the only action. One row = no-op.
    val dragEnabled = queue.size > 1 && normalizedQuery.isBlank()

    fun resetDragState() {
        dragIndex = null
        dragOffsetPx = 0f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Sheet chrome: IconWell header + ghost close well.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconWell(
                        icon = Icons.AutoMirrored.Filled.List,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = "Up Next",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (queue.isNotEmpty()) {
                            val posText = if (currentIndex in queue.indices) {
                                "Surah ${currentIndex + 1} of ${queue.size}"
                            } else {
                                "${queue.size} Surahs"
                            }
                            Text(
                                text = posText,
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (stationLabel != null) {
                            Text(
                                text = "Up next from $stationLabel",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Clear",
                        color = if (queue.isNotEmpty()) GhaisNoir.TextSecondary else GhaisNoir.TextDisabled,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .then(
                                if (queue.isNotEmpty()) Modifier.noirClickable { AudioEngine.clear() }
                                else Modifier
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                            .noirClickable { animateDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            NoirInsetField(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search surahs or reciter",
                                color = GhaisNoir.TextDisabled,
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(
                                color = GhaisNoir.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                .background(GhaisNoir.Fill2, CircleShape)
                                .noirClickable { query = "" },
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

            Spacer(modifier = Modifier.height(16.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconWell(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            size = 56.dp,
                            iconSize = 28.dp,
                            contentDescription = "Empty Queue",
                            tint = GhaisNoir.TextTertiary
                        )
                        Text(
                            text = "Queue is empty",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Play a surah or collection to see the queue",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (filteredQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No surahs match \"$query\"",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    itemsIndexed(
                        items = filteredQueue,
                        key = { _, entry -> "surah_${entry.value.surahId}_${entry.index}" }
                    ) { _, entry ->
                        val index = entry.index
                        val item = entry.value
                        val isActive = index == currentIndex || (currentIndex == -1 && currentTrack?.surahId == item.surahId)

                        val itemDuration = if (isActive && engineDurationMs > 0L) {
                            engineDurationMs
                        } else {
                            item.durationMs
                        }
                        val formattedDuration = if (itemDuration > 0L) formatQueueDuration(itemDuration) else ""

                        val surahTitle = when {
                            item.surahId > 0 && item.surahNameEn.isNotBlank() -> "${item.surahId}. ${item.surahNameEn}"
                            item.surahNameEn.isNotBlank() -> item.surahNameEn
                            item.surahId > 0 -> "Surah ${item.surahId}"
                            else -> "Unknown Surah"
                        }

                        val isDraggingRow = dragIndex == index
                        // Slot displacement while a drag crosses boundaries:
                        // rows between the dragged origin and the current
                        // target slide one stride in the opposite direction.
                        // (`di` capture: delegated state can't smart-cast.)
                        val di = dragIndex
                        val neighborShift = when {
                            di == null || isDraggingRow -> 0f
                            di < index && index <= dragTarget -> -rowStridePx
                            di > index && index >= dragTarget -> rowStridePx
                            else -> 0f
                        }
                        val neighborShiftAnim by animateFloatAsState(
                            targetValue = neighborShift,
                            animationSpec = tween(160, easing = FastOutSlowInEasing),
                            label = "queueRowShift"
                        )

                        // NoirListRow recipe: soft fill (+ active wash when playing),
                        // 1px card border (specular-bright when active), top specular.
                        // TrackItem carries no artwork (imageUrl is always null), so the
                        // typographic number well is the monochrome thumb — same as the
                        // explore SurahNoirRow.
                        Row(
                            modifier = Modifier
                                // Reorder visuals live on ONE layer: the dragged row
                                // follows the finger 1:1 (offset read inside the layer,
                                // so drag frames never hit the recomposer), while crossed
                                // neighbours ease to their displaced slot. Layer first so
                                // the clip + fill travel with the translation.
                                .graphicsLayer {
                                    translationY = if (isDraggingRow) dragOffsetPx else neighborShiftAnim
                                    if (isDraggingRow) {
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                        shape = GhaisShapes.row
                                        shadowElevation = with(density) { 8.dp.toPx() }
                                    }
                                }
                                .onSizeChanged {
                                    // Uniform rows: one stride covers every slot hop
                                    // (row height + the list's 8dp spacing).
                                    val measured = it.height + listSpacingPx
                                    if (rowStridePx != measured) rowStridePx = measured
                                }
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(
                                    if (isActive) GhaisNoir.cardFillActive()
                                    else GhaisNoir.cardFillSoft(),
                                    GhaisShapes.row
                                )
                                .border(
                                    1.dp,
                                    if (isActive) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    GhaisShapes.row
                                )
                                .topSpecular(inset = 22.dp)
                                .noirClickable { AudioEngine.skipToIndex(index) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GhaisNoir.wellFill())
                                    .border(
                                        1.dp,
                                        if (isActive) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Currently Playing",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${item.surahId.takeIf { it > 0 } ?: (index + 1)}",
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = surahTitle,
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (item.surahNameAr.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.surahNameAr,
                                            color = GhaisNoir.TextSecondary,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val surah = QuranDataRepository.getSurahById(item.surahId)
                                    val totalAyahs = surah?.ayahsCount ?: 7
                                    val subtitleText = if (isActive && isAyahMode) {
                                        val currentAyahNo = currentTrack?.ayahNo?.takeIf { it > 0 } ?: 1
                                        "${item.reciterName.ifBlank { "Quran Recitation" }} • Ayah $currentAyahNo of $totalAyahs • Playing"
                                    } else {
                                        listOfNotNull(
                                            item.reciterName.ifBlank { "Quran Recitation" },
                                            "$totalAyahs Ayahs",
                                            formattedDuration.takeIf { it.isNotEmpty() }
                                        ).joinToString(" • ")
                                    }
                                    Text(
                                        text = subtitleText,
                                        color = if (isActive && isAyahMode) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                            // Reorder affordance: a drag grip replaces the old
                            // up/down chevron wells. Rules of the gesture:
                            // 1. vertical drags are owned ONLY by this 32dp grip —
                            //    row taps and list scrolling elsewhere are untouched;
                            // 2. the shifted preview is visual-only, and the engine
                            //    reorders ONCE at drag end with the final (from, to),
                            //    reusing QueueManager.move (current-track remap intact);
                            // 3. the grip hides while a search filter is on — the
                            //    filtered slice is non-contiguous, so drag arithmetic
                            //    would move the wrong rows.
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dragEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDraggingRow) GhaisNoir.Fill2 else GhaisNoir.Fill1
                                            )
                                            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                            .pointerInput(Unit) {
                                                detectVerticalDragGestures(
                                                    onDragStart = {
                                                        dragIndex = index
                                                        dragOffsetPx = 0f
                                                    },
                                                    onVerticalDrag = { _, dragAmount ->
                                                        dragOffsetPx += dragAmount
                                                    },
                                                    onDragEnd = {
                                                        val from = dragIndex
                                                        if (from != null) {
                                                            val to = dragTarget
                                                            resetDragState()
                                                            if (to != from) {
                                                                AudioEngine.moveInQueue(from, to)
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = { resetDragState() }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Drag to reorder in queue",
                                            tint = if (isDraggingRow) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Fill1)
                                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                                        .noirClickable { AudioEngine.removeFromQueue(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove from queue",
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatQueueDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
