package com.quranify.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.quranify.data.seed.Reciter
import com.quranify.data.seed.StitchAssets
import com.quranify.ui.screens.reciters.ReciterProfileScreen
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.theme.QuranifyColors

@Composable
fun VerifiedRecitersSection(
    reciters: List<Reciter> = StitchAssets.VerifiedReciters,
    onSeeAllClick: () -> Unit = {}
) {
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Verified Reciters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Masters of Tartil and Tajweed",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
            Text(
                text = "See All",
                fontSize = 13.sp,
                color = Color(0xFFA855F7),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // LazyRow of Reciter Avatar Items
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(reciters) { index, reciter ->
                ReciterAvatarItem(
                    reciter = reciter,
                    isFeatured = index == 0,
                    onClick = {
                        rootNavigator?.push(ReciterProfileScreen(reciter.slug))
                    }
                )
            }
        }
    }
}

@Composable
fun ReciterAvatarItem(
    reciter: Reciter,
    isFeatured: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            // Circular photo avatar with either radiant gradient or glass ring
            val borderModifier = if (isFeatured) {
                Modifier.border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7),
                            Color(0xFFC084FC),
                            Color.White.copy(0.3f)
                        )
                    ),
                    shape = CircleShape
                )
            } else {
                Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
            }

            AsyncImage(
                model = reciter.photoUrl,
                contentDescription = reciter.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(borderModifier)
                    .background(QuranifyColors.Surface, CircleShape)
            )

            // Trending Purple verified checkmark badge
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFA855F7))
                    .border(1.5.dp, QuranifyColors.Background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reciter name
        Text(
            text = reciter.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Fan count
        Text(
            text = reciter.fans,
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
