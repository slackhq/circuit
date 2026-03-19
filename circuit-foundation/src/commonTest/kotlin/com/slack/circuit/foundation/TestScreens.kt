// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.StaticScreen

@Parcelize data object TestScreen : Screen

@Parcelize data object TestScreen2 : Screen

@Parcelize data object TestScreen3 : Screen

@Parcelize data object TestStaticScreen : StaticScreen

@Parcelize data object TestPopResult : PopResult

@Parcelize data class TestValuePopResult(val value: String) : PopResult

@Parcelize data object OtherPopResult : PopResult
