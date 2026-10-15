package com.ghais.ui.screens.home.moments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.theme.GhaisNoir

/**
 * Agent A8 — time-aware Home hero card (Noir styled).
 *
 * Greeting + gentle copy from [momentFor], one-tap play of the suggested
 * surah with the default reciter (`getFallbackReciter()`, the same
 * fallback [HomeScreen]'s resume path degrades to — never a hardcoded
 * slug). Mirrors the Daily Mix hero recipe: [NoirHeroCard] +
 * [ChromePillButton], guarded play that never throws.
 *
 * @param hour local 24h hour; the caller supplies it from the platform
 *   clock (see the Home insertion snippet in the A8 report).
 * @param prayerHint optional prayer hint forwarded to [momentFor].
 * @param onOpenPlayer navigate to NowPlaying after queueing, mirroring
 *   `HomeDailyMixShelf(onOpenPlayer)`.
 */
@Composable
fun MomentHeroCard(
    hour: Int,
    prayerHint: String? = null,
    onOpenPlayer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val suggestion = remember(hour, prayerHint) { momentFor(hour, prayerHint) }
    val surah = remember(suggestion.surahId) {
        try {
            QuranDataRepository.getSurahById(suggestion.surahId)
        } catch (_: Exception) {
            null
        }
    }
    val surahName = surah?.nameEn?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Surah ${suggestion.surahId}"
    val surahMeta = remember(surah) {
        if (surah == null) {
            ""
        } else {
            listOfNotNull(
                "Surah ${surah.id}",
                surah.ayahsCount.takeIf { it > 0 }?.let { "$it Ayahs" },
                surah.revelationType.trim().takeIf { it.isNotEmpty() },
            ).joinToString(" • ")
        }
    }

    NoirHeroCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = suggestion.moment.name.replace('_', ' '),
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = suggestion.greeting,
                color = GhaisNoir.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = suggestion.copy,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (surahMeta.isNotEmpty()) "$surahName • $surahMeta" else surahName,
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(16.dp))
            ChromePillButton(
                text = "${suggestion.playAction.labelPrefix} $surahName",
                onClick = {
                    try {
                        val track = momentTrack(suggestion.surahId) ?: return@ChromePillButton
                        AudioEngine.playQueue(listOf(track), startIndex = 0)
                        onOpenPlayer()
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.PlayArrow,
            )
        }
    }
}

/**
 * Builds the single-track queue for the suggested surah with the default
 * reciter. Null when the reciter has no playable surahs (catalog
 * unavailable) — the caller then leaves the tap inert rather than
 * queueing the wrong content.
 */
private fun momentTrack(surahId: Int): TrackItem? {
    val reciter = QuranDataRepository.getFallbackReciter()
    val surah = QuranDataRepository.getSurahById(surahId) ?: return null
    if (!reciter.isSurahAvailable(surah.id)) return null
    return TrackItem(
        reciterSlug = reciter.slug,
        reciterName = reciter.nameEn,
        surahId = surah.id,
        surahNameEn = surah.nameEn,
        surahNameAr = surah.nameAr,
        ayahNo = 0,
        audioUrl = reciter.getFullSurahUrl(surah.id),
        durationMs = surah.ayahsCount * 15_000L,
    )
}
