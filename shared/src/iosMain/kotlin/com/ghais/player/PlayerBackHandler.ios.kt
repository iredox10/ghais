package com.ghais.player

import androidx.compose.runtime.Composable

/**
 * iOS no-op: no system back button to intercept. Swipe-back (if any) is
 * handled by Voyager/transitions, out of scope for this guard.
 */
@Composable
actual fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
