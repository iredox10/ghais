package com.ghais.ui.screens.reciters.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassBlack = Color(0xFF000000)
private val GlassWhite = Color(0xFFFFFFFF)
private val GlassMuted = Color(0xFF9A9AA0)
private val GlassChromeLight = Color(0xFFF5F5F7)
private val GlassChromeGrey = Color(0xFFC7C7CC)
private val GlassInk = Color(0xFF0B0C0E)

@Composable
fun ProtonReciterHeader(
    titleWhite: String,
    titleGrey: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = titleWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GlassWhite
        )
        Text(
            text = titleGrey,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GlassMuted
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = GlassMuted
        )
    }
}

@Composable
fun ProtonReciterSearch(
    query: String,
    onQuery: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .height(50.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(GlassWhite.copy(alpha = 0.08f))
            .border(1.dp, GlassWhite.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = GlassMuted
        )
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(
                color = GlassWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search reciters...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassMuted
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun FollowBanner(
    followedCount: Int,
    onOpen: () -> Unit
) {
    val title = if (followedCount > 0) "Following $followedCount" else "Follow reciters"
    val sub = if (followedCount > 0) "Your voices, one tap away" else "Build your list of favorite voices"
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassWhite.copy(alpha = 0.09f),
                        GlassWhite.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, GlassWhite.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GlassWhite
            )
            Text(
                text = sub,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = GlassMuted
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GlassWhite.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = GlassWhite
            )
        }
    }
}

@Composable
fun ReelSectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GlassWhite,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "See All",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = GlassMuted,
            modifier = Modifier.clickable(onClick = onSeeAll)
        )
    }
}
