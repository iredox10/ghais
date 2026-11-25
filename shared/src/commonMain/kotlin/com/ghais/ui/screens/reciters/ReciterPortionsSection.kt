package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghais.data.repository.POPULAR_PORTIONS
import com.ghais.data.repository.PortionsStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.QuranPortion
import com.ghais.data.repository.toFullSurahTrackItem
import com.ghais.domain.model.Reciter
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

@Composable
fun ReciterPortionsSection(
    reciter: Reciter,
    onOpenPlayer: () -> Unit,
) {
    val customPortions by PortionsStore.customPortions.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val availablePortions = remember(customPortions, reciter.slug) {
        (customPortions + POPULAR_PORTIONS).filter { portion ->
            portion.surahIds.any { id -> reciter.isSurahAvailable(id) }
        }
    }

    if (availablePortions.isEmpty()) return

    fun playPortion(portion: QuranPortion) {
        val tracks = portion.surahIds.mapNotNull { id ->
            val surah = QuranDataRepository.getSurahById(id) ?: return@mapNotNull null
            if (!reciter.isSurahAvailable(id)) return@mapNotNull null
            surah.toFullSurahTrackItem(reciter)
        }
        if (tracks.isNotEmpty()) {
            AudioEngine.playQueue(tracks, startIndex = 0)
            onOpenPlayer()
        }
    }

    Spacer(modifier = Modifier.height(18.dp))
    NoirSectionHeader(
        label = "Portions",
        actionLabel = "+ Create",
        onAction = { showCreateDialog = true },
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(availablePortions, key = { it.id }) { portion ->
            PortionChipCard(
                portion = portion,
                onPlay = { playPortion(portion) },
                onDelete = if (portion.isCustom) {
                    { PortionsStore.remove(portion.id) }
                } else null,
            )
        }
    }

    if (showCreateDialog) {
        CreatePortionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, surahIds ->
                PortionsStore.addCustom(
                    name = name,
                    surahIds = surahIds,
                    reciterSlug = reciter.slug,
                )
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun PortionChipCard(
    portion: QuranPortion,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .width(164.dp)
            .clip(GhaisShapes.cardNoir)
            .background(if (portion.isCustom) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft())
            .border(
                1.dp,
                if (portion.isCustom) GhaisNoir.Lavender.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                GhaisShapes.cardNoir,
            )
            .topSpecular(inset = 18.dp)
            .noirClickable(onClick = onPlay)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.lavenderFill()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${portion.name}",
                    tint = GhaisNoir.LavenderInk,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .noirClickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete portion",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = portion.name,
            color = GhaisNoir.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = portion.subtitle,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CreatePortionDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, surahIds: List<Int>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val allSurahs = remember { QuranDataRepository.getSurahs() }
    val filteredSurahs = remember(allSurahs, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) allSurahs
        else allSurahs.filter { s ->
            s.nameEn.lowercase().contains(q) ||
                s.nameAr.contains(q) ||
                s.id.toString() == q
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = GhaisNoir.TextPrimary,
        unfocusedTextColor = GhaisNoir.TextPrimary,
        focusedLabelColor = GhaisNoir.TextTertiary,
        unfocusedLabelColor = GhaisNoir.TextDisabled,
        focusedBorderColor = Color.White,
        unfocusedBorderColor = GhaisNoir.BorderCard,
        cursorColor = Color.White,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(28.dp))
                .background(GhaisNoir.CanvasTop)
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(28.dp))
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Create custom portion",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${selectedIds.size} selected",
                    color = GhaisNoir.Lavender,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Portion name (e.g. Morning Wird)") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.insetFill())
                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search surahs",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(color = GhaisNoir.TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search surah by name or number...",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 13.sp,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredSurahs, key = { it.id }) { surah ->
                    val checked = surah.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GhaisShapes.row)
                            .background(if (checked) GhaisNoir.cardFillActive() else GhaisNoir.Fill2)
                            .border(
                                1.dp,
                                if (checked) GhaisNoir.Lavender.copy(alpha = 0.5f) else GhaisNoir.BorderGhost,
                                GhaisShapes.row,
                            )
                            .noirClickable {
                                selectedIds = if (checked) selectedIds - surah.id else selectedIds + surah.id
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .then(
                                    if (checked) Modifier.background(GhaisNoir.lavenderFill())
                                    else Modifier.background(GhaisNoir.wellFill())
                                )
                                .border(
                                    1.dp,
                                    if (checked) Color.Transparent else GhaisNoir.BorderCard,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = GhaisNoir.LavenderInk,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${surah.id}. ${surah.nameEn}",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = surah.nameAr,
                            color = GhaisNoir.TextTertiary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostPillButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                ChromePillButton(
                    text = "Save",
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { onCreate(name, selectedIds.sorted()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
