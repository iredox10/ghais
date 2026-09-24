package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

object FollowedRecitersScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        val followedSlugs by FollowStore.followedSlugs.collectAsState()

        val reciters = remember(followedSlugs) {
            followedSlugs.mapNotNull { slug -> resolveFollowedQari(slug) }
        }
        val filtered = remember(reciters, searchQuery) {
            if (searchQuery.isBlank()) reciters
            else reciters.filter {
                it.reciter.nameEn.contains(searchQuery, ignoreCase = true) ||
                    it.reciter.nameAr.contains(searchQuery, ignoreCase = true)
            }
        }

        NoirScreenRoot {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconWell(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        size = 42.dp,
                        iconSize = 22.dp,
                        contentDescription = "Back",
                        modifier = Modifier.noirClickable { navigator.pop() }
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Following",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filtered.size} following",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                NoirInsetField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = GhaisNoir.TextPrimary,
                                fontSize = 14.sp
                            ),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search following...",
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NoirCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconWell(
                                    icon = Icons.Filled.RecordVoiceOver,
                                    size = 52.dp,
                                    iconSize = 24.dp,
                                    contentDescription = null,
                                    tint = GhaisNoir.TextSecondary
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "No Qari followed yet",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Follow a Qari from their profile and they will appear here.",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                GhostPillButton(
                                    text = "Discover reciters",
                                    onClick = { navigator.pop() }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filtered,
                            key = { it.storedSlug }
                        ) { qari ->
                            val reciter = qari.reciter
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(GhaisShapes.row)
                                    .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
                                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
                                    .topSpecular(inset = 22.dp)
                                    .noirClickable { navigator.push(ReciterProfileScreen(reciter.slug)) }
                                    .padding(12.dp)
                            ) {
                                val photo = rememberCloudPhoto(reciter.slug)
                                    ?: qari.photoUrl
                                    ?: photoForSlug(reciter.slug)
                                if (photo != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(GhaisNoir.wellFill())
                                            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = photo,
                                            contentDescription = reciter.nameEn,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0f)
                                                )
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(GhaisNoir.wellFill())
                                            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reciter.nameEn.take(1).uppercase(),
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reciter.nameEn,
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${reciter.riwayah} • ${reciter.style}",
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = reciter.nameAr,
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Unfollow affordance — ghost well, monochrome glyph
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(GhaisNoir.wellFill(), GhaisShapes.well)
                                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
                                        .noirClickable { FollowStore.toggle(qari.storedSlug) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonRemove,
                                        contentDescription = "Unfollow",
                                        tint = GhaisNoir.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.well),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open profile",
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
