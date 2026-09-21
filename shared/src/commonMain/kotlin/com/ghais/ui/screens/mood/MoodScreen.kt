package com.ghais.ui.screens.mood

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.theme.GhaisColors
import com.ghais.ui.theme.GhaisTypography

// Obsidian-Emerald Glassmorphic Palette
private val SheetContainer = Color(0xFF181E20)       // Obsidian container
private val SheetBorder = Color(0x204EDEA3)          // Subtle emerald border
private val LiquidEmerald = Color(0xFF4EDEA3)        // Liquid Emerald accent
private val CardBackground = Color(0xFF1F2527)       // Elevated obsidian card
private val CardBorder = Color(0x184EDEA3)           // Card border
private val TextPrimary = Color(0xFFFFFFFF)          // High-emphasis white
private val TextSecondary = Color(0xFF8E989C)        // Muted subtitle

data class MoodItem(
    val id: String,
    val title: String,
    val category: String,
    val subtitle: String,
    val surahs: List<String>,
    val icon: ImageVector,
    val surahId: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val audioUrl: String
)

val AVAILABLE_MOODS = listOf(
    MoodItem(
        id = "gratitude",
        title = "Gratitude (Shukr)",
        category = "Reflection",
        subtitle = "Surah Ibrahim • Luqman • Ar-Rahman",
        surahs = listOf("Surah Ibrahim", "Surah Luqman", "Surah Ar-Rahman"),
        icon = Icons.Rounded.Face,
        surahId = 14,
        surahNameEn = "Ibrahim",
        surahNameAr = "إبراهيم",
        audioUrl = "https://server8.mp3quran.net/afs/014.mp3"
    ),
    MoodItem(
        id = "anxiety",
        title = "Anxiety & Relief",
        category = "Comfort",
        subtitle = "Ash-Sharh • Ad-Duha • Yusuf",
        surahs = listOf("Surah Ash-Sharh", "Surah Ad-Duha", "Surah Yusuf"),
        icon = Icons.Rounded.Favorite,
        surahId = 94,
        surahNameEn = "Ash-Sharh",
        surahNameAr = "الشرح",
        audioUrl = "https://server8.mp3quran.net/afs/094.mp3"
    ),
    MoodItem(
        id = "hifz",
        title = "Hifz Memorization",
        category = "Focus",
        subtitle = "Repeat loop mode • Steady pace recitations",
        surahs = listOf("Surah Al-Kahf", "Surah Yaseen", "Surah Al-Mulk"),
        icon = Icons.Rounded.Lightbulb,
        surahId = 18,
        surahNameEn = "Al-Kahf",
        surahNameAr = "الكهف",
        audioUrl = "https://server8.mp3quran.net/afs/018.mp3"
    ),
    MoodItem(
        id = "qiyam",
        title = "Qiyam al-Layl",
        category = "Night",
        subtitle = "Deep night recitations • Al-Mulk • Al-Muzzammil",
        surahs = listOf("Surah Al-Mulk", "Surah Al-Muzzammil", "Surah Al-Insan"),
        icon = Icons.Rounded.DarkMode,
        surahId = 67,
        surahNameEn = "Al-Mulk",
        surahNameAr = "الملك",
        audioUrl = "https://server8.mp3quran.net/afs/067.mp3"
    ),
    MoodItem(
        id = "study",
        title = "Deep Study & Focus",
        category = "Focus",
        subtitle = "Focus-oriented recitation with calming tempo",
        surahs = listOf("Surah Al-Kahf", "Surah Taha", "Surah Ar-Rahman"),
        icon = Icons.Default.GraphicEq,
        surahId = 20,
        surahNameEn = "Taha",
        surahNameAr = "طه",
        audioUrl = "https://server8.mp3quran.net/afs/020.mp3"
    ),
    MoodItem(
        id = "sleep",
        title = "Restful Sleep",
        category = "Night",
        subtitle = "Calming slow recitations to ease your mind",
        surahs = listOf("Surah Al-Mulk", "Surah As-Sajdah", "Surah Yaseen"),
        icon = Icons.Rounded.DarkMode,
        surahId = 32,
        surahNameEn = "As-Sajdah",
        surahNameAr = "السجدة",
        audioUrl = "https://server8.mp3quran.net/afs/032.mp3"
    )
)

