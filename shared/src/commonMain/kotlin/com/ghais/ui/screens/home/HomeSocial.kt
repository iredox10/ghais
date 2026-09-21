package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ghais.data.repository.UserUsageRepository
import com.ghais.ui.theme.QuranifyColors
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.resolveFollowedQari

private val DarkCard = Color(0xFF1C1C1E)
private val MutedGrey = Color(0xFF9A9AA0)

@Composable
fun HomeFollowedRow(onReciter: (String) -> Unit) {
    val followedSlugs by FollowStore.followedSlugs.collectAsState()
    val resolved = remember(followedSlugs) {
        followedSlugs.mapNotNull { slug ->
            resolveFollowedQari(slug)?.let { qari ->
                Triple(qari.reciter.slug, qari.reciter.nameEn, qari.photoUrl)
            }
        }
    }
    if (resolved.isEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Follow qari from their profiles — they will appear here",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MutedGrey,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(resolved) { (slug, name, photoUrl) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(104.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onReciter(slug) }
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(DarkCard, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeStatsCard() {
    val stats by UserUsageRepository.stats.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141418))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeStatCell(
                icon = Icons.Filled.LocalFireDepartment,
                contentDescription = "Days streak",
                value = stats.daysStreak.toString(),
                label = if (stats.daysStreak == 1) "day streak" else "days streak",
                iconTint = Color(0xFFF59E0B)
            )
            HomeStatCell(
                icon = Icons.Filled.Timer,
                contentDescription = "Minutes today",
                value = stats.minutesToday.toString(),
                label = "min today",
                iconTint = QuranifyColors.Primary
            )
            HomeStatCell(
                icon = Icons.Filled.RecordVoiceOver,
                contentDescription = "Reciters listened",
                value = stats.uniqueRecitersCount.toString(),
                label = if (stats.uniqueRecitersCount == 1) "reciter" else "reciters",
                iconTint = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun HomeStatCell(
    icon: ImageVector,
    contentDescription: String,
    value: String,
    label: String,
    iconTint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MutedGrey,
            textAlign = TextAlign.Center
        )
    }
}
