package com.ghais.hifz.quests

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ghais.data.share.ShareSheet
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A9 — surah-completion ceremony: share-card text builder plus a Noir
 * celebration dialog (surah name, reciter, days taken) with a Share button.
 *
 * Trigger (wiring snippet in the report): wherever the app marks ayahs
 * mastered, when `getSurahMasteredPercentage(surahId)` reaches `1f` for a
 * surah that was previously incomplete, present [SurahCeremonyDialog].
 */

/**
 * Plain-text share card for a completed surah. Shared via
 * [ShareSheet.shareText] (common call; the iOS stub no-ops safely).
 */
fun surahCeremonyText(
    surahName: String,
    reciterName: String,
    daysTaken: Int,
    masteredCount: Int,
    totalAyahs: Int,
    streakDays: Int = 0,
): String = buildString {
    val dayWord = if (daysTaken == 1) "day" else "days"
    appendLine("Surah $surahName — memorized")
    appendLine()
    appendLine("$masteredCount of $totalAyahs ayahs mastered with $reciterName")
    appendLine("Completed in $daysTaken $dayWord • $streakDays-day streak")
    appendLine()
    append("Memorize, review & reflect with Ghaith")
}

/**
 * Celebration dialog: surah name as title, reciter + days taken + ayah count
 * as body, chrome Share pill and ghost Keep-going pill.
 */
@Composable
fun SurahCeremonyDialog(
    surahName: String,
    reciterName: String,
    daysTaken: Int,
    masteredCount: Int,
    totalAyahs: Int,
    streakDays: Int = 0,
    onDismiss: () -> Unit,
) {
    val dayWord = if (daysTaken == 1) "day" else "days"
    val cardText = surahCeremonyText(
        surahName = surahName,
        reciterName = reciterName,
        daysTaken = daysTaken,
        masteredCount = masteredCount,
        totalAyahs = totalAyahs,
        streakDays = streakDays,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ChromePillButton(
                text = "Share",
                onClick = {
                    ShareSheet.shareText(
                        title = "Ghaith — Surah complete",
                        text = cardText,
                    )
                },
            )
        },
        dismissButton = {
            GhostPillButton(
                text = "Keep going",
                onClick = onDismiss,
            )
        },
        title = {
            Text(
                text = "Surah $surahName complete",
                color = GhaisNoir.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "$masteredCount of $totalAyahs ayahs with $reciterName" +
                    " — finished in $daysTaken $dayWord.",
                color = GhaisNoir.TextSecondary,
                fontSize = 14.sp,
            )
        },
        shape = GhaisShapes.cardNoir,
        containerColor = GhaisNoir.CanvasTop,
        titleContentColor = GhaisNoir.TextPrimary,
        textContentColor = GhaisNoir.TextSecondary,
    )
}
