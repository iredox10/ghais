package com.ghais.ui.screens.reciters.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.screens.reciters.art.ReciterArtwork
import com.ghais.ui.screens.reciters.monogramForReciter
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import kotlin.time.Clock

/**
 * Agent A6 — Voice of the day.
 *
 * Pure deterministic rotation in [voiceOfDay]: the browse catalog is sorted
 * by slug (stable order), well-known voices carry [WELL_KNOWN_WEIGHT] slots
 * per day against [OTHER_WEIGHT] for everyone else, and the pick is
 * `floorMod(epochDay, totalWeight)` walked down the weights. Same catalog +
 * same epoch day always yields the same reciter; the weighting only biases
 * frequency, so obscure voices still surface on their slots.
 */

private const val MILLIS_PER_DAY = 86_400_000L

/** Weight of a well-known voice in the daily rotation. */
const val WELL_KNOWN_WEIGHT = 5

/** Weight of any other reciter in the daily rotation. */
const val OTHER_WEIGHT = 1

/**
 * Canonical well-known voice slugs (lowercase, `-` spelling). Matching is
 * fragment-based ([isWellKnownVoice]), so cross-catalog alias spellings
 * (`mishary`/`alafasy`, `shuraim`/`shuraym`, ...) all count.
 */
val WELL_KNOWN_VOICE_SLUGS: Set<String> = setOf(
    "mishary",
    "alafasy",
    "al-sudais",
    "abdul-basit",
    "al-minshawi",
    "al-husary",
    "saad-alghamdi",
    "al-muaiqly",
    "saud-shuraim",
    "al-dossari",
    "ahmed-alajamy",
    "islam-sobhi",
)

/** Distinctive slug stems of well-known voices (covers alias spellings). */
private val WELL_KNOWN_STEMS: Set<String> = setOf(
    "mishary", "alafasy",
    "sudais",
    "basit", "baset",
    "minshawi",
    "husary", "hussary",
    "ghamdi", "alghamdi",
    "muaiqly",
    "shuraim", "shuraym",
    "dossari", "dussary",
    "ajamy", "alajamy",
    "sobhi",
)

/** True when [slug] belongs to a well-known voice (alias-tolerant). */
fun isWellKnownVoice(slug: String): Boolean {
    val clean = slug.trim().lowercase().replace('_', '-')
    if (clean.isEmpty()) return false
    if (clean in WELL_KNOWN_VOICE_SLUGS) return true
    return clean.split('-').any { it in WELL_KNOWN_STEMS }
}

/**
 * Deterministic voice for [epochDay] (days since Unix epoch, UTC).
 * Null when [reciters] is empty; the single reciter when size == 1.
 * Never throws.
 */
fun voiceOfDay(reciters: List<Reciter>, epochDay: Long): Reciter? {
    if (reciters.isEmpty()) return null
    if (reciters.size == 1) return reciters.first()
    return try {
        val ordered = reciters.sortedBy { it.slug.lowercase() }
        val weights = ordered.map { if (isWellKnownVoice(it.slug)) WELL_KNOWN_WEIGHT else OTHER_WEIGHT }
        val total = weights.sum().takeIf { it > 0 } ?: return ordered.first()
        var slot = ((epochDay % total) + total) % total
        for (i in ordered.indices) {
            slot -= weights[i]
            if (slot < 0) return ordered[i]
        }
        ordered.last()
    } catch (_: Exception) {
        reciters.firstOrNull()
    }
}

/** Epoch day (UTC) for "today" — the [voiceOfDay] rebuild key. */
fun currentEpochDay(): Long =
    Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY

/**
 * Noir hero card for the voice of the day: art (cloud/local photo,
 * [ReciterArtwork] fallback), name + riwayah line, one-tap play that queues
 * the reciter's available full-surah catalog via [AudioEngine.playQueue] and
 * reports through [onOpenPlayer]. Card tap opens the profile via
 * [onOpenProfile]. Hidden play path is null-safe: nothing happens when the
 * reciter has no available surahs.
 */
@Composable
fun VoiceOfDayCard(
    reciter: Reciter,
    onOpenProfile: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cloudPhoto = rememberCloudPhoto(reciter.slug)
    val localPhoto = remember(reciter.slug) { photoForSlug(reciter.slug) }
    val artUrl = cloudPhoto ?: localPhoto

    NoirHeroCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onOpenProfile(reciter.slug) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (artUrl != null) {
                    NoirArtworkWell(
                        coverUrl = artUrl,
                        monogram = monogramForReciter(reciter.nameEn),
                        shape = CircleShape,
                        size = 76.dp,
                        monogramSize = 26.sp,
                        ring = true,
                        grayscale = false,
                        scrimAlpha = 0f
                    )
                } else {
                    ReciterArtwork(
                        slug = reciter.slug,
                        size = 76.dp,
                        shape = CircleShape,
                        monogram = monogramForReciter(reciter.nameEn)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VOICE OF THE DAY",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = reciter.nameEn,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${reciter.riwayah} • ${reciter.style}",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (reciter.nameAr.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = reciter.nameAr,
                            color = GhaisNoir.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            ChromePillButton(
                text = "Play now",
                onClick = {
                    try {
                        val surahs = QuranDataRepository.getSurahsForReciter(reciter)
                            .filter { reciter.isSurahAvailable(it.id) }
                        if (surahs.isEmpty()) return@ChromePillButton
                        val tracks = surahs.map { surah ->
                            TrackItem(
                                reciterSlug = reciter.slug,
                                reciterName = reciter.nameEn,
                                surahId = surah.id,
                                surahNameEn = surah.nameEn,
                                surahNameAr = surah.nameAr,
                                ayahNo = 0,
                                audioUrl = reciter.getFullSurahUrl(surah.id),
                                textUthmani = "",
                                durationMs = surah.ayahsCount * 15_000L
                            )
                        }
                        AudioEngine.playQueue(tracks, startIndex = 0)
                        onOpenPlayer()
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.PlayArrow
            )
        }
    }
}
