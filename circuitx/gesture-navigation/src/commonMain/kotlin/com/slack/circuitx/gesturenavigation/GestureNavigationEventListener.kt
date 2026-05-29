// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

/**
 * Observes the back gesture lifecycle so side effects (e.g. analytics) can be wired into gesture
 * navigation. This is purely observational, it does not drive navigation, the `Navigator` still
 * owns the pop.
 *
 * @see GestureNavigationDecorationFactory
 */
public interface GestureNavigationEventListener {

  /**
   * Called as a back gesture progresses.
   *
   * @param progress The gesture progress, ranging from `0f` to `1f` (or `0f` to `-1f` depending on
   *   the swipe edge).
   */
  public fun onBackProgress(progress: Float) {}

  /** Called when a back gesture is cancelled before completing. */
  public fun onBackCancelled() {}

  /** Called when a back gesture completes, after the `Navigator` has popped. */
  public fun onBackCompleted() {}

  public companion object {
    /** A no-op [GestureNavigationEventListener] which ignores all events. */
    public val NoOp: GestureNavigationEventListener = object : GestureNavigationEventListener {}
  }
}
