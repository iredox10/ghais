package com.ghais.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

object KhatmaScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val progress = 0.35f

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = { navigator.pop() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(GhaisNoir.Fill2, CircleShape)
                                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "SPIRITUAL PLAN",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = "Khatma",
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconWell(
                                            icon = Icons.Default.TrackChanges,
                                            size = 64.dp,
                                            iconSize = 28.dp,
                                            contentDescription = "Khatma progress"
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "PROGRESS",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "${(progress * 100).toInt()}% completed",
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "12-day streak • 18 days remaining",
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        NoirStatChip(text = "12-day streak")
                                        NoirStatChip(text = "18 days left")
                                        NoirStatChip(text = "Est. Oct 15")
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    NoirSegmentedProgress(progress = progress, trackHeight = 8.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "35 of 114 portions",
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}% kept",
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            NoirSectionHeader(
                                label = "Today's portion",
                                actionLabel = "Ayahs 1–141",
                                onAction = {}
                            )

                            NoirCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "TODAY'S TARGET",
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Surah Al-Baqarah",
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Ayahs 1–141",
                                        color = GhaisNoir.TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ChromePillButton(
                                        text = "Play Today's Portion",
                                        onClick = {},
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = Icons.Default.PlayArrow
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            NoirSectionHeader(
                                label = "Journey",
                                actionLabel = "3 stats",
                                onAction = {}
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatBox("Streak", "12 Days", Icons.Filled.LocalFireDepartment, GhaisNoir.TextPrimary)
                                StatBox("Remaining", "18 Days", null, GhaisNoir.TextPrimary)
                                StatBox("Est. Finish", "Oct 15", null, GhaisNoir.TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, icon: ImageVector?, iconTint: Color) {
    // iconTint is accepted for backward compatibility; Noir renders all
    // glyphs in the monochrome text ramp (zero hue).
    val tint = when (iconTint) {
        Color.Unspecified -> GhaisNoir.TextSecondary
        else -> GhaisNoir.TextPrimary
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            IconWell(icon = icon, size = 40.dp, iconSize = 20.dp, tint = tint)
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp
        )
    }
}
