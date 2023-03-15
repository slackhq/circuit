package com.slack.circuit.tacos.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.slack.circuit.tacos.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object SummaryOrderStep : OrderStep {
  @IgnoredOnParcel override val number = 3
  @IgnoredOnParcel override val headerResId = R.string.summary_step_header

  data class SummaryState(val eventSink: () -> Unit) : OrderStep.State
}

@Composable
internal fun summaryProducer(eventSink: (OrderStep.Event) -> Unit) =
  SummaryOrderStep.SummaryState { eventSink(OrderStep.Restart) }

@Composable
internal fun SummaryUi(state: SummaryOrderStep.SummaryState, modifier: Modifier) {
  val sink = state.eventSink
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Button(onClick = sink) {
      Text(stringResource(R.string.summary_step_order_again))
    }
  }
}