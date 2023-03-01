package com.slack.circuit.wizard.composite

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiEvent
import kotlinx.coroutines.delay
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object CompositeChildStep02 : CompositeScreen.ChildScreen {
  @IgnoredOnParcel override val number = 1

  sealed interface State : CompositeScreen.ChildScreen.State {
    @Parcelize
    data class Result(
      val strings: List<String>,
      val selected: Set<String> = emptySet(),
      val eventSink: (CompositeChildStep02.Event) -> Unit
    ) : State
    @Parcelize object Loading : State
  }

  sealed interface Event : CircuitUiEvent {
    data class Selected(val value: String) : Event
    data class Deselected(val value: String) : Event
  }
}

@Composable
fun compositeChildStep02State(eventSink: (CompositeScreen.ChildScreen.Event) -> Unit): CompositeChildStep02.State {
  val selected = remember { mutableStateMapOf<String, Unit>() }
  val data by produceState<List<String>?>(initialValue = null) {
    delay(1000L)
    value = listOf("a","b","c")
  }

  return when (val strings = data) {
    null -> {
      eventSink(CompositeScreen.ChildScreen.Event.Invalid)
      CompositeChildStep02.State.Loading
    }
    else -> CompositeChildStep02.State.Result(strings, selected.keys) { event ->
      when (event) {
        is CompositeChildStep02.Event.Selected -> selected[event.value] = Unit
        is CompositeChildStep02.Event.Deselected -> selected.remove(event.value)
      }
      emit(selected.keys, eventSink)
    }
  }
}

private fun emit(selected: Set<String>, eventSink: (CompositeScreen.ChildScreen.Event) -> Unit) {
  when {
    selected.size < 2 -> eventSink(CompositeScreen.ChildScreen.Event.Invalid)
    else -> {
      eventSink(CompositeScreen.ChildScreen.Event.StringResults(selected))
      eventSink(CompositeScreen.ChildScreen.Event.Valid)
    }
  }
}

@Composable
fun CompositeChildStep02Ui(state: CompositeChildStep02.State, modifier: Modifier = Modifier) {
  when (state) {
    is CompositeChildStep02.State.Loading -> LoadingState(modifier)
    is CompositeChildStep02.State.Result -> ResultState(state, modifier)
  }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Text("Loading....", modifier = modifier)
}

@Composable
private fun ResultState(state: CompositeChildStep02.State.Result, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  Column(modifier = modifier) {
    state.strings.forEach { value ->
      val isSelected = state.selected.contains(value)
      Row {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { selected ->
            if (selected) sink(CompositeChildStep02.Event.Selected(value))
            else sink(CompositeChildStep02.Event.Deselected(value))
          }
        )
        Text(text = value.toString())
      }
    }
  }
}
