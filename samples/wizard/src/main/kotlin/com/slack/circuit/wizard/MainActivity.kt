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
import com.slack.circuit.wizard.managed.ManagedChildScreen01UiFactory
import com.slack.circuit.wizard.managed.ManagedChildScreen02UiFactory
import com.slack.circuit.wizard.managed.ManagingUiFactory
import com.slack.circuit.wizard.managed.managedChildScreen01PresenterFactory
import com.slack.circuit.wizard.managed.managedChildScreen02PresenterFactory
import com.slack.circuit.wizard.managed.managingPresenterFactory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val managedPresenterFactories = listOf(
      managingPresenterFactory(),
      managedChildScreen01PresenterFactory(),
      managedChildScreen02PresenterFactory(),
    )
    val managedUiFactories = listOf(
      ManagingUiFactory(),
      ManagedChildScreen01UiFactory(),
      ManagedChildScreen02UiFactory(),
    )

    val circuitConfig =
      CircuitConfig.Builder()
        .addPresenterFactory(mainPresenterFactory())
        .addPresenterFactories(managedPresenterFactories)
        .addUiFactory(MainUiFactory())
        .addUiFactories(managedUiFactories)
        .build()

    val screens: ImmutableList<Screen> = persistentListOf(MainScreen)

    setContent {
      val backStack = rememberSaveableBackStack { screens.forEach { screen -> push(screen) } }
      val navigator = rememberCircuitNavigator(backStack)

      CircuitCompositionLocals(circuitConfig) {
        NavigableCircuitContent(navigator, backStack)
      }
    }
  }
}