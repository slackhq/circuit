// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.stateprototyping

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.CircuitUiEvent

@Stable
interface EventSink<UiEvent : CircuitUiEvent> {
  fun send(event: UiEvent)
}

inline fun <UiEvent : CircuitUiEvent> eventSink(
  crossinline body: (UiEvent) -> Unit
): EventSink<UiEvent> =
  object : EventSink<UiEvent> {
    override fun send(event: UiEvent) {
      body(event)
    }
  }
