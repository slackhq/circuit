// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.star.animation.HomeAnimatedScreenTransform
import com.slack.circuit.star.animation.PetDetailAnimatedScreenTransform
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.petdetail.PetDetailScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.MapKey
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass

@OptIn(
  ExperimentalCircuitApi::class // For AnimatedScreenTransform
)
@ContributesTo(AppScope::class)
interface CircuitProviders {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun viewFactories(): Set<Ui.Factory>

  @Multibinds(allowEmpty = true)
  fun animatedScreenTransforms(): Map<KClass<out Screen>, AnimatedScreenTransform>

  @SingleIn(AppScope::class)
  @Provides
  fun provideCircuit(
    presenterFactories: Set<Presenter.Factory>,
    uiFactories: Set<Ui.Factory>,
    animatedScreenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
  ): Circuit {
    return Circuit.Builder()
      .addPresenterFactories(presenterFactories)
      .addUiFactories(uiFactories)
      .addAnimatedScreenTransforms(animatedScreenTransforms)
      .build()
  }

  @MapKey
  annotation class ScreenTransformKey(val screen: KClass<out Screen>)

  @Provides
  @IntoMap
  @ScreenTransformKey(HomeScreen::class)
  fun provideHomeTransform(): AnimatedScreenTransform = HomeAnimatedScreenTransform

  @Provides
  @IntoMap
  @ScreenTransformKey(PetDetailScreen::class)
  fun providePetDetailTransform(): AnimatedScreenTransform = PetDetailAnimatedScreenTransform
}
