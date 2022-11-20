// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Immutable

@Immutable public actual interface Screen {
  public actual fun update(result: ScreenResult): Screen = this
}

@Immutable public actual interface ScreenResult
