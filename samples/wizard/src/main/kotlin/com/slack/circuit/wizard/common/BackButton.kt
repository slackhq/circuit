package com.slack.circuit.wizard.common

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun BackButton(modifier: Modifier = Modifier, onBack: () -> Unit) {
  IconButton(modifier = modifier, onClick = onBack) {
    Image(
      modifier = modifier,
      painter = rememberVectorPainter(image = Icons.Filled.ArrowBack),
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
      contentDescription = "Close",
    )
  }
}
