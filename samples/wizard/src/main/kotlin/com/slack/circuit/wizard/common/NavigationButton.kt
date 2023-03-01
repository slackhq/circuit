package com.slack.circuit.wizard.common

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

enum class Direction { LEFT, RIGHT }

@Composable
fun NavigationButton(
  direction: Direction,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onClick: () -> Unit
) {
  val (icon, description) = when (direction) {
    Direction.LEFT -> Icons.Filled.ArrowBack to "Back"
    Direction.RIGHT -> Icons.Filled.ArrowForward to "Forward"
  }
  IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
    Image(
      modifier = modifier,
      painter = rememberVectorPainter(image = icon),
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
      contentDescription = description,
    )
  }
}
