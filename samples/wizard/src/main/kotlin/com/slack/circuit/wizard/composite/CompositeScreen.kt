package com.slack.circuit.wizard.composite

import android.os.Parcelable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.AppScope
import com.slack.circuit.wizard.common.Direction
import com.slack.circuit.wizard.common.NavigationButton
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize

@Parcelize
object CompositeScreen : Screen {
  data class State(
    val child: ChildScreen.State,
    val isNextEnabled: Boolean,
    val isNextVisible: Boolean,
    val eventSink: (Event) -> Unit
  ): CircuitUiState

  sealed interface Event : CircuitUiEvent {
    object Back : Event
    object Forward : Event
  }

  sealed interface ChildScreen : Parcelable {
    val number: Int

    sealed interface Event : CircuitUiEvent {
      object Invalid : Event
      object Valid : Event
      data class IntResults(val ints: Set<Int>) : Event
      data class StringResults(val strings: Set<String>) : Event
    }

    interface State : Parcelable
  }
}

private val steps = listOf(
  CompositeChildStep01,
  CompositeChildStep02,
  CompositeChildStep03
)

@Parcelize
private data class CompositeState(
  val ints: Set<Int> = emptySet(),
  val strings: Set<String> = emptySet()
) : Parcelable

private fun CompositeState.toChildState(): CompositeChildStep03.Input =
  CompositeChildStep03.Input(ints = ints, strings = strings)

class CompositeScreenPresenter @AssistedInject constructor(@Assisted private val navigator: Navigator) : Presenter<CompositeScreen.State> {
  @Composable
  override fun present(): CompositeScreen.State {
    val step = rememberSaveable { mutableStateOf<CompositeScreen.ChildScreen>(CompositeChildStep01) }
    var state by rememberSaveable { mutableStateOf(CompositeState()) }
    var isNextEnabled by remember { mutableStateOf(false) }

    val childState = step.getState(state) { event ->
      when (event) {
        is CompositeScreen.ChildScreen.Event.Valid -> isNextEnabled = true
        is CompositeScreen.ChildScreen.Event.Invalid -> isNextEnabled = false
        is CompositeScreen.ChildScreen.Event.IntResults -> state = state.copy(ints = event.ints)
        is CompositeScreen.ChildScreen.Event.StringResults -> state = state.copy(strings = event.strings)
      }
    }

    return CompositeScreen.State(childState, isNextEnabled, step.value.number < 2) { event ->
      when (event) {
        is CompositeScreen.Event.Back -> step.backOrPop()
        is CompositeScreen.Event.Forward -> step.forwardOrSubmit()
      }
    }
  }

  private fun MutableState<CompositeScreen.ChildScreen>.backOrPop() {
    val newStep = steps.elementAtOrNull(value.number - 1)
    if (newStep != null) {
      value = newStep
      return
    }

    navigator.pop()
  }

  private fun MutableState<CompositeScreen.ChildScreen>.forwardOrSubmit() {
    val newStep = steps.elementAtOrNull(value.number + 1)
    if (newStep != null) {
      value = newStep
      return
    }

    TODO("submit")
  }

  @CircuitInject(CompositeScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): CompositeScreenPresenter
  }
}

@Composable
private fun MutableState<CompositeScreen.ChildScreen>.getState(
  state: CompositeState,
  eventSink: (CompositeScreen.ChildScreen.Event) -> Unit
): CompositeScreen.ChildScreen.State =
  when (value) {
    CompositeChildStep01 -> compositeChildStep01State(eventSink)
    CompositeChildStep02 -> compositeChildStep02State(eventSink)
    CompositeChildStep03 -> compositeChildStep03State(state.toChildState())
  }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(CompositeScreen::class, AppScope::class)
@Composable
fun CompositeScreenUi(state: CompositeScreen.State, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("TODO") },
        modifier = modifier,
        navigationIcon = { NavigationButton(Direction.LEFT) { sink(CompositeScreen.Event.Back) } },
        actions = {
          if (state.isNextVisible) NavigationButton(Direction.RIGHT, enabled = state.isNextEnabled)  { sink(CompositeScreen.Event.Forward) }
        }
      )
    }
  ) { padding ->
    when (state.child) {
      is CompositeChildStep01.State -> CompositeChildStep01Ui(
        state = state.child,
        modifier = Modifier.padding(padding)
      )
      is CompositeChildStep02.State -> CompositeChildStep02Ui(
        state = state.child,
        modifier = Modifier.padding(padding)
      )
      is CompositeChildStep03.State -> CompositeChildStep03Ui(
        state = state.child,
        modifier = Modifier.padding(padding)
      )
    }
  }
}
