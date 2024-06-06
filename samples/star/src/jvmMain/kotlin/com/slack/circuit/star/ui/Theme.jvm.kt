package com.slack.circuit.star.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun StarTheme(
  useDarkTheme: Boolean,
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      useDarkTheme -> darkScheme
      else -> lightScheme
    }

  // No custom fonts available in Desktop
  MaterialTheme(colorScheme = colorScheme, content = content)
}
