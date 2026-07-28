// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.serialization.SerializableCircuitSaver
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun buildCircuitForTabs(
  tabs: Collection<TabScreen>,
  circuitSaver: CircuitSaver = buildCircuitSaver(),
): Circuit {
  return Circuit.Builder()
    .apply {
      for (tab in tabs) {
        addPresenterFactory(TabPresenter.Factory(tab::class))
        addUiFactory(TabUiFactory(tab::class))
      }
    }
    .setAnimatedNavDecoratorFactory(GestureNavigationDecorationFactory())
    .setCircuitSaver(circuitSaver)
    .build()
}

/**
 * A [CircuitSaver] that persists this sample's nav stack with kotlinx-serialization instead of
 * relying on Parcelable, so saving works the same on every platform.
 */
fun buildCircuitSaver(): CircuitSaver =
  SerializableCircuitSaver(
    SavedStateConfiguration {
      serializersModule = SerializersModule {
        // TODO: Replace these manual registrations with Circuit serialization code generation.
        polymorphic(CircuitSaveable::class) {
          subclass(TabScreen.Root::class)
          subclass(TabScreen.Screen1::class)
          subclass(TabScreen.Screen2::class)
          subclass(TabScreen.Screen3::class)
          subclass(InfoScreen::class)
        }
      }
    }
  )
