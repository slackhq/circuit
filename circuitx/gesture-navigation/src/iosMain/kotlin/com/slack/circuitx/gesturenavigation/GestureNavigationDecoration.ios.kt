// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.foundation.animation.AnimatedNavDecorator

@Suppress("OPT_IN_USAGE_FUTURE_ERROR") // TODO remove after migrating to AnchoredDraggable APIs
public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory {
  return CupertinoGestureNavigationDecorator.Factory(onBackInvoked = onBackInvoked)
}
