// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface CircuitModule {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun viewFactories(): Set<Ui.Factory>

  companion object {
    @SingleIn(AppScope::class)
    @Provides
    fun provideCircuit(
      presenterFactories: Set<Presenter.Factory>,
      uiFactories: Set<Ui.Factory>,
    ): Circuit {
      return Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
        .build()
    }
  }
}
