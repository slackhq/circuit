// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

@Parcelize data class DetailScreen(val primary: TabScreen) : SecondaryScreen

data class DetailState(
  val label: String,
  val description: String,
  val eventSink: (DetailEvent) -> Unit,
) : CircuitUiState

sealed interface DetailEvent : CircuitUiEvent {
  data object Close : DetailEvent
}

@Composable
fun DetailUi(state: DetailState, modifier: Modifier = Modifier) {
  Card(modifier = modifier.padding(8.dp).fillMaxSize()) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = state.label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 16.dp).weight(1f),
      )
      Icon(
        imageVector = Icons.Filled.Close,
        contentDescription = "Close",
        tint = MaterialTheme.colorScheme.onSurface,
        modifier =
          Modifier.padding(top = 8.dp)
            .clickable(
              interactionSource = null,
              indication = ripple(bounded = false, radius = 24.dp),
            ) {
              state.eventSink(DetailEvent.Close)
            }
            .padding(8.dp),
      )
    }
    Card(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      colors = CardDefaults.outlinedCardColors(),
    ) {
      Text(
        text = state.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
          Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
      )
    }
  }
}

class DetailPresenter(private val screen: DetailScreen, private val navigator: Navigator) :
  Presenter<DetailState> {
  @Composable
  override fun present(): DetailState {
    return DetailState(label = "Details for ${screen.primary.label}", description = LOREM_IPSUM) {
      event ->
      when (event) {
        DetailEvent.Close -> navigator.pop()
      }
    }
  }

  object Factory : Presenter.Factory {
    override fun create(
      screen: Screen,
      navigator: Navigator,
      context: CircuitContext,
    ): Presenter<*>? {
      return if (screen is DetailScreen) {
        DetailPresenter(screen, navigator)
      } else {
        null
      }
    }
  }
}

object DetailUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return if (screen is DetailScreen) {
      ui<DetailState> { state, modifier -> DetailUi(state, modifier) }
    } else {
      null
    }
  }
}

private val LOREM_IPSUM =
  """

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi tincidunt viverra ex, vel iaculis tellus lacinia lobortis. Nullam nisl nisi, semper et venenatis sed, volutpat porta ipsum. Pellentesque molestie sapien ac convallis bibendum. Quisque laoreet sollicitudin dapibus. Nunc ut ultrices eros. Phasellus ornare scelerisque diam, vitae scelerisque justo venenatis ut. Maecenas gravida fringilla lectus sed lobortis. Proin eu lectus scelerisque, egestas sem nec, posuere risus.

Sed sit amet vestibulum erat. Donec congue ullamcorper accumsan. Vestibulum nec neque a mi interdum consectetur. Curabitur dictum leo sit amet elit tristique, non sollicitudin mi molestie. Duis euismod nisi a ultricies congue. Aliquam massa est, pellentesque sed convallis ac, sodales quis leo. Fusce sodales velit quis nunc tincidunt molestie.

Morbi tincidunt aliquet velit, at feugiat sem dictum non. Phasellus cursus rhoncus lorem. Nunc quis neque mi. Praesent luctus, diam nec luctus efficitur, nibh risus tempus elit, ac blandit quam massa in justo. Nunc tincidunt cursus dui. Cras euismod purus nisi. Duis ac orci sapien. Nam urna neque, aliquet et nibh scelerisque, rutrum consectetur dui. Nunc nec gravida nisl, eu euismod felis. Aliquam faucibus leo vitae erat posuere, non fermentum sem lacinia. Ut lobortis, velit quis malesuada molestie, est justo pharetra neque, at venenatis mi lacus quis nisi. Ut dictum sollicitudin odio quis placerat.

Donec ac sapien vel lectus pretium fringilla. Donec tempor suscipit sapien eu venenatis. Etiam aliquam a nibh maximus suscipit. Quisque facilisis metus in vulputate pharetra. Fusce dictum condimentum quam mattis ullamcorper. Sed accumsan est sit amet tortor porta, et pulvinar diam semper. Aenean congue enim eget pulvinar mollis. Mauris sed auctor sem. Suspendisse potenti. Etiam lacinia tempus purus vel imperdiet. Integer ante massa, mollis vitae eleifend id, egestas vitae nulla. Quisque urna nisi, sagittis at elementum finibus, venenatis nec risus. Sed suscipit diam nulla. Cras vel lectus ligula.

"""
    .trim()
