package com.ghais.ui.screens.broadcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.Broadcast
import com.ghais.data.repository.BroadcastRepository
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Broadcasts inbox: public announcements newest-first (server orders
 * `$createdAt` desc, limit 50; the list keeps that order as-is).
 *
 * Pulls on open via [BroadcastRepository.refresh] (PUBLIC_READ, works
 * signed-out). Tapping a row expands the body and marks it read (local-only
 * seen-id set). Urgency reads through monochrome emphasis only — Urgent rows
 * get the active fill + chrome pill, everything else stays ghost.
 */
object BroadcastsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val items by BroadcastRepository.broadcasts.collectAsState()
        val seen by BroadcastRepository.seenIds.collectAsState()
        var expandedId by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            BroadcastRepository.refresh()
        }

        val unread = items.count { it.id !in seen }

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.noirClickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        IconWell(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            size = 40.dp,
                            iconSize = 20.dp,
                            contentDescription = "Back"
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = "Inbox",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (unread > 0) "$unread unread" else "Announcements",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${items.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (items.isEmpty()) {
                    NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconWell(
                                icon = Icons.Filled.Campaign,
                                size = 56.dp,
                                iconSize = 26.dp,
                                contentDescription = null,
                                tint = GhaisNoir.TextTertiary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No announcements yet",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New broadcasts from Ghais will appear here.",
                                color = GhaisNoir.TextSecondary,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = items,
                            key = { it.id }
                        ) { item ->
                            val isUnread = item.id !in seen
                            BroadcastRow(
                                item = item,
                                unread = isUnread,
                                expanded = expandedId == item.id,
                                onToggle = {
                                    BroadcastRepository.markSeen(item.id)
                                    expandedId = if (expandedId == item.id) null else item.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastRow(
    item: Broadcast,
    unread: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val urgent = item.urgency.equals("urgent", ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(
                if (urgent) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft(),
                GhaisShapes.row
            )
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onToggle)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(GhaisNoir.chromeFill(), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = item.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (unread || urgent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrgencyPill(urgency = item.urgency, urgent = urgent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.audience,
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.body,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.5.sp
            )
        }
    }
}

@Composable
private fun UrgencyPill(urgency: String, urgent: Boolean) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(
                if (urgent) GhaisNoir.chromeFill() else GhaisNoir.cardFillSoft(),
                GhaisShapes.pill
            )
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = urgency,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (urgent) GhaisNoir.OnChrome else GhaisNoir.TextSecondary,
            maxLines = 1
        )
    }
}
