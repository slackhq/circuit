package com.slack.circuit.wizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.Screen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuitConfig =
      CircuitConfig.Builder()
        .build()

    val screens: ImmutableList<Screen> = persistentListOf()

    setContent {
      val backStack = rememberSaveableBackStack { screens.forEach { screen -> push(screen) } }
      val navigator = rememberCircuitNavigator(backStack)

      CircuitCompositionLocals(circuitConfig) {
        NavigableCircuitContent(navigator, backStack)
      }
    }
  }
}