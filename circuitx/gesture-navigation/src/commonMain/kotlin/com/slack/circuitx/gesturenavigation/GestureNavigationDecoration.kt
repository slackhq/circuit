// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecorator

/**
 * Returns a [AnimatedNavDecorator.Factory] implementation which support navigation through
 * appropriate gestures on certain platforms.
 * * When running on Android 14 (or never) devices, this decoration supports Android's 'predictive
 *   back gesture'.
 * * When running on iOS, this decoration simulates iOS's 'interative pop gesture'.
 * * On other platforms, it defers to `NavigatorDefaults.DefaultDecoration`.
 *
 * @param fallback The [AnimatedNavDecorator.Factory] which should be used when running on platforms
 *   which [GestureNavigationDecorationFactory] does not support.
 * @param onBackInvoked A lambda which will be called when the user has invoked a 'back' gesture.
 *   Typically this should call `Navigator.pop()`.
 */
public expect fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory = NavigatorDefaults.DefaultDecoratorFactory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory
