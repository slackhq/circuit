package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object SummaryOrderStep : OrderStep {
  override val number = 2

  sealed interface State : OrderStep.State
}

@Composable
fun SummaryUi(state: SummaryOrderStep.State, modifier: Modifier = Modifier) {

}
