package com.ghais.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen

private val PureBlack = Color(0xFF000000)
private val MutedGrey = Color(0xFF9A9AA0)
private val LinkBlue = Color(0xFF4C8DFF)

object MyRoutinesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val routines by CustomRoutinesStore.routines.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            // Header: back + "My Routines" + count
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
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable { navigator.pop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Routines",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (routines.isEmpty()) "No routines yet"
                        else "${routines.size} routine${if (routines.size == 1) "" else "s"}",
                        color = MutedGrey,
                        fontSize = 13.sp
                    )
                }
            }

            // "New routine" pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LinkBlue)
                    .clickable { navigator.push(RoutineEditorScreen(null)) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New routine",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New routine",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (routines.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            tint = LinkBlue.copy(alpha = 0.85f),
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No routines yet",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your first routine to bundle surahs with your favourite reciters.",
                        color = MutedGrey,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
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

@Composable
private fun RoutineCard(
    routine: CustomRoutine,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublic: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${routine.items.size} surah${if (routine.items.size == 1) "" else "s"} • " +
                        if (routine.isPublic) "Public" else "Private",
                    color = MutedGrey,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LinkBlue)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play routine",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (routine.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = routine.description,
                color = MutedGrey,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Overflow row: edit / share / delete + public toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit routine",
                    tint = MutedGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share routine",
                    tint = MutedGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete routine",
                    tint = MutedGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (routine.isPublic) "Public" else "Private",
                color = MutedGrey,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = routine.isPublic,
                onCheckedChange = onTogglePublic,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = LinkBlue,
                    checkedThumbColor = Color.White
                )
            )
        }
    }
}
