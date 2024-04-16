// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.navigation

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.parcel.CommonParcelize

// TODO https://kotlinlang.slack.com/archives/C03PK0PE257/p1713288571883269
@CommonParcelize
data class OpenUrlScreen(val url: String) : Screen
