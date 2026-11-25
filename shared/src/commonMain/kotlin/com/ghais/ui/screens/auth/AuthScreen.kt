package com.ghais.ui.screens.auth

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import com.ghais.ui.components.noir.GhostPillButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.data.auth.BackendConfig
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
 * Voyager screen wrapper for authentication (guest-first; dismissible when pushed).
 *
 * @param onAuthenticated called on successful sign-in / sign-up (host pops/dismisses the gate).
 * `true` when a brand-new account was just created (host replays onboarding);
 * `false` for returning logins (host goes straight home).
 * @param onGuest optional callback invoked when the user chooses to continue without signing in
 * (defaults to popping the current navigator when pushed).
 */
class AuthScreen(
    private val onAuthenticated: (Boolean) -> Unit = {},
    private val onGuest: () -> Unit = {},
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        AuthScreenContent(
            onAuthenticated = onAuthenticated,
            onGuest = {
                onGuest()
                if (navigator?.canPop == true) navigator.pop()
            },
            canDismiss = navigator?.canPop == true,
        )
    }
}

@Composable
fun AuthScreenContent(
    onAuthenticated: (Boolean) -> Unit,
    onGuest: () -> Unit = {},
    canDismiss: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isSignup by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun submitEmailAuth() {
        if (loading || googleLoading) return
        focusManager.clearFocus()
        errorMessage = null
        infoMessage = null
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
    }

    val isOnline by NetworkMonitor.isOnline.collectAsState()

    val configured = BackendConfig.isConfigured()

    NoirScreenRoot(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (canDismiss) Arrangement.Top else Arrangement.Center,
        ) {
            if (canDismiss) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                            .noirClickable(onClick = onGuest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
            // Hero — monochrome mark well.
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(GhaisNoir.wellFill(), GhaisShapes.cardNoir)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir),
                contentAlignment = Alignment.Center,
            ) {
                com.ghais.ui.components.GhaithLogo(
                    modifier = Modifier.size(64.dp),
                    animated = true,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Ghaith",
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
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
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = GhaisNoir.TextTertiary,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitEmailAuth() }
                ),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!isSignup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "Forgot password?",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .noirClickable {
                                errorMessage = null
                                showResetDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            if (infoMessage != null) {
                Spacer(Modifier.height(10.dp))
                NoirInsetField(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = infoMessage.orEmpty(),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

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
                    onClick = ::submitEmailAuth,
                    enabled = !loading && !googleLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isSignup) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "By creating an account, you agree to our Terms & Privacy Policy.",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
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
                            infoMessage = null
                            googleLoading = true
                            scope.launch {
                                val result = AuthRepository.signInWithGoogle()
                                googleLoading = false
                                result
                                    .onSuccess { isNewUser ->
                                        onAuthenticated(isNewUser)
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

            if (canDismiss) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Continue as guest",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .noirClickable(onClick = onGuest)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showResetDialog) {
        PasswordResetDialog(
            initialEmail = email,
            onDismiss = { showResetDialog = false },
            onResetSuccess = { msg ->
                showResetDialog = false
                infoMessage = msg
            },
        )
    }
}

@Composable
private fun PasswordResetDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onResetSuccess: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

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
    val fieldShape = RoundedCornerShape(20.dp)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(GhaisNoir.CanvasTop)
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(28.dp))
                .padding(20.dp),
        ) {
            Text(
                text = if (codeSent) "Enter reset code" else "Reset password",
                color = GhaisNoir.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (codeSent) {
                    "Enter the 6-digit code sent to $resetEmail and choose a new password."
                } else {
                    "We'll send a 6-digit reset code to your email."
                },
                color = GhaisNoir.TextTertiary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = resetEmail,
                onValueChange = { resetEmail = it },
                label = { Text("Email") },
                singleLine = true,
                enabled = !codeSent && !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            if (codeSent) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit code") },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password (min 8 chars)") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (dialogError != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = dialogError.orEmpty(),
                    color = GhaisNoir.TextPrimary,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GhostPillButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                ChromePillButton(
                    text = when {
                        busy -> "Please wait"
                        codeSent -> "Update"
                        else -> "Send code"
                    },
                    enabled = !busy,
                    onClick = {
                        if (busy) return@ChromePillButton
                        dialogError = null
                        busy = true
                        scope.launch {
                            if (!codeSent) {
                                AuthRepository.requestPasswordReset(resetEmail)
                                    .onSuccess { codeSent = true }
                                    .onFailure { dialogError = it.message }
                            } else {
                                AuthRepository.confirmPasswordReset(resetEmail, code, newPassword)
                                    .onSuccess { msg -> onResetSuccess(msg) }
                                    .onFailure { dialogError = it.message }
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
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
