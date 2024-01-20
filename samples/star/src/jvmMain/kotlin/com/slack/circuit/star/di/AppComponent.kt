// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import com.slack.circuit.star.data.DataModule
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Component

@MergeComponent(
  scope = AppScope::class,
  modules = [BaseUiModule::class, CircuitModule::class, DataModule::class],
)
@SingleIn(AppScope::class)
interface AppComponent : CommonAppComponent {
  @Component.Factory
  interface Factory {
    fun create(): AppComponent
  }

  companion object {
    fun create(): AppComponent = DaggerAppComponent.factory().create()
  }
}
