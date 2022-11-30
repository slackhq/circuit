// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * A listener for tracking the state changes of a given [Screen]. This can be used for
 * instrumentation and other telemetry.
 */
public interface EventListener {

  /** Called when there is a new [state] returned by the [Presenter]. */
  public fun onState(state: Any) {}

  /** Called when a new screen is being composed for the first time */
  public fun onScreenInit(screen: Screen) {}

  /** Called when a screen is leaving the composition */
  public fun onScreenDispose(screen: Screen) {}

  public fun interface Factory {
    public fun create(screen: Screen): EventListener
  }

  public companion object {
    public val NONE: EventListener = object : EventListener {}
  }
}

@Composable
internal fun rememberEventListener(screen: Screen, factory: EventListener.Factory): EventListener {
  val listener = remember(screen) { factory.create(screen) }
  DisposableEffect(screen) {
    listener.onScreenInit(screen)
    onDispose { listener.onScreenDispose(screen) }
  }

  return listener
}