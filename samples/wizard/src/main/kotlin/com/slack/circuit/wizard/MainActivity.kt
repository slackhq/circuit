package com.slack.circuit.wizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.NavigableCircuitContent
import com.slack.circuit.NavigatorDefaults
import com.slack.circuit.Screen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.push
import com.slack.circuit.rememberCircuitNavigator
import com.slack.circuit.wizard.composite.CompositeScreenPresenterFactory_Factory
import com.slack.circuit.wizard.composite.CompositeScreenPresenter_Factory
import com.slack.circuit.wizard.composite.CompositeScreenPresenter_Factory_Impl
import com.slack.circuit.wizard.composite.CompositeScreenUiFactory
import com.slack.circuit.wizard.managed.ManagedChildScreen01UiFactory
import com.slack.circuit.wizard.managed.ManagedChildScreen02UiFactory
import com.slack.circuit.wizard.managed.ManagingUiFactory
import com.slack.circuit.wizard.managed.ManagedChildScreen01PresenterFactory
import com.slack.circuit.wizard.managed.ManagedChildScreen02PresenterFactory
import com.slack.circuit.wizard.managed.ManagingPresenterFactory
import com.slack.circuit.wizard.siblings.SiblingsChildScreen01UiFactory
import com.slack.circuit.wizard.siblings.SiblingsChildScreen02UiFactory
import com.slack.circuit.wizard.siblings.SiblingsChildScreen01PresenterFactory
import com.slack.circuit.wizard.siblings.SiblingsChildScreen02PresenterFactory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val managedPresenterFactories = listOf(
      ManagingPresenterFactory(),
      ManagedChildScreen01PresenterFactory(),
      ManagedChildScreen02PresenterFactory(),
    )
    val managedUiFactories = listOf(
      ManagingUiFactory(),
      ManagedChildScreen01UiFactory(),
      ManagedChildScreen02UiFactory(),
    )

    val siblingPresenterFactories = listOf(
      SiblingsChildScreen01PresenterFactory(),
      SiblingsChildScreen02PresenterFactory()
    )
    val siblingUiFactories = listOf(
      SiblingsChildScreen01UiFactory(),
      SiblingsChildScreen02UiFactory()
    )

    val delegateFactory = CompositeScreenPresenter_Factory()
    val factoryProvider = CompositeScreenPresenter_Factory_Impl.create(delegateFactory)
    val compositePresenterfactory = CompositeScreenPresenterFactory_Factory(factoryProvider)

    val circuitConfig =
      CircuitConfig.Builder()
        .addPresenterFactory(MainPresenterFactory())
        .addPresenterFactories(managedPresenterFactories)
        .addPresenterFactories(siblingPresenterFactories)
        .addPresenterFactory(compositePresenterfactory.get())
        .addUiFactory(MainUiFactory())
        .addUiFactories(managedUiFactories)
        .addUiFactories(siblingUiFactories)
        .addUiFactory(CompositeScreenUiFactory())
        .build()

    val screens: ImmutableList<Screen> = persistentListOf(MainScreen)

    setContent {
      val backStack = rememberSaveableBackStack { screens.forEach { screen -> push(screen) } }
      val navigator = rememberCircuitNavigator(backStack)

      CircuitCompositionLocals(circuitConfig) {
        NavigableCircuitContent(
          navigator = navigator,
          backstack = backStack,
          decoration = NavigatorDefaults.EmptyDecoration
        )
      }
    }
  }
}