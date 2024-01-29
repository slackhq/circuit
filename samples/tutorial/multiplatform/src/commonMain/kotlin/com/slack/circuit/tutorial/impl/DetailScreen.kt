package com.slack.circuit.tutorial.impl

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.slack.circuit.tutorial.common.EmailDetailContent
import com.slack.circuit.tutorial.common.EmailRepository

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
  Column(modifier = modifier.padding(16.dp), verticalArrangement = spacedBy(16.dp)) {
    IconButton(onClick = { state.eventSink(DetailScreen.Event.BackClicked) }) {
      Icon(Icons.Default.ArrowBack, contentDescription = "Back")
    }
    EmailDetailContent(state.email, modifier)
  }
}
