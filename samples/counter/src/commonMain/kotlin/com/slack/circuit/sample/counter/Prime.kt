// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.internal.runtime.Parcelize

@Parcelize data class CommonPrimeScreen(override val number: Int) : PrimeScreen

@Composable
fun Prime(state: PrimeScreen.State, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.headlineSmall,
        text = "${state.number}",
      )
      Spacer(modifier = Modifier.height(16.dp))
      if (state.isPrime) {
        Text(
          modifier = Modifier.align(Alignment.CenterHorizontally),
          text = "${state.number} is a prime number!",
        )
      } else {
        Text(
          modifier = Modifier.align(Alignment.CenterHorizontally),
          text = "${state.number} is not a prime number.",
        )
      }
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
