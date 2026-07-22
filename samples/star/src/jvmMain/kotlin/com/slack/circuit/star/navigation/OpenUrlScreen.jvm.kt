// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.navigation

import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable actual data class OpenUrlScreen actual constructor(actual val url: String) : Screen

internal actual val openUrlScreenSerializer: KSerializer<OpenUrlScreen> = OpenUrlScreen.serializer()
