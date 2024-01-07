package com.slack.circuit.star.benchmark

import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.parcel.CommonParcelize

@CommonParcelize
data class ListBenchmarksScreen(val useNestedContent: Boolean) : Screen {
  data class State(val useNestedContent: Boolean) : CircuitUiState
}