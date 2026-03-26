// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import androidx.compose.runtime.Composable

interface ConditionalSystemUiColors {
  fun save()

  fun restore()

  object None : ConditionalSystemUiColors {
    override fun save() {}

    override fun restore() {}
  }
}

@Composable expect fun rememberConditionalSystemUiColors(): ConditionalSystemUiColors
