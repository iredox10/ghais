package com.ghais.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.ghais.data.auth.AppwriteConfig
import com.ghais.data.auth.AuthRepository
import kotlinx.coroutines.launch

private val PureBlack = Color(0xFF000000)
private val LinkBlue = Color(0xFF4C8DFF)
private val MutedGrey = Color(0xFF9A9AA0)
private val GlassSurface = Color(0xFF141417)
private val FieldBorder = Color(0xFF2A2A2E)
private val AuthErrorRed = Color(0xFFFF6B6B)

/**
 * Voyager screen wrapper for the auth gate.
 *
 * Auth is mandatory — there is no guest mode.
 *
 * @param onAuthenticated called on successful sign-in / sign-up (host pops/dismisses the gate).
 * `true` when a brand-new account was just created (host replays onboarding);
 * `false` for returning logins (host goes straight home).
 * @param onGuest legacy no-op kept for backward compatibility; no guest affordance is rendered.
 */
class AuthScreen(
    private val onAuthenticated: (Boolean) -> Unit = {},
    private val onGuest: () -> Unit = {},
) : Screen {
    @Composable
    override fun Content() {
        AuthScreenContent(
            onAuthenticated = onAuthenticated,
            onGuest = onGuest,
        )
    }
}

@Composable
fun AuthScreenContent(
    onAuthenticated: (Boolean) -> Unit,
    onGuest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isSignup by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }

    val configured = AppwriteConfig.isConfigured()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Hero
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(GlassSurface, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ghais_mark),
                    contentDescription = "Ghais",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Ghais",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Listen, learn, reflect",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MutedGrey,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            if (!configured) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Backend not configured",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Backend not configured — paste endpoint in AppwriteConfig",
                            fontSize = 13.sp,
                            color = MutedGrey,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                return@Column
            }

            // Segmented Login / Signup tabs (two pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassSurface, RoundedCornerShape(24.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AuthTabPill(
                    label = "Log in",
                    selected = !isSignup,
                    onClick = {
                        isSignup = false
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                )
                AuthTabPill(
                    label = "Sign up",
                    selected = isSignup,
                    onClick = {
                        isSignup = true
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = MutedGrey,
                unfocusedLabelColor = MutedGrey,
                focusedBorderColor = LinkBlue,
                unfocusedBorderColor = FieldBorder,
                cursorColor = LinkBlue,
            )
            val fieldShape = RoundedCornerShape(20.dp)

            if (isSignup) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = AuthErrorRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Primary action
            Button(
                onClick = {
                    if (loading || googleLoading) return@Button
                    errorMessage = null
                    loading = true
                    scope.launch {
                        val result = if (isSignup) {
                            AuthRepository.signUp(
                                email = email.trim(),
                                name = name.trim(),
                                password = password,
                            )
                        } else {
                            AuthRepository.signIn(
                                email = email.trim(),
                                password = password,
                            )
                        }
                        loading = false
                        result
                            .onSuccess { onAuthenticated(isSignup) }
                            .onFailure { errorMessage = it.message }
                    }
                },
                enabled = !loading && !googleLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LinkBlue,
                    contentColor = Color.White,
                    disabledContainerColor = LinkBlue.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = if (isSignup) "Create account" else "Log in",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Divider "or"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(FieldBorder),
                )
                Text(
                    text = "or",
                    color = MutedGrey,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(FieldBorder),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Google button
            OutlinedButton(
                onClick = {
                    if (loading || googleLoading) return@OutlinedButton
                    errorMessage = null
                    googleLoading = true
                    scope.launch {
                        val result = AuthRepository.signInWithGoogle()
                        googleLoading = false
                        result
                            .onSuccess { onAuthenticated(false) }
                            .onFailure { errorMessage = it.message }
                    }
                },
                enabled = !loading && !googleLoading,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (googleLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "G",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AuthTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) LinkBlue else Color.Transparent,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MutedGrey,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}
