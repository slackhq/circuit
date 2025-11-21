// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.screen.Screen

/**
 * Provides context about the current navigation state during navigation events.
 *
 * @see NavigationInterceptor
 * @see NavigationEventListener
 */
public interface NavigationContext {

  /**
   * Returns the current screen at the top of the backstack, or null if the backstack is empty or
   * unavailable.
   */
  public fun peek(): Screen?

  /** Returns the full navigation backstack, or null if the backstack is unavailable. */
  public fun peekBackStack(): List<Screen>?

  /**
   * Returns a snapshot of the current navigation stack with position tracking, or null if
   * unavailable.
   */
  public fun peekNavStack(): NavStackList<Screen>?
}

/** A no-op [NavigationContext] implementation with no navigation state. */
public object NoOpNavigationContext : NavigationContext {
  override fun peek(): Screen? = null

  override fun peekBackStack(): List<Screen>? = null

  override fun peekNavStack(): NavStackList<Screen>? = null
}
