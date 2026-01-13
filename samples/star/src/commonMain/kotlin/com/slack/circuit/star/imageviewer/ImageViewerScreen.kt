// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.imageviewer.ImageViewerScreen.Event.Close
import com.slack.circuit.star.imageviewer.ImageViewerScreen.Event.NoOp
import com.slack.circuit.star.imageviewer.ImageViewerScreen.State
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@Parcelize
data class ImageViewerScreen(
  val id: Long,
  val url: String,
  val index: Int,
  val placeholderKey: String?,
) : Screen {
  data class State(
    val id: Long,
    val url: String,
    val index: Int,
    val placeholderKey: String?,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object Close : Event

    data object NoOp : Event // Weird but necessary because of the reuse in bottom sheet
  }
}

@AssistedInject
class ImageViewerPresenter(
  @Assisted private val screen: ImageViewerScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<State> {
  @CircuitInject(ImageViewerScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(screen: ImageViewerScreen, navigator: Navigator): ImageViewerPresenter
  }

  @Composable
  override fun present(): State {
    return State(
      id = screen.id,
      url = screen.url,
      index = screen.index,
      placeholderKey = screen.placeholderKey,
    ) { event ->
      when (event) {
        Close -> navigator.pop()
        NoOp -> {}
      }
    }
  }
}

@Composable expect fun ImageViewer(state: State, modifier: Modifier = Modifier)
