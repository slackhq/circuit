// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.internal.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
actual fun rememberViewModel(key: String?): MutableIntState {
  return rememberSaveable(key = key) { mutableIntStateOf(0) }
}
