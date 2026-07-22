// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.di

import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.home.InboxScreen
import com.slack.circuit.sample.inbox.list.InboxListScreen
import com.slack.circuit.serialization.SerializableCircuitSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@ContributesTo(AppScope::class)
interface CircuitProviders {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun uiFactories(): Set<Ui.Factory>

  @SingleIn(AppScope::class)
  @Provides
  fun provideCircuit(
    presenterFactories: Set<Presenter.Factory>,
    uiFactories: Set<Ui.Factory>,
  ): Circuit {
    val circuitSaver = buildCircuitSaver()
    return Circuit.Builder()
      .addPresenterFactories(presenterFactories)
      .addUiFactories(uiFactories)
      .setCircuitSaver(circuitSaver)
      .build()
  }
}

fun buildCircuitSaver(): CircuitSaver =
  SerializableCircuitSaver(
    SavedStateConfiguration {
      serializersModule = SerializersModule {
        // TODO: Replace these manual registrations with Circuit serialization code generation.
        polymorphic(CircuitSaveable::class) {
          subclass(InboxScreen::class)
          subclass(InboxListScreen::class)
          subclass(EmailDetailScreen::class)
        }
      }
    }
  )
