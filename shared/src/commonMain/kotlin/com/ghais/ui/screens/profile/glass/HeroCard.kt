package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HeroEmerald = Color(0xFF4EDEA3)
private val HeroCardTop = Color.White.copy(alpha = 0.09f)
private val HeroCardBottom = Color.White.copy(alpha = 0.03f)
private val HeroBorderTop = HeroEmerald.copy(alpha = 0.25f)
private val HeroBorderBottom = Color.White.copy(alpha = 0.08f)
private val HeroTitle = Color.White
private val HeroBio = Color.White.copy(alpha = 0.60f)
private val HeroEmail = Color.White.copy(alpha = 0.48f)
private val HeroBlue = Color(0xFF5B9DFF)
private val HeroGrey = Color(0xFF9AA0A6)
private val HeroChromeTop = Color(0xFFF5F5F7)
private val HeroChromeBottom = Color(0xFFC7C7CC)
private val HeroChromeContent = Color(0xFF0B0C0E)
private val HeroLogout = Color(0xFFFF6B6B).copy(alpha = 0.90f)
private val HeroAvatarInnerTop = Color(0xFF1D2F28)
private val HeroAvatarInnerBottom = Color(0xFF0B0C0E)

@Composable
fun ProfileHeroGlass(
    userName: String,
    userBio: String,
    onEditClick: () -> Unit,
    sessionName: String? = null,
    sessionEmail: String? = null,
    isGuest: Boolean = true,
    onLogoutClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(26.dp)
    val displayName = sessionName?.takeIf { it.isNotBlank() } ?: userName

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(HeroCardTop, HeroCardBottom)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(HeroBorderTop, HeroBorderBottom)),
                shape = cardShape
            )
            .padding(vertical = 26.dp, horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-120).dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            HeroEmerald.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                HeroEmerald,
                                Color(0xFF1A7A52),
                                Color(0xFF9FF5D0),
                                Color(0xFF2BBF84),
                                HeroEmerald
                            )
                        )
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(HeroAvatarInnerTop, HeroAvatarInnerBottom)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "G",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeroEmerald
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Text(
                text = displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeroTitle,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = userBio,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = HeroBio,
                textAlign = TextAlign.Center
            )

            if (!sessionEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = sessionEmail,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = HeroEmail,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroBadge(text = "PRO MEMBER", base = HeroBlue)
                Spacer(modifier = Modifier.width(8.dp))
                HeroBadge(text = "CLOUD SYNCED", base = HeroEmerald)
                if (isGuest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    HeroBadge(text = "GUEST MODE", base = HeroGrey)
                }
            }

            Spacer(modifier = Modifier.size(18.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.verticalGradient(listOf(HeroChromeTop, HeroChromeBottom))
                    )
                    .clickable(onClick = onEditClick)
                    .padding(vertical = 13.dp, horizontal = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = HeroChromeContent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Profile",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeroChromeContent
                )
            }

            if (!isGuest) {
                TextButton(onClick = onLogoutClick) {
                    Text(
                        text = "Logout",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeroLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBadge(text: String, base: Color) {
    val pill = RoundedCornerShape(50.dp)
    Box(
        modifier = Modifier
            .clip(pill)
            .background(base.copy(alpha = 0.16f))
            .border(1.dp, base.copy(alpha = 0.35f), pill)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = base,
            textAlign = TextAlign.Center
        )
    }
}
