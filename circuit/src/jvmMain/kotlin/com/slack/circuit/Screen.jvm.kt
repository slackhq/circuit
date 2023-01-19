// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Immutable

@Immutable public actual interface Screen

@Immutable
public actual interface NavigableScreen : Screen {
  public actual val route: String
}
