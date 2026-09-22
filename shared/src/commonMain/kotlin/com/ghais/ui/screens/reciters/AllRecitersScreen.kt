package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

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

/** True-grayscale filter — portraits stay recognisable while strictly monochrome. */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Noir Glass — strict monochrome redesign.
 *
 * Canvas #050506 via [NoirScreenRoot] (glow zone -> absolute black + grain).
 * Surfaces are alpha-white (Fill2 -> FillDeep) + 1px [GhaisNoir.BorderCard] +
 * 22% top-only specular. Text ladder 100/62/38/24%. Zero hue.
 *
 * Signature, search/filter logic and navigation preserved:
 * `navigator.pop()` back, `rootNavigator.push(ReciterProfileScreen(slug))` on row tap.
 */
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

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                // Top bar — IconWell back + title + monochrome count chip
                AllRecitersTopBar(
                    totalCount = filteredReciters.size,
                    onBackClick = { navigator.pop() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search — engraved NoirInsetField
                ReciterSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filters — chrome pill (selected) / ghost pill (resting)
                ReciterFilterChipsRow(
                    filters = filterOptions,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section label — shared Noir rhythm (home + profile pattern)
                NoirSectionHeader(
                    label = "All reciters",
                    actionLabel = "${filteredReciters.size} shown",
                    onAction = {}
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Biggest list — single-column Noir rows (was 2-col grid)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 2.dp,
                        bottom = 120.dp // MiniPlayer / dock clearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredReciters.isEmpty()) {
                        item {
                            EmptyRecitersState(query = searchQuery)
                        }
                    } else {
                        items(
                            items = filteredReciters,
                            key = { it.slug }
                        ) { reciter ->
                            ReciterNoirRow(
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
 * Top bar: clay [IconWell] back affordance, bold white title,
 * monochrome count chip (Fill2 wash + ghost hairline, 62% text).
 */
@Composable
private fun AllRecitersTopBar(
    totalCount: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.noirClickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            IconWell(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "Verified Reciters",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GhaisNoir.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Monochrome count chip — informational, no press affordance.
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$totalCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.TextSecondary
            )
        }
    }
}

/**
 * Search field in [NoirInsetField] style: engraved recessed surface
 * (black 45% gradient + 5% rim), white cursor, 24% placeholder.
 */
@Composable
private fun ReciterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    NoirInsetField(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search by reciter, Arabic name, country...",
                        color = GhaisNoir.TextDisabled,
                        fontSize = 13.5.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape)
                        .noirClickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Filter pills: selected = chrome gradient fill + near-black label
 * (primary action); resting = Fill2 wash + ghost hairline + 62% label.
 */
@Composable
private fun ReciterFilterChipsRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable { onFilterSelected(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.OnChrome
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .noirClickable { onFilterSelected(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Reciter row in the [com.ghais.ui.components.noir.NoirListRow] language:
 * soft card fill + 1px card border + 22% top-only specular, grayscale
 * photo well with monogram fallback, dual text, ghost style chip,
 * circular chevron affordance. Tapping navigates to
 * `ReciterProfileScreen(reciter.slug)` (signature preserved).
 */
@Composable
private fun ReciterNoirRow(
    reciter: VerifiedReciter,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReciterPhotoWell(reciter = reciter)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ghost style chip — informational only.
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reciter.style,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${reciter.country} · ${reciter.followers}",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Grayscale portrait in a clay well (home `NoirArtworkWell` pattern):
 * desaturated photo + 35% black scrim so it reads engraved, monogram
 * fallback in [IconWell] language, chrome verified dot (zero hue).
 */
@Composable
private fun ReciterPhotoWell(reciter: VerifiedReciter) {
    Box {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (reciter.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = reciter.photoUrl,
                    contentDescription = reciter.nameEn,
                    contentScale = ContentScale.Crop,
                    colorFilter = NoirGrayscale,
                    modifier = Modifier.fillMaxSize()
                )
                // Darkening scrim keeps the plate recessed instead of glowing.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                Text(
                    text = reciter.nameEn.firstOrNull()?.uppercase() ?: "Q",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Verified dot — chrome disc, near-black glyph (was purple badge).
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.BottomEnd)
                .background(GhaisNoir.chromeFill(), CircleShape)
                .border(2.dp, GhaisNoir.NoirBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified",
                tint = GhaisNoir.OnChrome,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

/**
 * Empty state — clay [IconWell], 100% headline + 62% hint.
 */
@Composable
private fun EmptyRecitersState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconWell(
            icon = Icons.Default.Search,
            size = 64.dp,
            iconSize = 30.dp,
            contentDescription = null,
            tint = GhaisNoir.TextTertiary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (query.isNotBlank()) "No reciters found for \"$query\"" else "No reciters found",
            color = GhaisNoir.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Try searching by name, Arabic spelling, or country",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
