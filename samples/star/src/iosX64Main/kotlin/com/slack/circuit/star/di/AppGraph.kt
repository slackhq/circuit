// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(scope = AppScope::class)
actual interface AppGraph : CommonAppGraph {
  actual companion object {
    actual fun create(): AppGraph = createGraph<AppGraph>()
  }
}