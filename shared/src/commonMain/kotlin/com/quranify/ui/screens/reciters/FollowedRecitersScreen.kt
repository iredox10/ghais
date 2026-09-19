package com.quranify.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.quranify.data.repository.FollowStore
import com.quranify.data.repository.QuranDataRepository

private val PureBlack = Color(0xFF000000)
private val MutedGrey = Color(0xFF9A9AA0)
private val LinkBlue = Color(0xFF4C8DFF)
private val DarkCard = Color(0xFF1C1C1E)

object FollowedRecitersScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        val followedSlugs by FollowStore.followedSlugs.collectAsState()

        val reciters = remember(followedSlugs) {
            followedSlugs
                .map { slug -> QuranDataRepository.getReciterBySlug(slug) }
                .distinctBy { it.slug }
        }
        val filtered = remember(reciters, searchQuery) {
            if (searchQuery.isBlank()) reciters
            else reciters.filter {
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                    it.nameAr.contains(searchQuery, ignoreCase = true)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LinkBlue.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                            text = "Following",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filtered.size} following",
                            color = MutedGrey,
                            fontSize = 12.sp
                        )
                    }
                }

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
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MutedGrey,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search following...",
                                    color = MutedGrey,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You're not following anyone yet — discover qari in Reciters",
                            color = MutedGrey,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 112.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { it.slug }
                        ) { reciter ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .clickable { navigator.push(ReciterProfileScreen(reciter.slug)) }
                                    .padding(12.dp)
                            ) {
                                val photo = photoForSlug(reciter.slug)
                                if (photo != null) {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = reciter.nameEn,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(DarkCard)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color.White.copy(alpha = 0.10f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reciter.nameEn.take(1),
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reciter.nameEn,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${reciter.riwayah} • ${reciter.style}",
                                        color = MutedGrey,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = reciter.nameAr,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Unfollow affordance
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                        .clickable { FollowStore.toggle(reciter.slug) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonRemove,
                                        contentDescription = "Unfollow",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open profile",
                                    tint = MutedGrey,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
