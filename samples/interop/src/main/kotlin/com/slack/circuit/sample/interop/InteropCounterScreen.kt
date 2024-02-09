// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import android.os.Parcelable
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

@Parcelize
data class InteropCounterScreen(
  val presenterSource: PresenterSource,
  val uiSource: UiSource,
  val count: Int,
) : Screen, Parcelable
