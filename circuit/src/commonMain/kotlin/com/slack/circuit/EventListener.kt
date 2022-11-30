// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

/**
 * A listener for tracking the state changes of a given [Screen]. This can be used for
 * instrumentation and other telemetry.
 */
public interface EventListener {

  /** Called when there is a new [state] returned by the [Presenter]. */
  public fun onState(state: Any) {}

  /** Called when a new [Screen] is being composed for the first time */
  public fun onScreenInit(screen: Screen) {}

  /** Called when a [Screen] is leaving the composition */
  public fun onScreenDispose(screen: Screen) {}

  public fun interface Factory {
    public fun create(screen: Screen): EventListener
  }

  public companion object {
    public val NONE: EventListener = object : EventListener {}
  }
}
