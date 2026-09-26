package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.ReciterCloudCache
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.prettifySlug
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir

/**
 * Phase 4 — "Qari you follow".
 *
 * Portraits render in natural colour inside a clay well, and the followed
 * state is expressed by a chromium specular ring rather than a coloured border.
 */
@Composable
fun HomeFollowedRow(onReciter: (String) -> Unit) {
    val followedSlugs by FollowStore.followedSlugs.collectAsState()
    // Re-resolve on catalog sync so an admin-uploaded reciter shows up here.
    val cloudCatalog by ReciterCloudCache.cloudReciters.collectAsState()
    val resolved = remember(followedSlugs, cloudCatalog) {
        // Keep an unresolvable slug rather than dropping it. mapNotNull here
        // made the whole section claim "No Qari followed yet" while
        // FollowStore still held the record. The fallback name comes from the
        // slug, so it never borrows another reciter's identity.
        followedSlugs.map { slug ->
            val qari = resolveFollowedQari(slug)
            Triple(
                qari?.reciter?.slug ?: slug,
                qari?.reciter?.nameEn ?: slug.prettifySlug(),
                qari?.photoUrl.orEmpty()
            )
        // Two followed slugs can resolve to the SAME reciter (mishary +
        // alafasy are the same person), which would both duplicate the row
        // and collide on the LazyRow key below.
        }.distinctBy { it.first }
    }

    if (resolved.isEmpty()) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No Qari followed yet",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Follow a Qari from their profile and they will appear here.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // Followed slugs can alias to the same human (mishary/alafasy);
        // `resolved` is already distinct by slug, so this key is unique.
        items(resolved, key = { it.first }) { (slug, name, photoUrl) ->
            // Cloud portrait first: the admin panel owns reciter photos, and
            // followed qaris outside the small bundled roster have no static
            // image — without this they rendered as bare monograms.
            val cloudPhoto = rememberCloudPhoto(slug)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .noirClickable { onReciter(slug) }
            ) {
                NoirArtworkWell(
                    coverUrl = cloudPhoto ?: photoUrl,
                    monogram = name.firstOrNull()?.uppercase() ?: "Q",
                    shape = CircleShape,
                    size = 84.dp,
                    monogramSize = 28.sp,
                    ring = true,
                    grayscale = false,
                    scrimAlpha = 0f
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = name,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Phase 4 — "Your stats".
 *
 * The streak / minutes / reciter cells were amber-emerald-violet. They are now
 * three monochrome cells separated by ghost hairlines, so the numbers carry the
 * hierarchy instead of the hues.
 */
@Composable
fun HomeStatsCard() {
    val stats by UserUsageRepository.stats.collectAsState()

    NoirCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirStatCell(
                icon = Icons.Filled.LocalFireDepartment,
                contentDescription = "Days streak",
                value = stats.daysStreak.toString(),
                label = if (stats.daysStreak == 1) "day streak" else "days streak",
                modifier = Modifier.weight(1f)
            )
            NoirStatSeparator()
            NoirStatCell(
                icon = Icons.Filled.Timer,
                contentDescription = "Minutes today",
                value = stats.minutesToday.toString(),
                label = "min today",
                modifier = Modifier.weight(1f)
            )
            NoirStatSeparator()
            NoirStatCell(
                icon = Icons.Filled.RecordVoiceOver,
                contentDescription = "Reciters listened",
                value = stats.uniqueRecitersCount.toString(),
                label = if (stats.uniqueRecitersCount == 1) "reciter" else "reciters",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NoirStatSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(44.dp)
            .background(GhaisNoir.BorderGhost)
    )
}

@Composable
private fun NoirStatCell(
    icon: ImageVector,
    contentDescription: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconWell(
            icon = icon,
            size = 40.dp,
            iconSize = 20.dp,
            contentDescription = contentDescription
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
