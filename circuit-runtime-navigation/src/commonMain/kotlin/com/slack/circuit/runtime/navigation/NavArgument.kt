// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import com.slack.circuit.runtime.screen.Screen

/** Argument used in navigation that exposes the underlying [Screen]. */
public interface NavArgument {
  /**
   * Identifier unique to the lifespan of the argument. Must be stable across configuration changes.
   */
  public val key: String

  /** The [Screen] that this argument presents. */
  public val screen: Screen
}
