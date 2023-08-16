// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.navigation

import com.slack.circuitx.android.AndroidScreen
import kotlinx.parcelize.Parcelize

@Parcelize data class CustomTabsIntentScreen(val url: String) : AndroidScreen
