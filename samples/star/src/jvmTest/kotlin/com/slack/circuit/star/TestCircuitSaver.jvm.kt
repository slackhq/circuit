// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import com.slack.circuit.runtime.screen.CircuitSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(scope = AppScope::class)
interface StarTestGraph {
  val circuitSaver: CircuitSaver
}

internal actual fun testCircuitSaver(): CircuitSaver = createGraph<StarTestGraph>().circuitSaver
