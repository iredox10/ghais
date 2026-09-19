package com.quranify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
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
import com.quranify.player.AmbientMixer
import com.quranify.player.AmbientType

// Keep old tokens for any external callers (PresetChip / AmbientChannelItem removed in redesign).
val quranifyBackground = Color(0xFF0A0A0F)
val quranifySurface = Color(0xFF141419)
val quranifyCard = Color(0xFF1C1C24)
val quranifyPrimary = Color(0xFFD4A853)
val quranifySecondary = Color(0xFF4A8C6F)
val quranifyTextPrimary = Color(0xFFF0EDE6)
val quranifyTextSecondary = Color(0xFF8A8A96)
val quranifyError = Color(0xFFCF6679)

private val SheetBg = Color(0xFF1E1E1E)
private val TileSelectedBg = Color(0xFF2E2E30)
private val FieldBg = Color(0xFF2C2C2E)
private val CircleBtnBg = Color(0xFF2C2C2E)
private val ConfirmBg = Color(0xFFE8E8ED)
private val BadgeGrey = Color(0xFF4A4A4C)
private val BadgeLight = Color(0xFFC7C7CC)
private val MutedPlaceholder = Color(0xFF6E6E73)

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
        containerColor = SheetBg,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF48484A))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: X | title | confirm-check
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CircleBtnBg)
                        .clickable { onDismissRequest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Background sound",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ConfirmBg)
                        .clickable { onDismissRequest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Confirm",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(FieldBg)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MutedPlaceholder,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Thunder...",
                                color = MutedPlaceholder,
                                fontSize = 17.sp
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(visible, key = { it.key }) { option ->
                    SoundTile(
                        option = option,
                        selected = selected == option.type,
                        onClick = { mixer.selectExclusive(option.type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundTile(
    option: SoundOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) TileSelectedBg else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (option.isNoSounds) {
                // White circle with minus, like the screenshot
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = null,
                        tint = SheetBg,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            // Top-end badge: check when selected, download when not yet downloaded
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BadgeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
