package com.quranify.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.quranify.data.seed.Reciter
import com.quranify.data.seed.StitchAssets
import com.quranify.ui.screens.reciters.ReciterProfileScreen
import com.quranify.ui.theme.QuranifyColors

@Composable
fun VerifiedRecitersSection(
    reciters: List<Reciter> = StitchAssets.VerifiedReciters,
    onSeeAllClick: () -> Unit = {}
) {
    val navigator = LocalNavigator.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
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
                    color = QuranifyColors.TextPrimary
                )
                Text(
                    text = "Masters of Tartil and Tajweed",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary
                )
            }
            Text(
                text = "See All",
                fontSize = 13.sp,
                color = QuranifyColors.Primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyRow of Reciter Avatar Items
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(reciters) { reciter ->
                ReciterAvatarItem(
                    reciter = reciter,
                    onClick = {
                        navigator?.push(ReciterProfileScreen(reciter.slug))
                    }
                )
            }
        }
    }
}

@Composable
fun ReciterAvatarItem(
    reciter: Reciter,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            // Circular photo avatar
            AsyncImage(
                model = reciter.photoUrl,
                contentDescription = reciter.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, QuranifyColors.Primary.copy(alpha = 0.3f), CircleShape)
                    .background(QuranifyColors.Surface, CircleShape)
            )

            // Verified checkmark badge
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(QuranifyColors.Secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reciter name
        Text(
            text = reciter.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = QuranifyColors.TextPrimary
        )

        // Fan count
        Text(
            text = reciter.fans,
            fontSize = 11.sp,
            color = QuranifyColors.TextSecondary
        )
    }
}
