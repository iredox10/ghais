package com.ghais.player

import androidx.compose.runtime.Composable

/**
 * Screen-level system-back interceptor for the player.
 *
 * commonMain declares the contract; androidMain routes through
 * androidx.activity.compose.BackHandler, iosMain is a no-op (no system back).
 */
@Composable
expect fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit)
