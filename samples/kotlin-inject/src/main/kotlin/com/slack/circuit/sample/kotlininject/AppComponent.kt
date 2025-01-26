// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.kotlininject

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.lattice.DependencyGraph
import dev.zacsweers.lattice.Provides
import dev.zacsweers.lattice.SingleIn
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
abstract class AppComponent {
  abstract val kotlinInjectApp: KotlinInjectAppClass
  abstract val presenterFactories: Set<Presenter.Factory>
  abstract val uiFactories: Set<Ui.Factory>

  @Suppress("FunctionOnlyReturningConstant")
  @Provides
  @SingleIn(AppScope::class)
  fun providesString(): String {
    return "kotlin-inject"
  }

  @SingleIn(AppScope::class)
  @Provides
  fun circuit(presenterFactories: Set<Presenter.Factory>, uiFactories: Set<Ui.Factory>): Circuit {
    return Circuit.Builder()
      .addPresenterFactories(presenterFactories)
      .addUiFactories(uiFactories)
      .build()
  }

  abstract val circuit: Circuit
}
