package com.slack.circuit.wizard.managed

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiState
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.AppScope
import kotlinx.parcelize.Parcelize

@Parcelize
object ManagedChildScreen02 : ManagedChildScreen {
  object NoState : CircuitUiState
}

@CircuitInject(ManagedChildScreen02::class, AppScope::class)
@Composable
fun managedChildScreen02Presenter() = ManagedChildScreen02.NoState

@CircuitInject(ManagedChildScreen02::class, AppScope::class)
@Composable
fun ManagedChildScreen02Ui(modifier: Modifier) {
  Text(text = "Child 02 content", modifier = modifier)
}
