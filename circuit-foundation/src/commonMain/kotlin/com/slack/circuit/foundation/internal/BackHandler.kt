package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable

/**
 * A multiplatform abstraction over a BackHandler that's only implemented on Android and a no-op
 * elsewhere.
 */
@Composable public expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
