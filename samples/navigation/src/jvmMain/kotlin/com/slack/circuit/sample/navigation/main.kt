package com.slack.circuit.sample.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalSharedTransitionApi::class)
fun main() {

  val tabs =
    persistentListOf(TabScreen.Root, TabScreen.Screen1, TabScreen.Screen2, TabScreen.Screen3)
  val circuit = buildCircuitForTabs(tabs)
  application {
    Window(title = "Navigation Sample", onCloseRequest = ::exitApplication) {
      MaterialTheme {
        val backStack = rememberSaveableBackStack(tabs.first())
        val navigator = rememberCircuitNavigator(backStack) { exitApplication() }
        CircuitCompositionLocals(circuit) {
          SharedElementTransitionLayout {
            ContentScaffold(backStack, navigator, tabs, Modifier.fillMaxSize())
          }
        }
      }
    }
  }
}
