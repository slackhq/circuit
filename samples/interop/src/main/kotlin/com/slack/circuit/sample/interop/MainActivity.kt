// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import kotlinx.parcelize.Parcelize

class MainActivity : AppCompatActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuit =
      Circuit.Builder()
        .addPresenterFactory { screen, _, context ->
          when (screen) {
            is InteropCounterScreen -> screen.presenterSource.createPresenter(screen, context)
            else -> null
          }
        }
        .addUiFactory { screen, _ ->
          when (screen) {
            is InteropCounterScreen -> screen.uiSource.createUi()
            else -> null
          }
        }
        .build()

    setContent {
      CircuitCompositionLocals(circuit) {
        var selectedPresenterIndex by remember { mutableStateOf(0) }
        var selectedUiIndex by remember { mutableStateOf(0) }
        val circuitScreen =
          remember(selectedUiIndex, selectedPresenterIndex) {
            InteropCounterScreen(
              PresenterSource.values()[selectedPresenterIndex],
              UiSource.values()[selectedUiIndex]
            )
          }
        val useColumn =
          calculateWindowSizeClass(this).widthSizeClass == WindowWidthSizeClass.Compact

        val menus: @Composable () -> Unit = {
          Column(Modifier.padding(16.dp), Arrangement.spacedBy(16.dp)) {
            SourceMenu("Presenter Source", selectedPresenterIndex, PresenterSource.values()) { index
              ->
              selectedPresenterIndex = index
            }
            SourceMenu("UI Source", selectedUiIndex, UiSource.values()) { index ->
              selectedUiIndex = index
            }
          }
        }

        val content = remember {
          movableContentOf {
            // TODO this is necessary because the CircuitContent caches the Ui and Presenter, which
            //  doesn't play well swapping out the Ui and Presenter sources. Might be nice to make
            //  them live enough to support this, but also sort of orthogonal to the point of this
            //  sample.
            key(circuitScreen) { CircuitContent(screen = circuitScreen) }
          }
        }

        Scaffold { paddingValues ->
          if (useColumn) {
            Column(Modifier.padding(paddingValues), Arrangement.spacedBy(16.dp)) {
              Box(Modifier.weight(1f)) { content() }
              menus()
            }
          } else {
            Row(
              modifier = Modifier.padding(paddingValues),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              menus()
              Box(Modifier.weight(1f)) { content() }
            }
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
  sourceValues: Array<out Displayable>,
  onSelected: (Int) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedName: String by
    remember(selectedIndex) { mutableStateOf(sourceValues[selectedIndex].presentationName) }
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
        SourceMenuItem(source, selectedIndex) {
          expanded = false
          onSelected(it)
        }
      }
    }
  }
}

@Composable
private fun SourceMenuItem(
  source: Displayable,
  selectedIndex: Int,
  onSelected: (Int) -> Unit,
) {
  val isSelected = selectedIndex == source.index
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
        text = source.presentationName,
        style = style,
      )
    },
    onClick = { onSelected(source.index) },
  )
}

@Parcelize
private data class InteropCounterScreen(
  val presenterSource: PresenterSource,
  val uiSource: UiSource
) : CounterScreen, Parcelable

@Stable
private interface Displayable {
  val index: Int
  val presentationName: String
}

@Suppress("UNCHECKED_CAST")
private enum class PresenterSource : Displayable {
  Circuit {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return CounterPresenterFactory().create(screen, Navigator.NoOp, context)
        as Presenter<CounterScreen.State>
    }

    override val presentationName
      get() = "Circuit"
  },
  Flow {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return FlowCounterPresenter().asCircuitPresenter()
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
      return RxCounterPresenter().asCircuitPresenter()
    }

    override val presentationName
      get() = "RxJava -> Circuit"
  },
  Simple {
    override fun createPresenter(
      screen: InteropCounterScreen,
      context: CircuitContext,
    ): Presenter<CounterScreen.State> {
      return SimpleCounterPresenter().asCircuitPresenter()
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
}

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
}
