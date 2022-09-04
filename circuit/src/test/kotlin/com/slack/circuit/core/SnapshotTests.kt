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
package com.slack.circuit.core

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.slack.circuit.Circuit
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitProvider
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.Ui
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

class SnapshotTests {
  @get:Rule
  val paparazzi =
    Paparazzi(
      deviceConfig = PIXEL_5,
      theme = "android:Theme.Material.Light.NoActionBar"
      // ...see docs for more options
      )

  @Test
  fun simpleCircuitContent() {
    val state = mutableStateOf("State")
    val presenter = StringPresenter(state)
    val ui = StringUi()
    val circuit =
      Circuit.Builder()
        .addPresenterFactory { _, _ -> presenter }
        .addUiFactory { ScreenView(ui) }
        .build()
    paparazzi.snapshot { CircuitProvider(circuit) { CircuitContent(TestScreen) } }
  }
}

@Parcelize private object TestScreen : Screen

private class StringPresenter(val state: State<String>) : Presenter<String, String> {
  @Composable
  override fun present(events: Flow<String>): String {
    return state.value
  }
}

private class StringUi : Ui<String, String> {

  @Composable
  override fun Render(state: String, events: (String) -> Unit) {
    Text(text = state)
  }
}
