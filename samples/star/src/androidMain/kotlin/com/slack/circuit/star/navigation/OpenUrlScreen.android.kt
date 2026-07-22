// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.navigation

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.android.AndroidScreen
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
actual data class OpenUrlScreen actual constructor(actual val url: String) : Screen, AndroidScreen

internal actual val openUrlScreenSerializer: KSerializer<OpenUrlScreen> = OpenUrlScreen.serializer()
