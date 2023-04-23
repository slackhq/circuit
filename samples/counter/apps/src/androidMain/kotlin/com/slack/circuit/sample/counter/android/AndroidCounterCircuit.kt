// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.android

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sample.counter.CounterScreen
import kotlinx.parcelize.Parcelize

// TODO why doesn't Parcelable get inherited through CounterScreen?
@Parcelize object AndroidCounterScreen : CounterScreen, Parcelable

@Composable
fun Counter(state: CounterScreen.State, modifier: Modifier = Modifier) {
  val color = if (state.count >= 0) Color.Unspecified else MaterialTheme.colorScheme.error
  Box(modifier.fillMaxSize()) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(CenterHorizontally),
        text = "Count: ${state.count}",
        style = MaterialTheme.typography.displayLarge,
        color = color
      )
      Spacer(modifier = Modifier.height(16.dp))
      val sink = state.eventSink
      Button(
        modifier = Modifier.align(CenterHorizontally),
        onClick = { sink(CounterScreen.Event.Increment) }
      ) {
        Icon(rememberVectorPainter(Icons.Filled.Add), "Increment")
      }
      Button(
        modifier = Modifier.align(CenterHorizontally),
        onClick = { sink(CounterScreen.Event.Decrement) }
      ) {
        Icon(rememberVectorPainter(Icons.Filled.Remove), "Decrement")
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
