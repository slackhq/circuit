// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import com.slack.circuit.star.data.StarAppDirs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(scope = AppScope::class)
interface AppGraph : CommonAppGraph {

  val appDirs: StarAppDirs

  companion object {
    fun create(): AppGraph = createGraph<AppGraph>()
  }
}
