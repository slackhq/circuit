// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.Navigator

public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  isForwardEnabled: (NavStackList<out NavArgument>) -> Boolean,
  isBackEnabled: (NavStackList<out NavArgument>) -> Boolean,
  onForwardInvoked: (Navigator, NavStackList<out NavArgument>) -> Unit,
  onBackInvoked: (Navigator, NavStackList<out NavArgument>) -> Unit,
): AnimatedNavDecorator.Factory = fallback
