// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import kotlin.reflect.KClass

@ContributesTo(AppScope::class)
interface BaseUiModule {
  @Multibinds fun provideActivityProviders(): Map<KClass<out Activity>, Activity>
}
