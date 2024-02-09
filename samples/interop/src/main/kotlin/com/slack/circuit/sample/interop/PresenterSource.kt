// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.presenter.Presenter

enum class PresenterSource : Displayable {
  Circuit {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return CircuitCounterPresenter(screen.count)
    }

    override val presentationName
      get() = "Circuit"
  },
  Flow {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return FlowCounterPresenter(screen.count).asCircuitPresenter()
    }

    override val presentationName
      get() = "Flow -> Circuit"
  },
  CircuitToFlow {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      // Two layers of interop!
      return Flow.createPresenter(screen, context).asFlowPresenter().asCircuitPresenter()
    }

    override val presentationName
      get() = "Circuit -> Flow -> Circuit"
  },
  RxJava {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return RxCounterPresenter(screen.count).asCircuitPresenter()
    }

    override val presentationName
      get() = "RxJava -> Circuit"
  },
  Simple {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return SimpleCounterPresenter(screen.count).asCircuitPresenter()
    }

    override val presentationName
      get() = "Simple -> Circuit"
  };

  abstract fun createPresenter(
    screen: InteropCounterScreen,
    context: CircuitContext,
  ): Presenter<CounterScreen.State>

  open override val index: Int
    get() = ordinal

  open override val presentationName: String
    get() = name

  companion object {
    const val LABEL = "Presenter Source"
  }
}
