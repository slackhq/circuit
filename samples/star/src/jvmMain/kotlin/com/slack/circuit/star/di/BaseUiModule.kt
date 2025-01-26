// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.PlatformContext
import dev.zacsweers.lattice.ContributesTo
import dev.zacsweers.lattice.Provides
import dev.zacsweers.lattice.SingleIn

@ContributesTo(AppScope::class)
interface BaseUiModule {
  companion object {
    @ApplicationContext
    @SingleIn(AppScope::class)
    @Provides
    fun provideAppContext(): PlatformContext = PlatformContext.INSTANCE
  }
}
