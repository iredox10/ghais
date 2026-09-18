package com.quranify.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.theme.QuranifyColors

object LibraryScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Library",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        Navigator(LibraryMainScreen)
    }
}

object LibraryMainScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
        ) {
            Text(
                text = "Your Library",
                color = QuranifyColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { 
                    LibraryRow(
                        icon = Icons.Filled.PlaylistPlay, 
                        title = "Playlists", 
                        subtitle = "0 playlists",
                        onClick = { navigator.push(PlaylistDetailScreen("p1")) }
                    ) 
                }
                item { 
                    LibraryRow(
                        icon = Icons.Filled.Favorite, 
                        title = "Favorites", 
                        subtitle = "12 tracks",
                        onClick = { navigator.push(FavoritesScreen) }
                    ) 
                }
                item { 
                    LibraryRow(
                        icon = Icons.Filled.Download, 
                        title = "Downloads", 
                        subtitle = "3 surahs",
                        onClick = { }
                    ) 
                }
                item { 
                    LibraryRow(
                        icon = Icons.Filled.History, 
                        title = "History", 
                        subtitle = "Recently played",
                        onClick = { }
                    ) 
                }
                item { 
                    LibraryRow(
                        icon = Icons.Filled.MenuBook, 
                        title = "Khatma Plans", 
                        subtitle = "No active plans",
                        onClick = { navigator.push(KhatmaScreen) }
                    ) 
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QuranifyColors.Card)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = QuranifyColors.Primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = QuranifyColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = QuranifyColors.TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = QuranifyColors.TextSecondary
        )
    }
}
