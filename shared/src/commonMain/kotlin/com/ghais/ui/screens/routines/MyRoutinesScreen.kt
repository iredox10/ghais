package com.ghais.ui.screens.routines

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.CustomRoutinesStore
import com.ghais.data.repository.CustomRoutine
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.toTrackItems
import com.ghais.data.share.ShareSheet
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

object MyRoutinesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val routines by CustomRoutinesStore.routines.collectAsState()

        NoirScreenRoot {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header: ghost back + dual text + count.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                            .noirClickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Routines",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = if (routines.isEmpty()) "No routines yet"
                            else "${routines.size} routine${if (routines.size == 1) "" else "s"}",
                            color = GhaisNoir.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Chrome "New routine" CTA.
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ChromePillButton(
                        text = "New routine",
                        onClick = { navigator.push(RoutineEditorScreen(null)) },
                        leadingIcon = Icons.Filled.Add,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (routines.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.cardLarge)
                                .background(GhaisNoir.cardFillSoft())
                                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardLarge)
                                .topSpecular(inset = 30.dp)
                                .padding(vertical = 36.dp, horizontal = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconWell(
                                    icon = Icons.Filled.QueueMusic,
                                    size = 56.dp,
                                    iconSize = 26.dp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "No routines yet",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Create your first routine to bundle surahs with your favourite reciters.",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(routines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                onPlay = {
                                    val tracks = routine.toTrackItems()
                                    if (tracks.isNotEmpty()) {
                                        AudioEngine.playQueue(tracks)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                                onEdit = { navigator.push(RoutineEditorScreen(routine.id)) },
                                onShare = {
                                    if (ShareSheet.isAvailable()) {
                                        ShareSheet.shareText(
                                            title = routine.title,
                                            text = buildShareText(routine)
                                        )
                                    }
                                },
                                onDelete = { CustomRoutinesStore.delete(routine.id) },
                                onTogglePublic = { isPublic ->
                                    CustomRoutinesStore.setPublic(routine.id, isPublic)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildShareText(routine: CustomRoutine): String {
        val surahs = QuranDataRepository.getSurahs()
        val lines = routine.items.map { item ->
            val surahName = surahs.find { it.id == item.surahId }?.nameEn
                ?: "Surah ${item.surahId}"
            val reciterName = QuranDataRepository.getReciterBySlug(item.reciterSlug).nameEn
            "• $surahName — $reciterName"
        }
        return buildString {
            append("My '${routine.title}' routine on Ghais:\n")
            lines.forEach { append(it).append("\n") }
            append("Listen with me!")
        }
    }
}

/**
 * Noir routine card: soft fill + BorderCard + top specular + sheen,
 * chrome play disc, ghost affordances, NoirSwitch. Zero hue.
 */
@Composable
private fun RoutineCard(
    routine: CustomRoutine,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublic: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular(inset = 26.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWell(icon = Icons.Filled.QueueMusic, size = 44.dp, iconSize = 22.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${routine.items.size} surah${if (routine.items.size == 1) "" else "s"} • " +
                            if (routine.isPublic) "Public" else "Private",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                // Chrome play disc — chromium carries the affordance, never hue.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .noirClickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play routine",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (routine.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = routine.description,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overflow row: ghost affordances + NoirSwitch.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GhostCircleButton(icon = Icons.Filled.Edit, description = "Edit routine", onClick = onEdit)
                GhostCircleButton(icon = Icons.Filled.Share, description = "Share routine", onClick = onShare)
                GhostCircleButton(icon = Icons.Filled.Delete, description = "Delete routine", onClick = onDelete)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (routine.isPublic) "Public" else "Private",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                NoirSwitch(
                    checked = routine.isPublic,
                    onCheckedChange = onTogglePublic
                )
            }
        }
    }
}

@Composable
private fun GhostCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(GhaisNoir.Fill1)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
