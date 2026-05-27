// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuitx.navstage.DetailPane
import com.slack.circuitx.navstage.ListPane

@Parcelize
data object ItemListScreen : Screen, ListPane

@Parcelize
data class ItemDetailScreen(val itemId: Int) : Screen, DetailPane

data class ItemListState(val items: List<String>, val eventSink: (ItemListEvent) -> Unit) :
  CircuitUiState

sealed interface ItemListEvent : CircuitUiEvent {
  data class ItemClicked(val index: Int) : ItemListEvent
}

data class ItemDetailState(val title: String, val body: String) : CircuitUiState

class ItemListPresenter(private val navigator: Navigator) : Presenter<ItemListState> {
  @Composable
  override fun present(): ItemListState {
    val items = (1..20).map { "Item $it" }
    return ItemListState(items) { event ->
      when (event) {
        is ItemListEvent.ItemClicked -> navigator.goTo(ItemDetailScreen(event.index))
      }
    }
  }
}

class ItemDetailPresenter(private val screen: ItemDetailScreen) : Presenter<ItemDetailState> {
  @Composable
  override fun present(): ItemDetailState {
    return ItemDetailState(
      title = "Item ${screen.itemId + 1}",
      body = "Detail content for item ${screen.itemId + 1}. " +
        "This demonstrates the list-detail nav stage pattern.",
    )
  }
}

@Composable
fun ItemListUi(state: ItemListState, modifier: Modifier = Modifier) {
  LazyColumn(modifier.fillMaxSize()) {
    items(state.items.indices.toList()) { index ->
      Text(
        text = state.items[index],
        style = MaterialTheme.typography.bodyLarge,
        modifier =
          Modifier.fillMaxWidth()
            .clickable { state.eventSink(ItemListEvent.ItemClicked(index)) }
            .padding(16.dp),
      )
      HorizontalDivider()
    }
  }
}

@Composable
fun ItemDetailUi(state: ItemDetailState, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxSize().padding(16.dp)) {
    Text(text = state.title, style = MaterialTheme.typography.headlineMedium)
    Text(
      text = state.body,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(top = 16.dp),
    )
  }
}

object ListDetailScreenFactory : Presenter.Factory, Ui.Factory {
  override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? {
    return when (screen) {
      is ItemListScreen -> ItemListPresenter(navigator)
      is ItemDetailScreen -> ItemDetailPresenter(screen)
      else -> null
    }
  }

  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      is ItemListScreen -> ui<ItemListState> { state, modifier -> ItemListUi(state, modifier) }
      is ItemDetailScreen -> ui<ItemDetailState> { state, modifier ->
        ItemDetailUi(state, modifier)
      }
      else -> null
    }
  }
}
