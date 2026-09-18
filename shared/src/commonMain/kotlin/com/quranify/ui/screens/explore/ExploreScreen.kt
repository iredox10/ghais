package com.quranify.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.domain.model.RoutineModeType
import com.quranify.ui.screens.mood.RoutineModeScreen
import com.quranify.ui.screens.reciters.RecitersScreen
import com.quranify.ui.screens.search.SearchScreen
import com.quranify.ui.theme.QuranifyColors

object ExploreScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Explore",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
                .padding(24.dp)
        ) {
            SearchBar()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Browse",
                color = QuranifyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            BrowseGrid()
        }
    }
}

@Composable
private fun SearchBar() {
    val navigator = LocalNavigator.currentOrThrow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { navigator.parent?.push(SearchScreen()) ?: navigator.push(SearchScreen()) }
    ) {
        TextField(
            value = "",
            onValueChange = {},
            enabled = false,
            placeholder = { Text("Search Surahs, Reciters...", color = QuranifyColors.TextSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = QuranifyColors.TextSecondary) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = QuranifyColors.Card,
                unfocusedContainerColor = QuranifyColors.Card,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledContainerColor = QuranifyColors.Card,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true
        )
    }
}

private data class Category(val title: String, val icon: ImageVector, val screen: Screen)

@Composable
private fun BrowseGrid() {
    val categories = listOf(
        Category("All Surahs", Icons.Filled.List, SurahsScreen),
        Category("All Reciters", Icons.Filled.Person, RecitersScreen()),
        Category("Moods & Mixes", Icons.Filled.Audiotrack, RoutineModeScreen(RoutineModeType.STUDY)),
        Category("Juz Browser", Icons.Filled.MenuBook, JuzBrowserScreen)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories.size) { index ->
            CategoryCard(category = categories[index])
        }
    }
}

@Composable
private fun CategoryCard(category: Category) {
    val navigator = LocalNavigator.currentOrThrow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(QuranifyColors.Card, RoundedCornerShape(16.dp))
            .clickable { navigator.parent?.push(category.screen) ?: navigator.push(category.screen) }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = QuranifyColors.Primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = category.title,
                color = QuranifyColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
