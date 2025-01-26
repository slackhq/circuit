// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.benchmark

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.benchmark.ListBenchmarksScreen.State
import com.slack.circuit.star.common.Platform
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.parcel.CommonParcelize
import dev.zacsweers.lattice.Inject
import dev.zacsweers.lattice.Assisted
import dev.zacsweers.lattice.AssistedFactory

@CommonParcelize
data class ListBenchmarksScreen(val useNestedContent: Boolean) : Screen {
  data class State(val useNestedContent: Boolean) : CircuitUiState
}

@Inject
class ListBenchmarksPresenter(@Assisted private val screen: ListBenchmarksScreen) : Presenter<State> {
  @CircuitInject(ListBenchmarksScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ListBenchmarksScreen): ListBenchmarksPresenter
  }

  @Composable override fun present() = State(screen.useNestedContent)
}

private const val ITEM_COUNT = 100

@CircuitInject(ListBenchmarksScreen::class, AppScope::class)
@Composable
fun ListBenchmarks(state: State, modifier: Modifier = Modifier) {
  var contentComposed by rememberRetained { mutableStateOf(false) }
  if (state.useNestedContent) {
    LazyColumn(modifier.systemBarsPadding()) {
      items(ITEM_COUNT) { index ->
        val itemScreen = remember { ListBenchmarksItemScreen(index) }
        CircuitContent(itemScreen)
      }
    }
  } else {
    LazyColumn(modifier.systemBarsPadding()) {
      items(ITEM_COUNT) { index -> ListBenchmarksItem(index) }
    }
  }
  contentComposed = true
  Platform.ReportDrawnWhen { contentComposed }
}

@CommonParcelize
data class ListBenchmarksItemScreen(val index: Int) : Screen {
  data class State(val index: Int) : CircuitUiState
}

@Inject
class IndexMultiplier {
  fun multiply(index: Int) = index * 1
}

@Inject
class ListBenchmarksItemPresenter(
  @Assisted private val screen: ListBenchmarksItemScreen,
  // Simulate injecting something that accumulates instances
  private val indexMultiplier: IndexMultiplier,
) : Presenter<ListBenchmarksItemScreen.State> {
  @CircuitInject(ListBenchmarksItemScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ListBenchmarksItemScreen): ListBenchmarksItemPresenter
  }

  @Composable
  override fun present() = ListBenchmarksItemScreen.State(indexMultiplier.multiply(screen.index))
}

@CircuitInject(ListBenchmarksItemScreen::class, AppScope::class)
@Composable
fun ListBenchmarksItem(state: ListBenchmarksItemScreen.State, modifier: Modifier = Modifier) {
  ListBenchmarksItem(state.index, modifier)
}

@Composable
fun ListBenchmarksItem(index: Int, modifier: Modifier = Modifier) {
  ListItem(
    headlineContent = { Text("Item $index") },
    leadingContent = {
      Text(
        modifier =
          Modifier.padding(16.dp).drawBehind {
            drawCircle(color = Color.Blue, radius = this.size.maxDimension)
          },
        color = Color.White,
        text = index.toString(),
      )
    },
    modifier = modifier,
  )
}

// @Preview
// @Composable
// fun ListBenchmarksItemPreview() {
//  Box { ListBenchmarksItem(ListBenchmarksItemScreen.State(0)) }
// }
