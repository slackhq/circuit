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
package com.slack.circuit.sample.interop

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitContent
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.CounterState
import com.slack.circuit.ui
import kotlinx.parcelize.Parcelize

class MainActivity : AppCompatActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuitConfig =
      CircuitConfig.Builder()
        .addPresenterFactory { screen, _, circuitConfig ->
          when (screen) {
            is InteropCounterScreen -> screen.presenterSource.createPresenter(screen, circuitConfig)
            else -> null
          }
        }
        .addUiFactory { screen, _ ->
          when (screen) {
            is InteropCounterScreen -> ScreenUi(screen.uiSource.createUi())
            else -> null
          }
        }
        .build()

    setContent {
      CircuitCompositionLocals(circuitConfig) {
        var selectedPresenterIndex by rememberSaveable { mutableStateOf(0) }
        var selectedUiIndex by rememberSaveable { mutableStateOf(0) }
        val circuitScreen =
          remember(selectedUiIndex, selectedPresenterIndex) {
            InteropCounterScreen(
              PresenterSource.values()[selectedPresenterIndex],
              UiSource.values()[selectedUiIndex]
            )
          }
        Scaffold(
          modifier = Modifier,
          bottomBar = {
            Column(Modifier.padding(16.dp).fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
              SourceMenu("Presenter Source", selectedPresenterIndex, PresenterSource.values()) {
                index ->
                selectedPresenterIndex = index
              }
              SourceMenu("UI Source", selectedUiIndex, UiSource.values()) { index ->
                selectedUiIndex = index
              }
            }
          },
        ) { paddingValues ->
          Box(Modifier.padding(paddingValues)) {
            // TODO this is necessary because the CircuitContent caches the Ui and Presenter, which
            //  doesn't play well swapping out the Ui and Presenter sources. Might be nice to make
            //  them live enough to support this, but also sort of orthogonal to the point of this
            //  sample.
            key(circuitScreen) { CircuitContent(screen = circuitScreen) }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceMenu(
  label: String,
  selectedIndex: Int,
  sourceValues: Array<out Enum<*>>,
  onSelected: (Int) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedName: String by
    remember(selectedIndex) { mutableStateOf(sourceValues[selectedIndex].name) }
  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    TextField(
      readOnly = true,
      value = selectedName,
      label = { Text(label) },
      onValueChange = {},
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      colors = ExposedDropdownMenuDefaults.textFieldColors(),
      modifier = Modifier.menuAnchor(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      for (source in sourceValues) {
        SourceMenuItem(source, selectedIndex, onSelected)
      }
    }
  }
}

@Composable
private fun SourceMenuItem(
  source: Enum<*>,
  selectedIndex: Int,
  onSelected: (Int) -> Unit,
) {
  val isSelected = selectedIndex == source.ordinal
  val style =
    if (isSelected) {
      MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary
      )
    } else {
      MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  DropdownMenuItem(
    text = {
      Text(
        modifier = Modifier.padding(8.dp),
        text = source.name,
        style = style,
      )
    },
    onClick = { onSelected(source.ordinal) },
  )
}

@Parcelize
private data class InteropCounterScreen(
  val presenterSource: PresenterSource,
  val uiSource: UiSource
) : CounterScreen, Parcelable

@Suppress("UNCHECKED_CAST")
private enum class PresenterSource {
  Circuit {
    override fun createPresenter(
      screen: InteropCounterScreen,
      config: CircuitConfig
    ): Presenter<CounterState> {
      return CounterPresenterFactory().create(screen, Navigator.NoOp, config)
        as Presenter<CounterState>
    }
  },
  Flow {
    override fun createPresenter(
      screen: InteropCounterScreen,
      config: CircuitConfig
    ): Presenter<CounterState> {
      return FlowCounterPresenter().asCircuitPresenter()
    }
  },
  RxJava {
    override fun createPresenter(
      screen: InteropCounterScreen,
      config: CircuitConfig
    ): Presenter<CounterState> {
      return RxCounterPresenter().asCircuitPresenter()
    }
  },
  Simple {
    override fun createPresenter(
      screen: InteropCounterScreen,
      config: CircuitConfig
    ): Presenter<CounterState> {
      return SimpleCounterPresenter().asCircuitPresenter()
    }
  };

  abstract fun createPresenter(
    screen: InteropCounterScreen,
    config: CircuitConfig
  ): Presenter<CounterState>
}

enum class UiSource {
  Circuit {
    override fun createUi(): Ui<CounterState> {
      return ui { state -> Counter(state) }
    }
  },
  View {
    override fun createUi(): Ui<CounterState> {
      return ui { state -> CounterViewComposable(state) }
    }
  };

  abstract fun createUi(): Ui<CounterState>
}
