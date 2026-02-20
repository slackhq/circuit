// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.mosaic

import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.PrimeScreen

private val Escape = KeyEvent("Escape")
private val Backspace = KeyEvent("Backspace")
private val q = KeyEvent("q")
private val Q = KeyEvent("Q")
private val p = KeyEvent("p")
private val P = KeyEvent("P")

private val ArrowUp = KeyEvent("ArrowUp")

private val ArrowDown = KeyEvent("ArrowDown")

internal fun Modifier.onCounterKeyEvent(
  count: Int,
  onEvent: (CounterScreen.Event) -> Unit,
): Modifier {
  return this.onKeyEvent {
    when (it) {
      Escape,
      q,
      Q -> onEvent(CounterScreen.Event.Escape)

      p,
      P ->
        if (count >= 0) {
          onEvent(CounterScreen.Event.GoTo(PrimeScreen(count)))
        } else return@onKeyEvent false

      ArrowUp -> onEvent(CounterScreen.Event.Increment)
      ArrowDown -> onEvent(CounterScreen.Event.Decrement)
      else -> return@onKeyEvent false
    }
    true
  }
}

internal fun Modifier.onPrimeKeyEvent(onEvent: (PrimeScreen.Event) -> Unit): Modifier {
  return this.onKeyEvent {
    when (it) {
      Escape,
      Backspace -> onEvent(PrimeScreen.Event.Pop)
      else -> return@onKeyEvent false
    }
    true
  }
}
