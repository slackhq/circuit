// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui

/**
 * A simple interface that exposes [uiFactories] and [presenterFactories]. This can be useful for DI
 * frameworks that expose multibindings for factories.
 */
public interface CircuitComponent {
  public val uiFactories: Set<Ui.Factory>
  public val presenterFactories: Set<Presenter.Factory>
}

/**
 * Returns a new [Circuit.Builder] initialized with this [CircuitComponent.uiFactories] and
 * [CircuitComponent.presenterFactories] added.
 */
public fun CircuitComponent.circuitBuilder(): Circuit.Builder {
  return Circuit.Builder().addPresenterFactories(presenterFactories).addUiFactories(uiFactories)
}
