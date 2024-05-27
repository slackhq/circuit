// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.material.ExperimentalMaterialApi
import com.slack.circuit.backstack.NavDecoration

@OptIn(ExperimentalMaterialApi::class)
public actual fun GestureNavigationDecoration(
  fallback: NavDecoration,
  onBackInvoked: () -> Unit,
): NavDecoration = CupertinoGestureNavigationDecoration(onBackInvoked = onBackInvoked)
