// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.screen.ParcelablePopResult
import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.StaticScreen

@Parcelize data object TestScreen : ParcelableScreen

@Parcelize data object TestScreen2 : ParcelableScreen

@Parcelize data object TestScreen3 : ParcelableScreen

@Parcelize data object TestStaticScreen : StaticScreen, ParcelableScreen

@Parcelize data object TestPopResult : ParcelablePopResult

@Parcelize data class TestValuePopResult(val value: String) : ParcelablePopResult

@Parcelize data object OtherPopResult : ParcelablePopResult
