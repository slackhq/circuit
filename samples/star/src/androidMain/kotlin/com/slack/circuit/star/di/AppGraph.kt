// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import android.content.Context
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass

@DependencyGraph(
  scope = AppScope::class,
)
@SingleIn(AppScope::class)
interface AppGraph : CommonAppGraph {
  val activityProviders: Map<KClass<out Activity>, Provider<Activity>>

  @DependencyGraph.Factory
  interface Factory {
    fun create(@ApplicationContext @Provides context: Context): AppGraph
  }
}
