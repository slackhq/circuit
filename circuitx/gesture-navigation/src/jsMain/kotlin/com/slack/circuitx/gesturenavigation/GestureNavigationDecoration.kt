// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.NavigatorDefaults

public actual fun GestureNavigationDecoration(
  onBack: () -> Unit,
): NavDecoration = NavigatorDefaults.DefaultDecoration
