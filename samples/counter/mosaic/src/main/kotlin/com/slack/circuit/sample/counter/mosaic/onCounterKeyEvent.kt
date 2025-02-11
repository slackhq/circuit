// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.mosaic

import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.slack.circuit.sample.counter.CounterScreen

private val Escape = KeyEvent("Escape")
private val q = KeyEvent("q")
private val Q = KeyEvent("Q")

private val ArrowUp = KeyEvent("ArrowUp")

private val ArrowDown = KeyEvent("ArrowDown")

internal fun Modifier.onCounterKeyEvent(onEvent: (CounterScreen.Event) -> Unit): Modifier {
  return this.onKeyEvent {
    when (it) {
      Escape,
      q,
      Q -> onEvent(CounterScreen.Event.Escape)
      ArrowUp -> onEvent(CounterScreen.Event.Increment)
      ArrowDown -> onEvent(CounterScreen.Event.Decrement)
      else -> return@onKeyEvent false
    }
    true
  }
}
