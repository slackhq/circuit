// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.Assisted
import com.slack.circuit.star.di.AssistedFactory
import com.slack.circuit.star.di.AssistedInject

@Parcelize
data class FiltersScreen(val initialFilters: Filters) : Screen {
  data class State(val initialFilters: Filters, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event {
    data class Save(val filters: Filters) : Event
  }

  @Parcelize data class Result(val filters: Filters) : PopResult
}

class FiltersPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  @Assisted private val screen: FiltersScreen,
) : Presenter<FiltersScreen.State> {
  @Composable
  override fun present(): FiltersScreen.State {
    return FiltersScreen.State(
      initialFilters = screen.initialFilters,
      eventSink = { event ->
        when (event) {
          is FiltersScreen.Event.Save -> {
            navigator.pop(FiltersScreen.Result(event.filters))
          }
        }
      },
    )
  }

  @CircuitInject(FiltersScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator, screen: FiltersScreen): FiltersPresenter
  }
}

@CircuitInject(FiltersScreen::class, AppScope::class)
@Composable
internal fun FilterUi(state: FiltersScreen.State, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier.fillMaxWidth(),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      CenterAlignedTopAppBar(title = { Text("Filters") }, navigationIcon = { BackPressNavIcon() })
    },
  ) { contentPadding ->
    Box(modifier = Modifier.padding(contentPadding)) {
      UpdateFiltersSheet(state.initialFilters, Modifier.padding(16.dp)) { newFilters ->
        state.eventSink(FiltersScreen.Event.Save(newFilters))
      }
    }
  }
}
