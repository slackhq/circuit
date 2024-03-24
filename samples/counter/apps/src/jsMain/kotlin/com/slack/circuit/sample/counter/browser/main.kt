// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION_ERROR")

package com.slack.circuit.sample.counter.browser

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.DefaultViewConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.CounterUiFactory
import org.jetbrains.compose.web.renderComposable

data object BrowserCounterScreen : CounterScreen

fun main() {
  val circuit: Circuit =
    Circuit.Builder()
      .addPresenterFactory(CounterPresenterFactory())
      .addUiFactory(CounterUiFactory())
      .build()
  // https://github.com/JetBrains/compose-multiplatform/issues/2186
  val fontFamilyResolver = createFontFamilyResolver()
  renderComposable(rootElementId = "root") {
    CompositionLocalProvider(
      LocalDensity provides Density(1.0f),
      LocalLayoutDirection provides LayoutDirection.Ltr,
      LocalViewConfiguration provides DefaultViewConfiguration(Density(1.0f)),
      LocalInputModeManager provides InputModeManagerObject,
      LocalFontFamilyResolver provides fontFamilyResolver,
    ) {
      CircuitCompositionLocals(circuit) { CircuitContent(BrowserCounterScreen) }
    }
  }
}

private object InputModeManagerObject : InputModeManager {
  override val inputMode = InputMode.Keyboard

  @ExperimentalComposeUiApi override fun requestInputMode(inputMode: InputMode) = false
}
