// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.star.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(scope = AppScope::class)
interface StarTestGraph {
  val circuitSaver: CircuitSaver

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@ApplicationContext @Provides context: Context): StarTestGraph
  }
}

internal actual fun testCircuitSaver(): CircuitSaver {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val graph = createGraphFactory<StarTestGraph.Factory>().create(context)
  return graph.circuitSaver
}
