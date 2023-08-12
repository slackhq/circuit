package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as ActivityBackHandler

@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  ActivityBackHandler(enabled = enabled, onBack = onBack)
}