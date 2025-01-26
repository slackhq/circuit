// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import android.content.Context
import dev.zacsweers.lattice.BindsInstance
import dev.zacsweers.lattice.DependencyGraph
import dev.zacsweers.lattice.Provider
import dev.zacsweers.lattice.SingleIn

@DependencyGraph(
  scope = AppScope::class,
)
@SingleIn(AppScope::class)
interface AppComponent : CommonAppComponent {
  val activityProviders: Map<Class<out Activity>, Provider<Activity>>

  @DependencyGraph.Factory
  interface Factory {
    fun create(@ApplicationContext @BindsInstance context: Context): AppComponent
  }
}
