package com.quranify.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.components.QuranifyCard
import com.quranify.ui.components.SectionHeader
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
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                GreetingSection()
            }
            item {
                RoutineModesSection()
            }
            item {
                FeaturedRecitersSection()
            }
            item {
                RecentlyPlayedSection()
            }
        }
    }
}

@Composable
private fun GreetingSection() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Assalamu Alaikum",
            color = QuranifyColors.Primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Good morning", // Dynamic time-based in the future
            color = QuranifyColors.TextSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun RoutineModesSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Routine Modes",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MoodCard(
                    title = "Study",
                    subtitle = "Focus & Learn",
                    icon = Icons.Filled.School,
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                )
            }
            item {
                MoodCard(
                    title = "Work",
                    subtitle = "Productivity",
                    icon = Icons.Filled.Work,
                    colors = listOf(Color(0xFF064E3B), Color(0xFF10B981))
                )
            }
            item {
                MoodCard(
                    title = "Sleep",
                    subtitle = "Relax & Rest",
                    icon = Icons.Filled.Nightlight,
                    colors = listOf(Color(0xFF4C1D95), Color(0xFF8B5CF6))
                )
            }
        }
    }
}

@Composable
private fun MoodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeaturedRecitersSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        SectionHeader(
            title = "Featured Reciters",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val reciters = listOf("Mishary Alafasy", "AbdulBaset", "Al-Sudais", "Al-Shuraim", "Al-Minshawi")
            items(reciters.size) { index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.Card)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reciters[index].split(" ").first(),
                        color = QuranifyColors.TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)) {
        SectionHeader(
            title = "Recently Played",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(100.dp)
                .background(QuranifyColors.Card, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recent activity yet.",
                color = QuranifyColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
