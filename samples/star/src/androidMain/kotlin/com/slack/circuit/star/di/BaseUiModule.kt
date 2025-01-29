// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(AppScope::class)
interface BaseUiModule {
  @Multibinds fun provideActivityProviders(): Map<Class<out Activity>, Activity>
}
