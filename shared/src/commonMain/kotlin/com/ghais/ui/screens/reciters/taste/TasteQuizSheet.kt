package com.ghais.ui.screens.reciters.taste

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * 3-step taste-onboarding bottom sheet (AGENT A1), Noir Glass style.
 *
 * - Step 1: pace pick ([Pace]: SLOW / MEASURED / FLOWING).
 * - Step 2: mood pick ([Mood]: CALM / UPLIFTING / FOCUS).
 * - Step 3: A/B voice test — two play buttons preview two contrasting
 *   reciters ([OPTION_A_SLUG] vs [OPTION_B_SLUG], Al-Fatiha ayah 1 via
 *   [AudioEngine.playAyah]); the user picks the one that moves them and the
 *   verdict is recorded as liked/skipped slugs in [TasteStore].
 *
 * Picks persist incrementally ([TasteStore.savePace]/[saveMood]/
 * [recordVoicePick]); [TasteStore.markCompleted] fires only on the step-3
 * verdict, which is what the RecitersScreen first-visit gate reads. Audio
 * preview is paused on dismiss and on finish.
 *
 * AudioEngine surface used (see `player/AudioEngine.kt`): [AudioEngine.playAyah]
 * for previews, [AudioEngine.pause] to stop them, [AudioEngine.currentTrack] /
 * [AudioEngine.isPlaying] to render the play/pause pills.
 */
const val TASTE_PREVIEW_SURAH_ID: Int = 1
const val TASTE_PREVIEW_AYAH_NO: Int = 1

/** Modern Murattal, measured/flowing — side A of the voice test. */
const val TASTE_OPTION_A_SLUG: String = "mishary"

