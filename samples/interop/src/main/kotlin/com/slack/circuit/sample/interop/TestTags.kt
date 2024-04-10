// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

object TestTags {
  const val ROOT = "root"
  const val COUNT = "count"
  const val INCREMENT = "increment"
  const val DECREMENT = "decrement"
  const val SOURCE = "source"
  const val UI_SOURCE = "uiSource"
  const val PRESENTER_DROPDOWN = "presenterDropdown"
  const val UI_DROPDOWN = "uiDropdown"

  fun currentSourceFor(label: String): String {
    return "$label-source"
  }
}
