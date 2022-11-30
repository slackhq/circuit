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

  public fun interface Factory {
    public fun create(screen: Screen, circuitConfig: CircuitConfig): EventListener
  }

  public companion object {
    public val NONE: EventListener = object : EventListener {}
  }
}
