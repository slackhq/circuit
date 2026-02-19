// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

@Composable
fun Counter(state: CounterScreen.State, modifier: Modifier = Modifier) {
  val color = if (state.count >= 0) Color.Unspecified else MaterialTheme.colorScheme.error
  Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = "Count: ${state.count}",
        style = MaterialTheme.typography.displayLarge,
        color = color,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { state.eventSink(CounterScreen.Event.Increment) },
      ) {
        Icon(rememberVectorPainter(Icons.Filled.Add), "Increment")
      }
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { state.eventSink(CounterScreen.Event.Decrement) },
      ) {
        Icon(rememberVectorPainter(Remove), "Decrement")
      }
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = { state.eventSink(CounterScreen.Event.GoTo(PrimeScreen(state.count))) },
        modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(modifier = Modifier.padding(4.dp), text = "Prime?")
      }
    }
  }
}

@Composable
fun Prime(state: PrimeScreen.State, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.displayLarge,
        text = "${state.number}",
      )
      Spacer(modifier = Modifier.height(16.dp))
      val text =
        if (state.isPrime) {
          "${state.number} is a prime number!"
        } else {
          "${state.number} is not a prime number."
        }
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.titleLarge,
        text = text,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { state.eventSink(PrimeScreen.Event.Pop) },
      ) {
        Text("Back")
      }
    }
  }
}

class CounterUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      is CounterScreen -> ui<CounterScreen.State> { state, modifier -> Counter(state, modifier) }
      is PrimeScreen -> ui<PrimeScreen.State> { state, modifier -> Prime(state, modifier) }
      else -> null
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun CounterPreview() {
  Counter(CounterScreen.State(0))
}

@Preview(showBackground = true)
@Composable
private fun CounterPreviewNegative() {
  Counter(CounterScreen.State(-1))
}
