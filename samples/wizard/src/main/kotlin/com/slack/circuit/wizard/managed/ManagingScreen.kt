package com.slack.circuit.wizard.managed

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.GoToNavEvent
import com.slack.circuit.NavEvent
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.AppScope
import com.slack.circuit.wizard.common.Direction
import com.slack.circuit.wizard.common.NavigationButton
import kotlinx.parcelize.Parcelize

@Parcelize
object ManagingScreen : Screen {
  data class State(val child: ManagedChildScreen, val eventSink: (Event) -> Unit) : CircuitUiState
  sealed interface Event : CircuitUiEvent {
    object Back : Event
    data class ChildEvent(val navEvent: NavEvent) : Event
  }
}

sealed interface ManagedChildScreen : Screen
@CircuitInject(ManagingScreen::class, AppScope::class)
@Composable
fun managingPresenter(navigator: Navigator): ManagingScreen.State {
  val childScreenStack: SnapshotStateList<ManagedChildScreen> =
    remember { mutableStateListOf(ManagedChildScreen01) }
  return ManagingScreen.State(childScreenStack.last()) {
    when (val event = flatten(it)) {
      is ManagingScreen.Event.Back -> {
        if (!childScreenStack.pop()) navigator.pop()
      }
      is GoToNavEvent -> {
        (event.screen as? ManagedChildScreen)
          ?.let { screen -> childScreenStack.add(screen) }
      }
    }
  }
}

private fun SnapshotStateList<ManagedChildScreen>.pop(): Boolean {
  if (size <= 1) return false

  removeLast()
  return true
}

private fun flatten(event: ManagingScreen.Event): CircuitUiEvent =
  when (event) {
    is ManagingScreen.Event.ChildEvent -> event.navEvent
    else -> event
  }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ManagingScreen::class, AppScope::class)
@Composable
fun ManagingUi(state: ManagingScreen.State, modifier: Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = { ManagingTopBar { state.eventSink(ManagingScreen.Event.Back) } }
  ) {padding ->
    CircuitContent(
      screen = state.child,
      modifier = Modifier.padding(padding),
      onNavEvent = { event -> state.eventSink(ManagingScreen.Event.ChildEvent(event)) }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagingTopBar(modifier: Modifier = Modifier, onBack: () -> Unit) {
  CenterAlignedTopAppBar(
    title = { Text("Managed Wizard") },
    modifier = modifier,
    navigationIcon = { NavigationButton(Direction.LEFT, onClick = onBack) }
  )
}

