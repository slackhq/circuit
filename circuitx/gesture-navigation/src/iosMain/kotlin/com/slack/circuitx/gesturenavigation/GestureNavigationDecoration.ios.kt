// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.material.ExperimentalMaterialApi
import com.slack.circuit.foundation.animation.AnimatedNavDecorator

@OptIn(ExperimentalMaterialApi::class)
public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory {
  return CupertinoGestureNavigationDecorator.Factory(onBackInvoked = onBackInvoked)
}
