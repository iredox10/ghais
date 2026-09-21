package com.ghais.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.ghais.ui.theme.QuranifyColors
import com.ghais.ui.theme.QuranifyShapes

// Floating Dock Tokens: transparent black glass + white (reference design)
private val GlassTop = Color(0x8C141416) // ~55% black — content ghosts through
private val GlassBottom = Color(0x59101012) // ~35% black — lighter at the base
private val GlassSheen = Color.White.copy(alpha = 0.06f)
private val ActiveWhite = Color(0xFFFFFFFF)
private val ActivePillBackground = Color(0xFFFFFFFF).copy(alpha = 0.12f)
private val ActivePillBorder = Color(0xFFFFFFFF).copy(alpha = 0.20f)
private val InactiveGrey = Color(0xFF8E8E93)

/**
 * Transparent frosted-glass bottom navigation dock (reference design).
 * Floats over the scrolling content (see MainScreen overlay):
 * - Translucent black vertical gradient so content ghosts through
 * - Diagonal white sheen + bright top specular hairline for the glass read
 * - Selected tab: white icon + label on a soft white pill, white dot indicator
 * - Inactive tabs: iOS-style system grey
 */
@Composable
fun QuranifyBottomNavBar(modifier: Modifier = Modifier) {
    val tabNavigator = LocalTabNavigator.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(66.dp),
            shape = QuranifyShapes.navDock,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x4DFFFFFF),
                        Color(0x1FFFFFFF)
                    )
                )
            ),
            shadowElevation = 16.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(GlassTop, GlassBottom)),
                        QuranifyShapes.navDock
                    )
                    .clip(QuranifyShapes.navDock)
            ) {
                // Diagonal glass sheen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GlassSheen,
                                    Color.Transparent,
                                    Color.Transparent,
                                    GlassSheen.copy(alpha = 0.03f)
                                )
                            )
                        )
                )
                // Bright top specular hairline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 22.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                        .align(Alignment.TopCenter)
                )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTab.entries.forEach { appTab ->
                    val currentTab = runCatching { tabNavigator.current }.getOrNull()
                    val isSelected = currentTab == appTab.tab

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) ActiveWhite else InactiveGrey,
                        animationSpec = tween(durationMillis = 200),
                        label = "tabContentColor"
                    )

                    val pillAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "tabPillAlpha"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                color = ActivePillBackground.copy(alpha = ActivePillBackground.alpha * pillAlpha),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 0.5.dp,
                                        color = ActivePillBorder.copy(alpha = pillAlpha),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                tabNavigator.current = appTab.tab
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = appTab.icon,
                                contentDescription = appTab.title,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = appTab.title,
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Glowing white underglow dot indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(pillAlpha),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer diffuse glow aura
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = ActiveWhite.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        )
                                )
                                // Core bright dot
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .background(
                                            color = ActiveWhite,
                                            shape = CircleShape
                                        )
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
