// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import coil3.PlatformContext
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides

@ContributesTo(AppScope::class)
@Module
object BaseUiModule {
  @ApplicationContext
  @SingleIn(AppScope::class)
  @Provides
  fun provideAppContext(): PlatformContext = PlatformContext.INSTANCE
}
