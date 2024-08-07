// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import android.content.Context
import com.slack.circuit.star.data.DataModule
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.BindsInstance
import javax.inject.Provider

@MergeComponent(
  scope = AppScope::class,
  modules = [BaseUiModule::class, CircuitModule::class, DataModule::class],
)
@SingleIn(AppScope::class)
interface AppComponent : CommonAppComponent {
  val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

  @MergeComponent.Factory
  interface Factory {
    fun create(@ApplicationContext @BindsInstance context: Context): AppComponent
  }

  companion object {
    fun create(context: Context): AppComponent = DaggerAppComponent.factory().create(context)
  }
}
