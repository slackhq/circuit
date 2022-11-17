// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds

@ContributesTo(AppScope::class)
@Module
interface BaseUiModule {
  @Multibinds fun provideViewModelProviders(): Map<Class<out ViewModel>, ViewModel>
  @Multibinds fun provideActivityProviders(): Map<Class<out Activity>, Activity>
}
