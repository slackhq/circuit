// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

@Parcelize data object TestScreen : Screen

@Parcelize data object TestScreen2 : Screen

@Parcelize data object TestScreen3 : Screen

@Parcelize data object TestPopResult : PopResult
