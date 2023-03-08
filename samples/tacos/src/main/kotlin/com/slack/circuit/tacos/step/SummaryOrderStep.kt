package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object SummaryOrderStep : OrderStep {
  override val number = 2
}

@Composable
fun SummaryUi(state: SummaryProducer.State, modifier: Modifier = Modifier) {

}
