/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.sample.counter.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitContent
import com.slack.circuit.backstack.BackStackRecordLocalProviderViewModel
import com.slack.circuit.retained.Continuity
import com.slack.circuit.sample.counter.CounterPresenterFactory

class CounterActivity : AppCompatActivity() {

  private val viewModelProviderFactory: ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
          Continuity::class.java -> Continuity()
          BackStackRecordLocalProviderViewModel::class.java ->
            BackStackRecordLocalProviderViewModel()
          else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
          as T
      }
    }
  private val circuitConfig: CircuitConfig =
    CircuitConfig.Builder()
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
        CircuitCompositionLocals(circuitConfig) { CircuitContent(AndroidCounterScreen) }
      }
    }
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return viewModelProviderFactory
  }
}
