package com.ghais.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.theme.GhaisNoir

/**
 * Black splash shown while session restore is in flight (prevents auth-gate flash).
 *
 * Noir Glass: plain canvas + centered monochrome mark. Deliberately lightweight —
 * no glow, no grain, no store reads, no heavy work.
 */
object SplashScreen : Screen {
    @Composable
    override fun Content() {
        NoirScreenRoot(glow = false, grain = false) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ghais_mark),
                    contentDescription = "Ghais",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Ghais",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GhaisNoir.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator(
                    color = GhaisNoir.TextSecondary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
