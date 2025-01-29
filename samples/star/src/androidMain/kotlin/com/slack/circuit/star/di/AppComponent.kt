// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import android.content.Context
import dev.zacsweers.metro.BindsInstance
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.SingleIn

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
