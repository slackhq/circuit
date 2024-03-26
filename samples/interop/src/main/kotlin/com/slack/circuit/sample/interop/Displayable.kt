// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.Stable

@Stable
interface Displayable {
  val index: Int
  val presentationName: String
}
