// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial.sharedelements

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.tutorial.common.Email
import com.slack.circuit.tutorial.common.EmailRepository
import com.slack.circuit.tutorial.common.sharedelements.EmailDetailContent

data class DetailScreen(val emailId: String) : Screen {
  data class State(val email: Email, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object BackClicked : Event
  }
}

class DetailPresenter(
  private val screen: DetailScreen,
  private val navigator: Navigator,
  private val emailRepository: EmailRepository,
) : Presenter<DetailScreen.State> {
  @Composable
  override fun present(): DetailScreen.State {
    val email = emailRepository.getEmail(screen.emailId)
    return DetailScreen.State(email) { event ->
      when (event) {
        DetailScreen.Event.BackClicked -> navigator.pop()
      }
    }
  }

  class Factory(private val emailRepository: EmailRepository) : Presenter.Factory {
    override fun create(
      screen: Screen,
      navigator: Navigator,
      context: CircuitContext,
    ): Presenter<*>? {
      return when (screen) {
        is DetailScreen -> return DetailPresenter(screen, navigator, emailRepository)
        else -> null
      }
    }
  }
}

@Composable
fun EmailDetail(state: DetailScreen.State, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(state.email.subject) },
        navigationIcon = {
          IconButton(onClick = { state.eventSink(DetailScreen.Event.BackClicked) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding), verticalArrangement = spacedBy(16.dp)) {
      EmailDetailContent(state.email)
    }
  }
}
