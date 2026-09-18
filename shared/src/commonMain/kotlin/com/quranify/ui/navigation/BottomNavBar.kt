package com.quranify.ui.navigation

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
import com.quranify.ui.theme.QuranifyColors
import com.quranify.ui.theme.QuranifyShapes

// Stitch Mockup Design Tokens
private val ObsidianGlassBg = Color(0xFF111415).copy(alpha = 0.85f)
private val GlassBorderColor = Color(0xFF4EDEA3).copy(alpha = 0.15f)
private val TopHighlightColor = Color(0xFF4EDEA3).copy(alpha = 0.25f)
private val ActiveEmerald = Color(0xFF4EDEA3)
private val ActivePillBackground = Color(0xFF4EDEA3).copy(alpha = 0.12f)
private val InactiveGrey = Color(0xFF8E989C)

/**
 * Floating frosted glassmorphic bottom navigation dock matching Stitch mockups:
 * - Obsidian backdrop (#111415 at 85% opacity) with rounded pill silhouette
 * - Subtle emerald top border/specular highlight (#4EDEA3 at 15-25% opacity)
 * - Active tab with glowing emerald (#4EDEA3) icon, label, pill capsule & glowing underglow dot
 * - Inactive tabs in muted grey (#8E989C)
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
                .height(64.dp),
            shape = QuranifyShapes.navDock,
            color = ObsidianGlassBg,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TopHighlightColor, // Specular subtle top highlight
                        GlassBorderColor   // Soft translucent emerald border
                    )
                )
            ),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
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
                        targetValue = if (isSelected) ActiveEmerald else InactiveGrey,
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
                                        color = GlassBorderColor.copy(alpha = 0.35f * pillAlpha),
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

                            // Glowing emerald underglow dot indicator
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
                                            color = ActiveEmerald.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        )
                                )
                                // Core bright dot
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .background(
                                            color = ActiveEmerald,
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
