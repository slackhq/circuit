// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.foundation.animation.AnimatedNavDecorator

public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory {
  TODO("Rebuild CupertinoGestureNavigationDecoration as a AnimatedNavDecorator.Factory")
  //    CupertinoGestureNavigationDecoration(onBackInvoked = onBackInvoked)
}
