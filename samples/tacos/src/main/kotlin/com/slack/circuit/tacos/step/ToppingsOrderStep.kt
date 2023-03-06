package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object ToppingsOrderStep : OrderStep {
  override val number = 1

  sealed interface State : OrderStep.State
}

@Composable
fun ToppingsUi(state: ToppingsOrderStep.State, modifier: Modifier = Modifier) {

}
