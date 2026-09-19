package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.quranify.data.seed.QuranData
import com.quranify.data.seed.StitchAssets
import com.quranify.domain.model.Reciter

private val PureBlack = Color(0xFF000000)
private val DarkCard = Color(0xFF1C1C1E)
private val MutedGrey = Color(0xFF9A9AA0)
private val LinkBlue = Color(0xFF4C8DFF)

// Preferred display order for nation groups
private val NationOrder = listOf("Saudi Arabia", "Egypt", "Kuwait")

class RecitersScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Reciters",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }

        val groups = remember(searchQuery) {
            QuranData.RECITERS
                .filter {
                    searchQuery.isBlank() ||
                        it.nameEn.contains(searchQuery, ignoreCase = true) ||
                        it.nameAr.contains(searchQuery, ignoreCase = true) ||
                        it.country.contains(searchQuery, ignoreCase = true)
                }
                .groupBy { it.country.ifBlank { "Other" } }
                .toSortedMap(compareBy { NationOrder.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            // Header: back + title + count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { navigator.pop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Reciters",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${QuranData.RECITERS.size} reciters • ${groups.size} nations",
                        color = MutedGrey,
                        fontSize = 12.sp
                    )
                }
            }

            // Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MutedGrey,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(text = "Search reciters or nations...", color = MutedGrey, fontSize = 14.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                groups.forEach { (nation, reciters) ->
                    item(key = "header_$nation") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = nation,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${reciters.size}",
                                    color = MutedGrey,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    items(
                        count = reciters.size,
                        key = { i -> "reciter_${nation}_${reciters[i].slug}" }
                    ) { i ->
                        NationReciterRow(
                            reciter = reciters[i],
                            onClick = { navigator.push(ReciterProfileScreen(reciters[i].slug)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NationReciterRow(reciter: Reciter, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val photo = photoForSlug(reciter.slug)
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = reciter.nameEn,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reciter.nameEn.take(1),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reciter.nameEn,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${reciter.riwayah} • ${reciter.style} • ${reciter.tempo}",
                color = MutedGrey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = reciter.nameAr,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open surahs",
            tint = LinkBlue,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun photoForSlug(slug: String): String? {
    val photos = StitchAssets.VerifiedReciters
    return when (slug) {
        "alafasy" -> photos.firstOrNull { it.slug == "mishary" }?.photoUrl
        "sudais" -> photos.firstOrNull { it.slug == "al-sudais" }?.photoUrl
        "muaiqly" -> photos.firstOrNull { it.slug == "al-muaiqly" }?.photoUrl
        "dossari" -> photos.firstOrNull { it.slug == "al-dossari" }?.photoUrl
        "abdulbaset_murattal", "abdulbaset_mujawwad" ->
            photos.firstOrNull { it.slug == "abdul-basit" }?.photoUrl
        else -> null
    }
}
