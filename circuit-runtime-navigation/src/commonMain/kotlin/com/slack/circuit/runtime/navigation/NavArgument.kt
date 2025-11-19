// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.navigation

import com.slack.circuit.runtime.screen.Screen

/** Argument provided to [NavDecoration] that exposes the underlying [Screen]. */
public interface NavArgument {
  public val screen: Screen
  public val key: String
}
