package com.slack.circuit.sample.counter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

@Composable
fun Counter(state: CounterScreen.State, modifier: Modifier = Modifier) {
  val color = if (state.count >= 0) Color.Unspecified else MaterialTheme.colorScheme.error
  Box(modifier.fillMaxSize()) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = "Count: ${state.count}",
        style = MaterialTheme.typography.displayLarge,
        color = color
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { state.eventSink(CounterScreen.Event.Increment) }
      ) {
        Icon(rememberVectorPainter(Icons.Filled.Add), "Increment")
      }
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { state.eventSink(CounterScreen.Event.Decrement) }
      ) {
        Icon(rememberVectorPainter(Remove), "Decrement")
      }
    }
  }
}

class CounterUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      is CounterScreen -> ui<CounterScreen.State> { state, modifier -> Counter(state, modifier) }
      else -> null
    }
  }
}
