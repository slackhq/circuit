// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.backstack.NavDecoration

public actual fun GestureNavigationDecoration(
  onBackInvoked: () -> Unit,
  fallback: NavDecoration,
): NavDecoration = fallback
