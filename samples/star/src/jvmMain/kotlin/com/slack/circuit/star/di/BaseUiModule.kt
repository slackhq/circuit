// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface BaseUiModule {
  companion object {
    @ApplicationContext
    @SingleIn(AppScope::class)
    @Provides
    fun provideAppContext(): PlatformContext = PlatformContext.INSTANCE
  }
}
