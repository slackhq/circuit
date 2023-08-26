// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import kotlin.DeprecationLevel.ERROR

@Deprecated(
    "Use Screen from the new package instead",
    ReplaceWith("Screen", "com.slack.circuit.runtime.screen.Screen"),
    ERROR)
public typealias Screen = com.slack.circuit.runtime.screen.Screen