/**
 * MoodSheet - Modal Bottom Sheet for selecting recitation mood.
 * Follows the Obsidian-Emerald glassmorphic theme strictly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodSheet(
    onDismiss: () -> Unit = {},
    initialMoodId: String = "gratitude",
    onMoodSelected: (MoodItem) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald.copy(alpha = 0.45f))
            )
        },
        modifier = Modifier.border(
            width = 1.dp,
            color = SheetBorder,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        )
    ) {
        MoodModalContent(
            initialMoodId = initialMoodId,
            onClose = onDismiss,
            onMoodSelected = { mood ->
                onMoodSelected(mood)
                onDismiss()
            }
        )
    }
}

/**
 * MoodScreen - Voyager Screen modal view for full-screen mood selection.
 */
data class MoodScreen(
    val initialMoodId: String = "gratitude"
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            containerColor = Color(0xFF111415),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recitation by Mood",
                        style = GhaisTypography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                MoodModalContent(
                    initialMoodId = initialMoodId,
                    onClose = { navigator.pop() },
                    onMoodSelected = { mood ->
                        AudioEngine.playTrack(
                            TrackItem(
                                reciterSlug = "mishary",
                                reciterName = "Sheikh Mishary Rashid Alafasy",
                                surahId = mood.surahId,
                                surahNameEn = mood.surahNameEn,
                                surahNameAr = mood.surahNameAr,
                                ayahNo = 0,
                                audioUrl = mood.audioUrl,
                                durationMs = 300000L
                            )
                        )
                        navigator.pop()
                    }
                )
            }
        }
    }
}

/**
 * Reusable modal content component for Mood selection.
 */
@Composable
fun MoodModalContent(
    initialMoodId: String = "gratitude",
    onClose: () -> Unit = {},
    onMoodSelected: (MoodItem) -> Unit = {}
) {
    var selectedMoodId by remember { mutableStateOf(initialMoodId) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Reflection", "Comfort", "Focus", "Night")

    val filteredMoods = remember(selectedCategory) {
        if (selectedCategory == "All") {
            AVAILABLE_MOODS
        } else {
            AVAILABLE_MOODS.filter { it.category == selectedCategory }
        }
    }

    val selectedMood = remember(selectedMoodId) {
        AVAILABLE_MOODS.find { it.id == selectedMoodId } ?: AVAILABLE_MOODS.first()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recitation by Mood",
                style = GhaisTypography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Curated recitations tailored to your spiritual state",
                style = GhaisTypography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category Filter Pills (Glowing Pill for active)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) LiquidEmerald.copy(alpha = 0.18f) else CardBackground
                )
                val pillBorder by animateColorAsState(
                    targetValue = if (isSelected) LiquidEmerald else CardBorder
                )
                val pillText by animateColorAsState(
                    targetValue = if (isSelected) LiquidEmerald else TextSecondary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(pillBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = pillBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        style = GhaisTypography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = pillText
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mood Items List
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredMoods) { mood ->
                val isSelected = selectedMoodId == mood.id
                MoodOptionCard(
                    mood = mood,
                    isSelected = isSelected,
                    onClick = { selectedMoodId = mood.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action Button (Liquid Emerald Pill with halo)
        Button(
            onClick = {
                onMoodSelected(selectedMood)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LiquidEmerald,
                contentColor = Color(0xFF003824)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF003824),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start ${selectedMood.title}",
                style = GhaisTypography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003824)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoodOptionCard(
    mood: MoodItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardBg by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald.copy(alpha = 0.12f) else CardBackground
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald.copy(alpha = 0.6f) else CardBorder
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box with emerald glow
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) LiquidEmerald.copy(alpha = 0.22f) else Color(0xFF141819)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) LiquidEmerald.copy(alpha = 0.4f) else CardBorder,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = mood.icon,
                contentDescription = null,
                tint = if (isSelected) LiquidEmerald else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mood.title,
                style = GhaisTypography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = mood.subtitle,
                style = GhaisTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Glowing Radio Button
        GlowingMoodRadioButton(selected = isSelected)
    }
}

@Composable
private fun GlowingMoodRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor by animateColorAsState(
        targetValue = if (selected) LiquidEmerald else Color(0xFF3C4A42)
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = ringColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald)
            )
        }
    }
}
