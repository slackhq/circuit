// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.sample.counter.CounterPresenterFactory

class CounterActivity : AppCompatActivity() {

  private val circuit: Circuit =
    Circuit.Builder()
      .addPresenterFactory(CounterPresenterFactory())
      .addUiFactory(CounterUiFactory())
      .build()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val context = LocalContext.current
      val colorScheme =
        if (isSystemInDarkTheme()) {
          dynamicDarkColorScheme(context)
        } else {
          dynamicLightColorScheme(context)
        }
      val systemUiController = rememberSystemUiController()
      systemUiController.setSystemBarsColor(color = colorScheme.primaryContainer)
      MaterialTheme(colorScheme = colorScheme) {
        CircuitCompositionLocals(circuit) { CircuitContent(AndroidCounterScreen) }
      }
    }
  }
}
