package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
public expect fun SharedElementTransitionLayout(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
)
