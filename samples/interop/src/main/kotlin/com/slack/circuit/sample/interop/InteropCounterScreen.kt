// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import com.slack.circuit.runtime.screen.ParcelableScreen
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class InteropCounterScreen(
  val presenterSource: PresenterSource,
  val uiSource: UiSource,
  val count: Int,
) : ParcelableScreen
