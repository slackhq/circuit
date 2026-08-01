// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial

import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.serialization.reflect.ReflectiveSerializableCircuitSaver

fun buildCircuitSaver(): CircuitSaver = ReflectiveSerializableCircuitSaver()