/** Mujawwad heritage voice, slow/ornamented — side B of the voice test. */
const val TASTE_OPTION_B_SLUG: String = "abdul-basit-mujawwad"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteQuizSheet(
    onDismiss: () -> Unit,
    onFinished: () -> Unit = onDismiss
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saved = TasteStore.taste.collectAsState().value

    var step by remember { mutableIntStateOf(0) }
    var pace by remember(saved.pace) { mutableStateOf(saved.pace) }
    var mood by remember(saved.mood) { mutableStateOf(saved.mood) }

    fun stopPreview() {
        try {
            AudioEngine.pause()
        } catch (_: Exception) {
        }
    }

    fun dismissQuietly() {
        stopPreview()
        onDismiss()
    }

    val optionA = remember { QuranDataRepository.getReciterBySlug(TASTE_OPTION_A_SLUG) }
    val optionB = remember { QuranDataRepository.getReciterBySlug(TASTE_OPTION_B_SLUG) }

    ModalBottomSheet(
        onDismissRequest = ::dismissQuietly,
        sheetState = sheetState,
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        icon = Icons.Filled.Check,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = "Find your voice",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Step ${step + 1} of 3",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                        .noirClickable(::dismissQuietly),
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

            Spacer(Modifier.height(14.dp))

            // Progress dots — fill elevation only, zero hue.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) GhaisNoir.TextPrimary else GhaisNoir.Fill2)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            when (step) {
                0 -> TastePaceStep(
                    selected = pace,
                    onSelect = {
                        pace = it
                        TasteStore.savePace(it)
                    }
                )
                1 -> TasteMoodStep(
                    selected = mood,
                    onSelect = {
                        mood = it
                        TasteStore.saveMood(it)
                    }
                )
                else -> TasteVoiceStep(
                    optionAName = optionA.nameEn.ifBlank { TASTE_OPTION_A_SLUG },
                    optionBName = optionB.nameEn.ifBlank { TASTE_OPTION_B_SLUG },
                    onPick = { winnerSlug, loserSlug ->
                        TasteStore.recordVoicePick(winnerSlug, loserSlug)
                        TasteStore.markCompleted()
                        stopPreview()
                        onFinished()
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            // Footer nav: Back / Skip / Continue — chrome primary, ghost secondary.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    TasteGhostButton(
                        label = "Back",
                        modifier = Modifier.weight(1f),
                        onClick = { step -= 1 }
                    )
                }
                if (step < 2) {
                    TasteGhostButton(
                        label = "Skip for now",
                        modifier = Modifier.weight(1f),
                        onClick = ::dismissQuietly
                    )
                    TasteChromeButton(
                        label = if (step == 1) "Hear voices" else "Continue",
                        modifier = Modifier.weight(1f),
                        onClick = { step += 1 }
                    )
                } else {
                    TasteGhostButton(
                        label = "Skip voice test",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = ::dismissQuietly
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TastePaceStep(
    selected: Pace?,
    onSelect: (Pace) -> Unit
) {
    TasteStepTitle(
        title = "How do you love\nto listen?",
        subtitle = "Pick the pace that holds your heart."
    )
    Spacer(Modifier.height(14.dp))
    val options = listOf(
        Pace.SLOW to ("Unhurried" to "Slow, weighty, every word lands"),
        Pace.MEASURED to ("Steady" to "Balanced pace for daily listening"),
        Pace.FLOWING to ("Flowing" to "Smooth and melodic, carries you along")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((value, labels) in options) {
            TasteOptionCard(
                title = labels.first,
                subtitle = labels.second,
                active = selected == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun TasteMoodStep(
    selected: Mood?,
    onSelect: (Mood) -> Unit
) {
    TasteStepTitle(
        title = "What should it\nfeel like?",
        subtitle = "Choose the mood you return to."
    )
    Spacer(Modifier.height(14.dp))
    val options = listOf(
        Mood.CALM to ("Calm" to "Softens the evening, settles the heart"),
        Mood.UPLIFTING to ("Uplifting" to "Bright and moving, lifts the spirit"),
        Mood.FOCUS to ("Focus" to "Clear and steady, made for study")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((value, labels) in options) {
            TasteOptionCard(
                title = labels.first,
                subtitle = labels.second,
                active = selected == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun TasteVoiceStep(
    optionAName: String,
    optionBName: String,
    onPick: (winnerSlug: String, loserSlug: String) -> Unit
) {
    TasteStepTitle(
        title = "Which voice\nmoves you?",
        subtitle = "Play both, then choose the one that holds you."
    )
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TasteVoiceCard(
            name = optionAName,
            slug = TASTE_OPTION_A_SLUG,
            tag = "Voice A",
            onPick = { onPick(TASTE_OPTION_A_SLUG, TASTE_OPTION_B_SLUG) }
        )
        TasteVoiceCard(
            name = optionBName,
            slug = TASTE_OPTION_B_SLUG,
            tag = "Voice B",
            onPick = { onPick(TASTE_OPTION_B_SLUG, TASTE_OPTION_A_SLUG) }
        )
    }
}

@Composable
private fun TasteStepTitle(title: String, subtitle: String) {
    Text(
        text = title,
        style = GhaisTypography.displayEditorialSmall,
        color = GhaisNoir.TextPrimary
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        color = GhaisNoir.TextSecondary,
        fontSize = 13.sp
    )
}

@Composable
private fun TasteOptionCard(
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val fill = if (active) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(fill)
            .border(1.dp, border, GhaisShapes.row)
            .topSpecular()
            .noirClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextTertiary,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (active) Modifier.background(GhaisNoir.chromeFill())
                    else Modifier.background(GhaisNoir.Fill2).border(1.dp, GhaisNoir.BorderCard, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = GhaisNoir.OnChrome,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun TasteVoiceCard(
    name: String,
    slug: String,
    tag: String,
    onPick: () -> Unit
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val isMine = currentTrack?.reciterSlug == slug &&
        currentTrack?.surahId == TASTE_PREVIEW_SURAH_ID
    val activelyPlaying = isMine && isPlaying

    val fill = if (activelyPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(fill)
            .border(1.dp, border, GhaisShapes.cardNoir)
            .topSpecular()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Preview play/pause pill: chrome when actively playing, smoked otherwise.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        if (activelyPlaying) Modifier.background(GhaisNoir.chromeFill())
                        else Modifier.background(GhaisNoir.Fill2)
                    )
                    .border(
                        1.dp,
                        if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                        CircleShape
                    )
                    .noirClickable {
                        if (activelyPlaying) {
                            try {
                                AudioEngine.pause()
                            } catch (_: Exception) {
                            }
                        } else {
                            try {
                                AudioEngine.playAyah(
                                    surahId = TASTE_PREVIEW_SURAH_ID,
                                    ayahNo = TASTE_PREVIEW_AYAH_NO,
                                    reciterSlug = slug
                                )
                            } catch (_: Exception) {
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (activelyPlaying) "Pause preview" else "Play preview",
                    tint = if (activelyPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = name,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Al-Fatiha · ayah 1 preview",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.chromeFill())
                .noirClickable(onClick = onPick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This one moves me",
                color = GhaisNoir.OnChrome,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TasteChromeButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.chromeFill())
            .noirClickable(onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = GhaisNoir.OnChrome,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TasteGhostButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
