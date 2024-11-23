// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.parcel.CommonParcelize

@CommonParcelize
data class ImageViewerScreen(
  val id: Long,
  val url: String,
  val index: Int,
  val placeholderKey: String?,
) : Screen {
  data class State(
    val id: Long,
    val url: String,
    val index: Int,
    val placeholderKey: String?,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object Close : Event

    data object NoOp : Event // Weird but necessary because of the reuse in bottom sheet
  }
}
