package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect object Platform {
  @Composable
  fun ReportDrawnWhen(predicate: () -> Boolean)

  @Composable
  fun appIconPainter(): Painter
}