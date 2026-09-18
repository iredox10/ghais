package com.quranify.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.quranify.data.seed.StitchAssets
import com.quranify.ui.screens.home.components.AyahOfTheDayCard
import com.quranify.ui.screens.home.components.CuratedForPeaceSection
import com.quranify.ui.screens.home.components.DhikrStreakCard
import com.quranify.ui.screens.home.components.JumpBackInSection
import com.quranify.ui.screens.home.components.RecitationByMoodSection
import com.quranify.ui.screens.home.components.VerifiedRecitersSection
import com.quranify.ui.theme.QuranifyColors

object HomeScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 0u,
                title = "Home",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(QuranifyColors.Background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                HomeTopBar()
            }
            item {
                DhikrStreakCard()
            }
            item {
                AyahOfTheDayCard()
            }
            item {
                JumpBackInSection()
            }
            item {
                VerifiedRecitersSection()
            }
            item {
                CuratedForPeaceSection()
            }
            item {
                RecitationByMoodSection()
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Assalamu Alaikum",
                color = QuranifyColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Home",
                color = QuranifyColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* TODO: Search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = QuranifyColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = StitchAssets.ProfileAvatarUrl,
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.SurfaceHigh)
            )
        }
    }
}
