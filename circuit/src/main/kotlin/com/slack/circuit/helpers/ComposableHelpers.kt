package com.slack.circuit.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.CompositeCircuitUiEvent
import com.slack.circuit.Presenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

@Composable
inline fun <reified E : CompositeCircuitUiEvent , reified R> rememberFilterEventAndGetState(
  events: Flow<CircuitUiEvent>,
  presenter: Presenter<CircuitUiState , CircuitUiEvent>
): R {
  val rememberEventFlow = remember {
    events.filterIsInstance<E>().map { it.event }
  }
  return presenter.present(rememberEventFlow)  as R
}