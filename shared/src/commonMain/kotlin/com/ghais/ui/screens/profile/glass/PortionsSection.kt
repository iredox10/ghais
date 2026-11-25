package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.POPULAR_PORTIONS
import com.ghais.data.repository.PortionsStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.QuranPortion
import com.ghais.data.repository.toFullSurahTrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

@Composable
fun PortionsProfileSection(
    onOpenPlayer: () -> Unit,
) {
    val customPortions by PortionsStore.customPortions.collectAsState()
    val followed by FollowStore.followedSlugs.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    val allPortions = remember(customPortions) {
        customPortions + POPULAR_PORTIONS
    }

    fun playPortion(portion: QuranPortion) {
        val slug = portion.reciterSlug ?: followed.firstOrNull() ?: "mishary"
        val reciter = QuranDataRepository.getReciterBySlug(slug)
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

    NoirSectionHeader(
        label = "Listening Portions",
        actionLabel = "+ Create",
        onAction = { showCreateDialog = true },
    )
    GlassCardContainer {
        allPortions.forEachIndexed { index, portion ->
            PortionRow(
                portion = portion,
                onPlay = { playPortion(portion) },
                onDelete = if (portion.isCustom) {
                    { PortionsStore.remove(portion.id) }
                } else null,
            )
            if (index < allPortions.lastIndex) {
                NoirRowDivider()
            }
        }
    }

    if (showCreateDialog) {
        CreatePortionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, fromSurah, toSurah ->
                PortionsStore.addRange(
                    name = name,
                    fromSurah = fromSurah,
                    toSurah = toSurah,
                    reciterSlug = followed.firstOrNull(),
                )
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun PortionRow(
    portion: QuranPortion,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noirClickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (portion.isCustom) GhaisNoir.lavenderFill() else GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = if (portion.isCustom) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = portion.name,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = portion.subtitle,
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onDelete != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill2)
                    .noirClickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete portion",
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play ${portion.name}",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CreatePortionDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, fromSurah: Int, toSurah: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var fromSurah by remember { mutableIntStateOf(1) }
    var toSurah by remember { mutableIntStateOf(18) }

    val fromName = QuranDataRepository.getSurahById(fromSurah)?.nameEn ?: "Surah $fromSurah"
    val toName = QuranDataRepository.getSurahById(toSurah)?.nameEn ?: "Surah $toSurah"

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
                .clip(RoundedCornerShape(28.dp))
                .background(GhaisNoir.CanvasTop)
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(28.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "Create custom portion",
                color = GhaisNoir.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pick any surah range to listen to as your own portion.",
                color = GhaisNoir.TextTertiary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Portion name (e.g. Morning Wird)") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            SurahStepperRow(
                label = "From",
                surahId = fromSurah,
                surahName = fromName,
                onMinus = {
                    val next = (fromSurah - 1).coerceAtLeast(1)
                    fromSurah = next
                    if (toSurah < next) toSurah = next
                },
                onPlus = {
                    val next = (fromSurah + 1).coerceAtMost(114)
                    fromSurah = next
                    if (toSurah < next) toSurah = next
                },
            )
            Spacer(Modifier.height(10.dp))
            SurahStepperRow(
                label = "To",
                surahId = toSurah,
                surahName = toName,
                onMinus = { toSurah = (toSurah - 1).coerceAtLeast(fromSurah) },
                onPlus = { toSurah = (toSurah + 1).coerceAtMost(114) },
            )
            Spacer(Modifier.height(18.dp))
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
                    onClick = { onCreate(name, fromSurah, toSurah) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SurahStepperRow(
    label: String,
    surahId: Int,
    surahName: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.row)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
            )
            Text(
                text = "$surahId. $surahName",
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onMinus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Previous surah",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onPlus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Next surah",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
