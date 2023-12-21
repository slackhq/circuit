// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.presenter.Presenter

/**
 * Remembers an [eventSink] for use in a Circuit [Presenter].
 *
 * It's recommended to set [keys] to non-[State] values captured by [eventSink] that can change.
 */
@Composable
public fun <T : CircuitUiEvent> rememberEventSink(
  vararg keys: Any?,
  eventSink: (T) -> Unit
): (T) -> Unit {
  return remember(*keys) { eventSink }
}
