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
object CompositeChildStep01 : CompositeScreen.ChildScreen {
  @IgnoredOnParcel override val number = 0

  sealed interface State : CompositeScreen.ChildScreen.State {
    @Parcelize
    data class Result(
      val ints: List<Int>,
      val selected: Set<Int> = emptySet(),
      val eventSink: (Event) -> Unit
    ) : State
    @Parcelize object Loading : State
  }

  sealed interface Event : CircuitUiEvent {
    data class Selected(val value: Int) : Event
    data class Deselected(val value: Int) : Event
  }
}

@Composable
fun compositeChildStep01State(eventSink: (CompositeScreen.ChildScreen.Event) -> Unit): CompositeChildStep01.State {
  val selected = remember { mutableStateMapOf<Int, Unit>() }
  val data by produceState<List<Int>?>(initialValue = null) {
    delay(1000L)
    value = listOf(0,1,2)
  }

  return when (val ints = data) {
    null -> {
      eventSink(CompositeScreen.ChildScreen.Event.Invalid)
      CompositeChildStep01.State.Loading
    }
    else -> CompositeChildStep01.State.Result(ints, selected.keys) { event ->
      when (event) {
        is CompositeChildStep01.Event.Selected -> selected[event.value] = Unit
        is CompositeChildStep01.Event.Deselected -> selected.remove(event.value)
      }
      emit(selected.keys, eventSink)
    }
  }
}

private fun emit(selected: Set<Int>, eventSink: (CompositeScreen.ChildScreen.Event) -> Unit) {
  when {
    selected.isEmpty() -> eventSink(CompositeScreen.ChildScreen.Event.Invalid)
    else -> {
      eventSink(CompositeScreen.ChildScreen.Event.IntResults(selected))
      eventSink(CompositeScreen.ChildScreen.Event.Valid)
    }
  }
}

@Composable
fun CompositeChildStep01Ui(state: CompositeChildStep01.State, modifier: Modifier = Modifier) {
  when (state) {
    is CompositeChildStep01.State.Loading -> LoadingState(modifier)
    is CompositeChildStep01.State.Result -> ResultState(state, modifier)
  }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Text("Loading....", modifier = modifier)
}

@Composable
private fun ResultState(state: CompositeChildStep01.State.Result, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  Column(modifier = modifier) {
    state.ints.forEach { value ->
      val isSelected = state.selected.contains(value)
      Row {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { selected ->
            if (selected) sink(CompositeChildStep01.Event.Selected(value))
            else sink(CompositeChildStep01.Event.Deselected(value))
          }
        )
        Text(text = value.toString())
      }
    }
  }
}
