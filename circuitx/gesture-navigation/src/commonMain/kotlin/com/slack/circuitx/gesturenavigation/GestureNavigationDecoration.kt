// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.AnimatedNavigationTransform
import com.slack.circuit.foundation.NavigatorDefaults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Returns a [NavDecoration] implementation which support navigation through appropriate gestures on
 * certain platforms.
 * * When running on Android 14 (or never) devices, this decoration supports Android's 'predictive
 *   back gesture'.
 * * When running on iOS, this decoration simulates iOS's 'interative pop gesture'.
 * * On other platforms, it defers to `NavigatorDefaults.DefaultDecoration`.
 *
 * @param fallback The [NavDecoration] which should be used when running on platforms which
 *   [GestureNavigationDecoration] does not support.
 * @param onBackInvoked A lambda which will be called when the user has invoked a 'back' gesture.
 *   Typically this should call `Navigator.pop()`.
 */
public expect fun GestureNavigationDecoration(
  animatedNavOverrides: ImmutableList<AnimatedNavigationTransform> = persistentListOf(),
  fallback: NavDecoration = NavigatorDefaults.DefaultDecoration,
  onBackInvoked: () -> Unit,
): NavDecoration
