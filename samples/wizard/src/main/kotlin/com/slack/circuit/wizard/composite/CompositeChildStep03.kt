package com.slack.circuit.wizard.composite

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object CompositeChildStep03 : CompositeScreen.ChildScreen {
  @IgnoredOnParcel override val number = 2

  @Parcelize data class Input(
    val ints: Set<Int> = emptySet(),
    val strings: Set<String> = emptySet()
  ) : Parcelable

  @Parcelize data class State(
    val ints: Set<Int> = emptySet(),
    val strings: Set<String> = emptySet()
  ) : CompositeScreen.ChildScreen.State
}

@Composable
fun compositeChildStep03State(input: CompositeChildStep03.Input): CompositeScreen.ChildScreen.State {
  // perform operations to prepare input state for display
  return CompositeChildStep03.State(
    ints = input.ints,
    strings = input.strings
  )
}

@Composable
fun CompositeChildStep03Ui(state: CompositeChildStep03.State, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text("You selected the following ints:")
    Text(state.ints.joinToString(", "))
    Divider()
    Text("You selected the following strings:")
    Text(state.strings.joinToString(", "))
  }
}