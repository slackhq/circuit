// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

enum class UiSource : Displayable {
  Circuit {
    override fun createUi(): Ui<CounterScreen.State> {
      return ui { state, modifier -> Counter(state, modifier) }
    }

    override val presentationName
      get() = "Circuit"
  },
  View {
    override fun createUi(): Ui<CounterScreen.State> {
      return ui { state, modifier -> CounterViewComposable(state, modifier) }
    }

    override val presentationName
      get() = "View -> Circuit"
  };

  abstract fun createUi(): Ui<CounterScreen.State>

  open override val index: Int
    get() = ordinal

  open override val presentationName: String
    get() = name

  companion object {
    const val LABEL = "UI Source"
  }
}
