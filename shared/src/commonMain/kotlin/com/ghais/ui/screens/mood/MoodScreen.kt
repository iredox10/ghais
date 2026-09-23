package com.ghais.ui.screens.mood

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.ghais.data.repository.QuranDataRepository
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

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
 * MoodSheet — strict Noir Glass monochrome.
 * CanvasTop container + Scrim + IconWell header + chrome check selection.
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
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        dragHandle = null
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

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    Text(
                        text = "Recitation by Mood",
                        style = GhaisTypography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                    )
                }
                MoodModalContent(
                    initialMoodId = initialMoodId,
                    onClose = { navigator.pop() },
                    onMoodSelected = { mood ->
                        // Defensive: never queue a surah the reciter has no audio for.
                        // Mishary carries the full catalog, so this is a no-op in practice.
                        val reciter = QuranDataRepository.getReciterBySlug("mishary")
                        if (reciter.isSurahAvailable(mood.surahId)) {
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp, top = 20.dp)
    ) {
        // Sheet header: IconWell + dual text + ghost close (sort-sheet pattern).
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
                    icon = Icons.Rounded.Favorite,
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = null
                )
                Column {
                    Text(
                        text = "Recitation by Mood",
                        style = GhaisTypography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                    )
                    Text(
                        text = "Curated recitations for your state",
                        style = GhaisTypography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = GhaisNoir.TextSecondary
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .noirClickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category filter: chrome pill (selected) / ghost pill (resting).
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.chromeFill())
                            .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                            .noirClickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = GhaisTypography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhaisNoir.OnChrome
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                            .noirClickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = GhaisTypography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = GhaisNoir.TextSecondary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mood items list — soft glass cards, active = elevated wash + chrome check.
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

        // Primary CTA — chrome pill.
        ChromePillButton(
            text = "Start ${selectedMood.title}",
            onClick = { onMoodSelected(selectedMood) },
            leadingIcon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MoodOptionCard(
    mood: MoodItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardFill = if (isSelected) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val cardBorder by animateColorAsState(
        targetValue = if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(cardFill)
            .border(1.dp, cardBorder, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(
            icon = mood.icon,
            size = 44.dp,
            iconSize = 22.dp,
            contentDescription = null,
            tint = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mood.title,
                style = GhaisTypography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = GhaisNoir.TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = mood.subtitle,
                style = GhaisTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = GhaisNoir.TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Chrome check when selected, ghost ring otherwise.
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(GhaisNoir.chromeFill(), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = GhaisNoir.OnChrome,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.5.dp, GhaisNoir.BorderCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}
