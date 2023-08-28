package com.slack.circuit.foundation.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable

@Composable
internal actual fun BackHandlerBox(content: @Composable () -> Unit) {
  Box { content() }
}
