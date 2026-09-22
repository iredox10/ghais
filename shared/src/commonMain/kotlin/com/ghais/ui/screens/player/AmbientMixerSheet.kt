package com.ghais.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storm
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ghais.player.AmbientMixer
import com.ghais.player.AmbientType
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

private data class SoundOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val type: AmbientType?,
    val needsDownload: Boolean,
    val isNoSounds: Boolean = false
)

private fun soundOptions(): List<SoundOption> = listOf(
    SoundOption("none", "No sounds", Icons.Filled.Remove, null, false, isNoSounds = true),
    SoundOption("rain", "Rain", Icons.Filled.Umbrella, AmbientType.Rain, false),
    SoundOption("birds", "Birds", Icons.Filled.FlutterDash, AmbientType.Birdsong, false),
    SoundOption("fire", "Fire", Icons.Filled.Whatshot, AmbientType.Fire, false),
    SoundOption("waves", "Waves", Icons.Filled.Waves, AmbientType.OceanWaves, false),
    SoundOption("wind", "Wind", Icons.Filled.Air, AmbientType.GentleWind, false),
    SoundOption("cat", "Cat", Icons.Filled.Pets, AmbientType.Cat, true),
    SoundOption("owl", "Owl", Icons.Filled.Face, AmbientType.Owl, true),
    SoundOption("river", "River", Icons.Filled.WaterDrop, AmbientType.River, true),
    SoundOption("whale", "Whale", Icons.Filled.Cloud, AmbientType.Whale, true),
    SoundOption("crickets", "Crickets", Icons.Filled.DarkMode, AmbientType.NightCrickets, true),
    SoundOption("thunder", "Thunder", Icons.Filled.Thunderstorm, AmbientType.Thunder, true),
    SoundOption("storm", "Storm", Icons.Filled.Storm, AmbientType.Storm, true),
    SoundOption("train", "Train", Icons.Filled.Train, AmbientType.Train, true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientMixerSheet(
    mixer: AmbientMixer,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    fun dismiss() {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismissRequest()
            }
        }
    }

    val channels by mixer.channels.collectAsState()
    val allOptions = remember { soundOptions() }
    // Live selection: highlight follows the mixer, taps apply instantly
    // so the user hears the sound the moment they pick it.
    val selected: AmbientType? = remember(channels) {
        channels.firstOrNull { it.isEnabled }?.type
    }
    var query by remember { mutableStateOf("") }

    val visible = remember(query) {
        if (query.isBlank()) allOptions
        else allOptions.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header — sort-sheet recipe: IconWell + title, ghost close disc.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconWell(
                        icon = Icons.Filled.Waves,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null
                    )
                    Text(
                        text = "Background sound",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .noirClickable { dismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Search — engraved inset field, monochrome.
            NoirInsetField {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = GhaisNoir.TextPrimary, fontSize = 17.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Thunder...",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 17.sp
                                )
                            }
                            inner()
                        }
                    )
                    if (query.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill4)
                                .noirClickable { query = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = GhaisNoir.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visible, key = { it.key }) { option ->
                    SoundTile(
                        option = option,
                        selected = if (option.isNoSounds) selected == null else selected == option.type,
                        onClick = {
                            if (option.isNoSounds || option.type == null) {
                                mixer.selectExclusive(null)
                            } else {
                                mixer.selectExclusive(option.type)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SoundTile(
    option: SoundOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Grid variant of the sort-sheet row recipe: active fill + chrome check when selected.
    Column(
        modifier = Modifier
            .clip(GhaisShapes.row)
            .background(
                if (selected) GhaisNoir.cardFillActive()
                else GhaisNoir.cardFillSoft()
            )
            .border(
                1.dp,
                if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.row
            )
            .topSpecular(inset = 16.dp)
            .noirClickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            IconWell(
                icon = option.icon,
                size = 52.dp,
                iconSize = 24.dp,
                contentDescription = option.label,
                tint = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
            )
            // Chrome check disc when selected — state reads through fill + chrome, never hue.
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
