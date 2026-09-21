package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.theme.GhaisColors

/**
 * Data representation for verified reciters shown in [AllRecitersScreen].
 */
data class VerifiedReciter(
    val slug: String,
    val nameEn: String,
    val nameAr: String,
    val country: String,
    val style: String,
    val followers: String,
    val photoUrl: String
)

private const val AIDA_PUBLIC_PREFIX = "https://lh3.googleusercontent.com/aida-public/"

val ALL_VERIFIED_RECITERS = listOf(
    VerifiedReciter(
        slug = "mishary",
        nameEn = "Mishary Rashid Alafasy",
        nameAr = "مشاري راشد العفاسي",
        country = "Kuwait",
        style = "Murattal",
        followers = "4.8M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCVq7pqUhr_vS9hx1DOLeIcQ7oOf-d00g_cojjDLspmDlxOaQGBKbrZ372xlC7XtuhQTBfUHrTej_QIKrC1_RJzUrRLPQwbSae0rPEJ-PnW2FepBr_rO0F86S7MaqQrd8uELPi6_ip_6xuedtBym7Ag9yIZW_znPwtV706_u7vdSszFO0Go4EX6Fqw1-04de24SMZZW-f5PPH77Yk8BDtb2T2s2Fz5UKnd-ivOJLI1U66mNeJYMjGSgKQ"
    ),
    VerifiedReciter(
        slug = "abdul-basit",
        nameEn = "AbdulBaset AbdulSamad",
        nameAr = "عبد الباسط عبد الصمد",
        country = "Egypt",
        style = "Mujawwad",
        followers = "5.1M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCDC0-4ROINJxYGSxbAWcYctH_0THHCxU5gubgORkVyhQMyl9AvpNpePXHQJdS67zKw8eMaouJ-zk1C5sAyIDHw4NqZxAP6m5LDQVhG2HBSP9sX2UjwEqyuvH-RACVPK_MlMFly-G9PwG0l8DyZPoP-iht8qz4j0_OxBwXdCpefdRao2p1fhn4_Rjwa8jkWOlDMtNhOBHVpCb-RHhRTDhvMJWsg81tFxI5rc8CMpg2ZyKYJLvJfvv7ZAQ"
    ),
    VerifiedReciter(
        slug = "al-sudais",
        nameEn = "Abdur-Rahman As-Sudais",
        nameAr = "عبد الرحمن السديس",
        country = "Saudi Arabia",
        style = "Taraweeh",
        followers = "3.9M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuBUUXSSQnjbh9PcmTg5THpn3mvDpVVxiH_GKcHhZGP-taghc9BmoJoNWE0ft_q3y_Oeka9G4qJLbGRkde06mO-L_0Jv9oBcMgSlZe8g65QBl6b7lOIJnL6D5CybQ8PVppePQ8RipIaS7EpYks3LNyldifo5N6m6aXBJiDrAcUUJaZYNjuBZ4kax_YHflvxYG-C3CRtpzbSiSwsZ4W5mxnhkFPZFAFDQovfCGAWpPVUlyzOOpY1xBfc22Q"
    ),
    VerifiedReciter(
        slug = "al-muaiqly",
        nameEn = "Maher Al-Muaiqly",
        nameAr = "ماهر المعيقلي",
        country = "Saudi Arabia",
        style = "Murattal",
        followers = "3.2M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCe9KI2uHdihkC6MNA1pT-CAxgMZw9htwZaNdc9ZwyJcugtxNLhv2mjYhKAFgFh7VPapZ-UR3W9WT4nzRJRbmv4R7iEnluD_2_taI1U8TBOySJNo8M3P4Dr2Iz8QSsVy-5jwmzKmbkPiWfVOzWVZEPkL3ELv5JYOZlMKy6vLl3AThQbeuMk7Vt-b0WdUU-mJLdC-32gRmBq-a3sgm_hQGdLrwCnAMYkIWyGnBwSlodGMJgVTdMAEgkSfw"
    ),
    VerifiedReciter(
        slug = "minshawi",
        nameEn = "Mohamed Siddiq Al-Minshawi",
        nameAr = "محمد صديق المنشاوي",
        country = "Egypt",
        style = "Mujawwad",
        followers = "4.6M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCVq7pqUhr_vS9hx1DOLeIcQ7oOf-d00g_cojjDLspmDlxOaQGBKbrZ372xlC7XtuhQTBfUHrTej_QIKrC1_RJzUrRLPQwbSae0rPEJ-PnW2FepBr_rO0F86S7MaqQrd8uELPi6_ip_6xuedtBym7Ag9yIZW_znPwtV706_u7vdSszFO0Go4EX6Fqw1-04de24SMZZW-f5PPH77Yk8BDtb2T2s2Fz5UKnd-ivOJLI1U66mNeJYMjGSgKQ"
    ),
    VerifiedReciter(
        slug = "husary",
        nameEn = "Mahmoud Khalil Al-Husary",
        nameAr = "محمود خليل الحصري",
        country = "Egypt",
        style = "Murattal",
        followers = "4.2M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuA7ppBI-mBpDcMXZBQBLHr3cDTqVc0AUVJ1NJnwqguMxpA0hBzodlv2AaPxNbG-l-ebm1QYLeeBXgFrS-8mrWVkcKdVlIKvO0bCKSt073yPcQR76sTbT3wxoHkKtDy0c0ME9Ou7WCDSNKjcv6FDci6c_JCN8BMnzHc36UNKKfQYdfgK2HDpTNtdT1rSvv22AyjRX-Cm_lOXiOU7Af_MrE1Gp2ehp4QMy_ErC1iY2L1SB2BFyDYYbLLYbw"
    ),
    VerifiedReciter(
        slug = "al-dossari",
        nameEn = "Yasser Al-Dossari",
        nameAr = "ياسر الدوسري",
        country = "Saudi Arabia",
        style = "Taraweeh",
        followers = "2.7M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuACIOW-S_FSJrlW0ZRGDNhL-x4HzJEmJIwIzuMKDReZE6rWbmX-muvOypYAECz9PA1sMWdEwIKEkjCtcMuCGbOusGgV0w59lNHUgrWz1ZaKQkgeTj4s6CSnVAdEp7TKuaCDrm_cmvqfK5vmKO6djuBmBdgkC5HOFFoB6Tzu_foAFiAmVi-SWqEOZK5NmjsEGM4YJI7YP8wAVvrwB958PTOCl3Uq3tTQYro_H30leRqVQ4W7NfRbZDGBLQ"
    ),
    VerifiedReciter(
        slug = "shuraim",
        nameEn = "Saud Al-Shuraim",
        nameAr = "سعود الشريم",
        country = "Saudi Arabia",
        style = "Taraweeh",
        followers = "3.4M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuBUUXSSQnjbh9PcmTg5THpn3mvDpVVxiH_GKcHhZGP-taghc9BmoJoNWE0ft_q3y_Oeka9G4qJLbGRkde06mO-L_0Jv9oBcMgSlZe8g65QBl6b7lOIJnL6D5CybQ8PVppePQ8RipIaS7EpYks3LNyldifo5N6m6aXBJiDrAcUUJaZYNjuBZ4kax_YHflvxYG-C3CRtpzbSiSwsZ4W5mxnhkFPZFAFDQovfCGAWpPVUlyzOOpY1xBfc22Q"
    ),
    VerifiedReciter(
        slug = "islam-sobhi",
        nameEn = "Islam Sobhi",
        nameAr = "إسلام صبحي",
        country = "Egypt",
        style = "Murattal",
        followers = "2.1M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuCzOyQUcWQrLRalTR2UVt5Z61FZm1cBuurJEVrVvWaTHkjM_wmWbYuMiVPbFQQdy4nSbVo5uZRGlb1d4Tt0g5e5fTo2Ou2ZJextr0N1pLm9wyVpRTSD8kp35B-W9UQZIt985fydj0nseJuSmjOmpAkRgnbfVQQohu43Z1CW3EAn7amGbcZD7tlweCQ-JBaZhBJDvCmWu9rKzcD-qIWhGvGxKomXe4KBSWNdQso65BWAnXNujB3s3BaPiw"
    ),
    VerifiedReciter(
        slug = "omar-hisham",
        nameEn = "Omar Hisham",
        nameAr = "عمر هشام العربي",
        country = "Egypt",
        style = "Murattal",
        followers = "1.5M fans",
        photoUrl = "${AIDA_PUBLIC_PREFIX}AB6AXuDgsbtNwrTvZMDxhet9KzTJPINfQqnQ2Gc74H1NotuIf_y1ZYbY3ChOWF2Rvly3n97R741_TRWgpd9CXEZUIhOCQJMCUWrHGCDLra3ozOJUdRllZlWJVjFumo6IU0Nn21IafKwYHzi0oGVNgAiqQ1feu1y0UAX2l0_E-k108XS4KFJbXV7nSETDDmeqfDUHp5Kp-KE3necEvLlvnSTmqvOEUjLwtC3P4ta3bURWLF70RzPfdAx2C1nDGw"
    )
)

class AllRecitersScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        val filterOptions = remember {
            listOf("All", "Murattal", "Mujawwad", "Taraweeh", "Egypt", "Saudi Arabia")
        }

        val filteredReciters = remember(searchQuery, selectedFilter) {
            ALL_VERIFIED_RECITERS.filter { reciter ->
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim()
                    reciter.nameEn.contains(q, ignoreCase = true) ||
                    reciter.nameAr.contains(q, ignoreCase = true) ||
                    reciter.country.contains(q, ignoreCase = true) ||
                    reciter.style.contains(q, ignoreCase = true)
                }

                val matchesFilter = when (selectedFilter) {
                    "All" -> true
                    "Murattal" -> reciter.style.equals("Murattal", ignoreCase = true)
                    "Mujawwad" -> reciter.style.equals("Mujawwad", ignoreCase = true)
                    "Taraweeh" -> reciter.style.equals("Taraweeh", ignoreCase = true)
                    "Egypt" -> reciter.country.equals("Egypt", ignoreCase = true)
                    "Saudi Arabia" -> reciter.country.equals("Saudi Arabia", ignoreCase = true)
                    else -> true
                }

                matchesQuery && matchesFilter
            }
        }

        Scaffold(
            containerColor = GhaisColors.PitchBlack
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(GhaisColors.PitchBlack)
            ) {
                // Top Bar
                AllRecitersTopBar(
                    totalCount = filteredReciters.size,
                    onBackClick = { navigator.pop() }
                )

                // Search Bar
                ReciterSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                // Filter Chips Row
                ReciterFilterChipsRow(
                    filters = filterOptions,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 2-Column Grid of Reciter Cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp // 120.dp bottom padding for MiniPlayer/dock
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (filteredReciters.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            EmptyRecitersState(query = searchQuery)
                        }
                    } else {
                        items(
                            items = filteredReciters,
                            key = { it.slug }
                        ) { reciter ->
                            ReciterGridCard(
                                reciter = reciter,
                                onClick = {
                                    rootNavigator.push(ReciterProfileScreen(reciter.slug))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top bar with Back button (`navigator.pop()`), Title "Verified Reciters" in bold white,
 * and reciter count badge ("10 Reciters" or dynamic count).
 */
@Composable
private fun AllRecitersTopBar(
    totalCount: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Count Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Verified Reciters",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Reciter count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$totalCount Reciters",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

/**
 * Search Bar with instant text filtering (by English name, Arabic name, or country)
 * in mono glass pill (white 8% + white 10% border).
 */
@Composable
private fun ReciterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search by reciter, Arabic name, country...",
                        color = Color(0xFF6B7280),
                        fontSize = 13.5.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Filter chips row: "All", "Murattal", "Mujawwad", "Taraweeh", "Egypt", "Saudi Arabia"
 * with active chip in silver gradient + dark text.
 */
@Composable
private fun ReciterFilterChipsRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF2F3F5),
                                    Color(0xFFC9CED6)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = filter,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF101216) else Color(0xFF9CA3AF)
                )
            }
        }
    }
}

/**
 * 2-column Reciter Card:
 * - Large circular avatar with mono white 20% border.
 * - Verified mono check badge (black + white check).
 * - Name in bold white, Arabic name, follower count.
 * - Style badge pill (e.g. "Murattal").
 * - Clicking navigates to `ReciterProfileScreen(reciter.slug)`.
 */
@Composable
private fun ReciterGridCard(
    reciter: VerifiedReciter,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Large circular avatar with subtle purple gradient border + Verified badge
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.20f),
                            shape = CircleShape
                        )
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    AsyncImage(
                        model = reciter.photoUrl,
                        contentDescription = reciter.nameEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Verified check badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            // Name in bold white
            Text(
                text = reciter.nameEn,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Arabic name
            Text(
                text = reciter.nameAr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Follower count
            Text(
                text = reciter.followers,
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(9.dp))

            // Style badge pill (e.g. "Murattal")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = reciter.style,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Empty state when search or filter returns no reciters.
 */
@Composable
private fun EmptyRecitersState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (query.isNotBlank()) "No reciters found for \"$query\"" else "No reciters found",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Try searching by name, Arabic spelling, or country",
            color = Color(0xFF9CA3AF),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
