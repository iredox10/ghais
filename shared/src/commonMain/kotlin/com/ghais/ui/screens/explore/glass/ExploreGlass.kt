package com.ghais.ui.screens.explore.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProtonBlack = Color(0xFF000000)
private val ProtonWhite = Color(0xFFFFFFFF)
private val ProtonMuted = Color(0xFF8E8E93)
private val ProtonChromeLight = Color(0xFFF5F5F7)
private val ProtonChromeDark = Color(0xFFC7C7CC)
private val ProtonInk = Color(0xFF0B0C0E)

@Composable
fun ProtonExploreHeader(titleWhite: String, titleGrey: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = titleWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ProtonWhite,
            lineHeight = 36.sp
        )
        Text(
            text = titleGrey,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ProtonMuted,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = ProtonMuted
        )
    }
}

@Composable
fun ProtonExploreSearch(query: String, onQuery: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(ProtonWhite.copy(alpha = 0.08f))
            .border(1.dp, ProtonWhite.copy(alpha = 0.10f), RoundedCornerShape(50.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = ProtonMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(
                    color = ProtonWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isEmpty()) {
                Text(
                    text = "Search collections, surahs & moods...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = ProtonMuted
                )
            }
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQuery("") }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = ProtonMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProtonCategoryRow(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(categories) { category ->
            val isSelected = category == selected
            Box(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(ProtonChromeLight, ProtonChromeDark)
                                )
                            )
                        } else {
                            Modifier
                                .background(ProtonWhite.copy(alpha = 0.05f))
                                .border(
                                    1.dp,
                                    ProtonWhite.copy(alpha = 0.08f),
                                    RoundedCornerShape(50.dp)
                                )
                        }
                    )
                    .clickable { onSelect(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) ProtonInk else ProtonMuted
                )
            }
        }
    }
}

@Composable
fun ProtonFeaturedBanner(title: String, subtitle: String, buttonText: String, onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ProtonWhite.copy(alpha = 0.09f),
                        ProtonWhite.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, ProtonWhite.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val gridColor = Color.White.copy(alpha = 0.04f)
            val vStep = size.width / 4f
            for (i in 1..3) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(vStep * i, 0f),
                    end = androidx.compose.ui.geometry.Offset(vStep * i, size.height)
                )
            }
            val hStep = size.height / 5f
            for (i in 1..4) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, hStep * i),
                    end = androidx.compose.ui.geometry.Offset(size.width, hStep * i)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProtonWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = ProtonMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ProtonChromeLight, ProtonChromeDark)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProtonInk
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ProtonWhite.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = ProtonWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ProtonEmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(ProtonWhite.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = ProtonMuted,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProtonWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = ProtonMuted,
            textAlign = TextAlign.Center
        )
    }
}
