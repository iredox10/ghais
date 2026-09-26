package com.ghais.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ghais.data.sync.NetworkMonitor
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import kotlinx.coroutines.launch

/**
 * Voyager screen wrapper for the auth gate.
 *
 * Auth is mandatory — there is no guest mode.
 *
 * @param onAuthenticated called on successful sign-in / sign-up (host pops/dismisses the gate).
 * `true` when a brand-new account was just created (host replays onboarding);
 * `false` for returning logins (host goes straight home).
 *
 * Email: `true` iff the Sign-up tab was used — Appwrite rejects an existing
 * email with 409, so a successful `signUp` is always a fresh account.
 *
 * Google: there is no local "signup" intent to read, so newness is resolved by
 * [resolveGoogleIsNewAccount] against the userId this device had cached
 * *before* the OAuth round-trip. It fails OPEN (reports `true`) whenever the
 * device holds no prior account, so a new Google user is never dropped on Home
 * with a `null` goal.
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

    val isOnline by NetworkMonitor.isOnline.collectAsState()

    val configured = AppwriteConfig.isConfigured()

    NoirScreenRoot(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Hero — monochrome mark well.
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(GhaisNoir.wellFill(), GhaisShapes.cardNoir)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir),
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
                color = GhaisNoir.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Listen, learn, reflect",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (isSignup) "Create your account" else "Welcome back",
                style = GhaisTypography.displayEditorialSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            if (!configured) {
                NoirCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Backend not configured",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = GhaisNoir.TextPrimary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "The service isn't set up yet. Please try again later.",
                            fontSize = 13.sp,
                            color = GhaisNoir.TextTertiary,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                return@Column
            }

            // Segmented Login / Signup tabs — engraved track, chrome selection.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GhaisNoir.insetFill(), GhaisShapes.pill)
                    .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
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
                focusedTextColor = GhaisNoir.TextPrimary,
                unfocusedTextColor = GhaisNoir.TextPrimary,
                focusedLabelColor = GhaisNoir.TextTertiary,
                unfocusedLabelColor = GhaisNoir.TextDisabled,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = GhaisNoir.BorderCard,
                cursorColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            )
            val fieldShape = RoundedCornerShape(24.dp)

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
                NoirInsetField(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Slim non-blocking offline notice — ghost well, tertiary text.
            // Buttons stay tappable; the repository preflight surfaces the friendly message.
            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "You're offline — connect to log in",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Primary action — chrome CTA.
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .padding(horizontal = 28.dp, vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = GhaisNoir.OnChrome,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                ChromePillButton(
                    text = if (isSignup) "Create account" else "Log in",
                    onClick = {
                        if (loading || googleLoading) return@ChromePillButton
                        errorMessage = null
                        val wasSignup = isSignup
                        loading = true
                        scope.launch {
                            val result = if (wasSignup) {
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
                                .onSuccess { onAuthenticated(wasSignup) }
                                .onFailure {
                                    errorMessage = it.message?.takeIf { msg -> msg.isNotBlank() }
                                        ?: "Authentication failed. Please try again."
                                }
                        }
                    },
                    enabled = !loading && !googleLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        .background(GhaisNoir.BorderGhost),
                )
                Text(
                    text = "or",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(GhaisNoir.BorderGhost),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Google button — ghost pill.
            if (googleLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = GhaisNoir.TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .noirClickable(onClick = {
                            if (loading || googleLoading) return@noirClickable
                            errorMessage = null
                            // Snapshot BEFORE the round-trip: the successful
                            // sign-in overwrites cachedSession with the account
                            // that just logged in, which would erase the only
                            // evidence that this device knew someone else.
                            val userIdCachedBefore = AuthRepository.cachedSession.value?.userId
                            googleLoading = true
                            scope.launch {
                                val result = AuthRepository.signInWithGoogle()
                                googleLoading = false
                                result
                                    .onSuccess {
                                        onAuthenticated(
                                            resolveGoogleIsNewAccount(userIdCachedBefore),
                                        )
                                    }
                                    .onFailure {
                                        errorMessage = it.message?.takeIf { msg -> msg.isNotBlank() }
                                            ?: "Authentication failed. Please try again."
                                    }
                            }
                        })
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            color = GhaisNoir.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Decides whether the Google account that just signed in is brand new.
 *
 * ## Signal
 * [AuthRepository.cachedSession] — the last userId this device persisted —
 * sampled *before* the OAuth round-trip. Purely local: no network, so it can
 * never hang the sign-in path. `AuthRepository` exposes no account
 * `createdAt` and no database probe, so a device-local comparison is the only
 * newness signal reachable from this file.
 *
 * ## Outcome table
 * | Pre-sign-in cached userId | Signed-in userId | Reported |
 * |---|---|---|
 * | null (fresh install / after sign-out) | any | `true` — nothing here has ever been established |
 * | `A` | `B` | `true` — account switch to an account this device never held |
 * | `A` | `A` | `false` — returning login on this device |
 * | `A` | null (refresh failed) | `false` — mirror the pre-sign-in state, do not claim newness we cannot prove |
 * | null | null (refresh failed) | `true` — device held no prior account; fail open |
 *
 * ## Failure direction
 * Fails OPEN, deliberately: any case where newness cannot be *disproven*
 * reports `true`, so a new Google user always reaches onboarding instead of
 * landing on Home with a `null` goal. The cost of failing open is that a
 * returning user who signs in on a *different* device than the one they last
 * used sees onboarding once more — cosmetic and self-correcting (completing it
 * sets the per-user done flag), whereas failing closed would permanently
 * strand a new user with no goal.
 */
private fun resolveGoogleIsNewAccount(userIdCachedBefore: String?): Boolean {
    val userIdNow = AuthRepository.session.value?.userId
    return when {
        userIdNow == null -> userIdCachedBefore == null
        userIdCachedBefore == null -> true
        else -> userIdNow != userIdCachedBefore
    }
}

@Composable
private fun AuthTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pillModifier = modifier
        .clip(RoundedCornerShape(20.dp))
        .then(
            if (selected) {
                Modifier.background(GhaisNoir.chromeFill())
            } else {
                Modifier.background(Color.Transparent)
            }
        )
        .noirClickable(onClick = onClick)
        .padding(vertical = 10.dp)
    Box(
        modifier = pillModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) GhaisNoir.OnChrome else GhaisNoir.TextTertiary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}
