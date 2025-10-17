// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberPresenter
import com.slack.circuit.foundation.rememberUi
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { InteropSample() }
  }
}

internal fun buildCircuit(): Circuit {
  return Circuit.Builder()
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
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InteropSample(modifier: Modifier = Modifier) {
  val circuit = remember { buildCircuit() }
  CircuitCompositionLocals(circuit) {
    var selectedPresenterIndex by remember { mutableIntStateOf(0) }
    var selectedUiIndex by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    val circuitScreen =
      remember(selectedUiIndex, selectedPresenterIndex) {
        InteropCounterScreen(
          PresenterSource.entries[selectedPresenterIndex],
          UiSource.entries[selectedUiIndex],
          count,
        )
      }
    val useColumn = calculateWindowSizeClass().widthSizeClass == WindowWidthSizeClass.Compact

    val menus: @Composable () -> Unit = {
      Column(Modifier.padding(16.dp), Arrangement.spacedBy(16.dp)) {
        SourceMenu(
          label = PresenterSource.LABEL,
          selectedIndex = selectedPresenterIndex,
          sourceValues = PresenterSource.entries.toTypedArray(),
          onSelected = { index -> selectedPresenterIndex = index },
          modifier = Modifier.testTag(TestTags.PRESENTER_DROPDOWN),
        )
        SourceMenu(
          label = UiSource.LABEL,
          selectedIndex = selectedUiIndex,
          sourceValues = UiSource.entries.toTypedArray(),
          onSelected = { index -> selectedUiIndex = index },
          modifier = Modifier.testTag(TestTags.UI_DROPDOWN),
        )
      }
    }

    val content = rememberContent(circuit) { count = it }

    Scaffold(
      modifier = modifier.testTag(TestTags.ROOT),
      topBar = { CenterAlignedTopAppBar(title = { Text("Circuit Interop") }) },
    ) { paddingValues ->
      if (useColumn) {
        Column(Modifier.padding(paddingValues), Arrangement.spacedBy(16.dp)) {
          Box(Modifier.weight(1f)) { content(circuitScreen) }
          menus()
        }
      } else {
        Row(
          modifier = Modifier.padding(paddingValues),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          menus()
          Box(Modifier.weight(1f)) { content(circuitScreen) }
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
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedName: String by
    remember(selectedIndex) { mutableStateOf(sourceValues[selectedIndex].presentationName) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier,
  ) {
    TextField(
      readOnly = true,
      value = selectedName,
      label = { Text(label) },
      onValueChange = {},
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      colors = ExposedDropdownMenuDefaults.textFieldColors(),
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
          .testTag(TestTags.currentSourceFor(label)),
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
private fun SourceMenuItem(source: Displayable, selectedIndex: Int, onSelected: (Int) -> Unit) {
  val isSelected = selectedIndex == source.index
  val style =
    if (isSelected) {
      MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
      )
    } else {
      MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  DropdownMenuItem(
    text = {
      Text(
        modifier = Modifier.padding(8.dp).testTag(TestTags.SOURCE),
        text = source.presentationName,
        style = style,
      )
    },
    onClick = { onSelected(source.index) },
  )
}

@Composable
private fun rememberContent(
  circuit: Circuit,
  onCountUpdate: (Int) -> Unit,
): @Composable (Screen) -> Unit {
  return remember {
    movableContentOf { screen: Screen ->
      @Suppress("UNCHECKED_CAST")
      val presenter =
        rememberPresenter(screen = screen, factory = circuit::presenter)
          as Presenter<CounterScreen.State>
      @Suppress("UNCHECKED_CAST")
      val ui = rememberUi(screen = screen, factory = circuit::ui) as Ui<CounterScreen.State>

      // Replicate what CircuitContent does but intercept the state
      // We could also intercept with an event listener or make this a composite presenter,
      // but for the scope of this sample those aren't necessary.
      val state = presenter.present()
      SideEffect { onCountUpdate(state.count) }
      ui.Content(state = state, modifier = Modifier)
    }
  }
}
