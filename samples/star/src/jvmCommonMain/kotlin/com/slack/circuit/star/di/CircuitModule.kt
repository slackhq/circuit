// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import kotlin.reflect.KClass

@ContributesTo(AppScope::class)
@Module
interface CircuitModule {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun viewFactories(): Set<Ui.Factory>

  @Multibinds fun animatedScreenTransforms(): Map<KClass<Screen>, AnimatedScreenTransform>

  companion object {
    @SingleIn(AppScope::class)
    @Provides
    fun provideCircuit(
      presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>,
      uiFactories: @JvmSuppressWildcards Set<Ui.Factory>,
      animatedScreenTransforms: @JvmSuppressWildcards Map<KClass<Screen>, AnimatedScreenTransform>,
    ): Circuit {
      return Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
        .addAnimatedScreenTransforms(animatedScreenTransforms)
        .build()
    }
  }
}
