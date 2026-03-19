// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import app.cash.turbine.Turbine
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTestApi::class)
@RunWith(ComposeUiTestRunner::class)
class RecordLifeCycleTest {

  private val state1 = Turbine<TestState>()
  private val state2 = Turbine<TestState>()
  private val state3 = Turbine<TestState>()

  private val circuit =
    Circuit.Builder()
      .addPresenter<TestScreen, TestState> { _, navigator, _ ->
        TestPresenter(TestScreen, navigator)
      }
      .addPresenter<TestScreen2, TestState> { _, navigator, _ ->
        TestPresenter(TestScreen2, navigator)
      }
      .addPresenter<TestScreen3, TestState> { _, navigator, _ ->
        TestPresenter(TestScreen3, navigator)
      }
      .addUi<TestScreen, TestState> { state, _ -> SideEffect { state1.add(state) } }
      .addUi<TestScreen2, TestState> { state, _ -> SideEffect { state2.add(state) } }
      .addUi<TestScreen3, TestState> { state, _ -> SideEffect { state3.add(state) } }
      .presentWithLifecycle(true)
      .build()

  @Test
  fun `value is retained when popped back`() = runTest {
    runComposeUiTest {
      setCircuitContent(circuit)

      val initialState = state1.awaitItem()
      assertEquals(0, initialState.value)
      state1.expectNoEvents()

      initialState.eventSink(NavEvent.GoTo(TestScreen2))
      waitForIdle()

      state2.awaitItem().eventSink(NavEvent.Pop())
      waitForIdle()
      // Paused state is emitted first as its not the active item in the navstack
      // when its composed into the AnimatedContent in AnimatedNavDecoration.
      val pausedState = state1.awaitItem()
      assertEquals(0, pausedState.value)
      val activeState = state1.awaitItem()
      assertEquals(1, activeState.value)
    }
  }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setCircuitContent(circuit: Circuit) {
  setContent {
    CircuitCompositionLocals(circuit) {
      val backStack = rememberSaveableBackStack(TestScreen)
      val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
      NavigableCircuitContent(navigator = navigator, backStack = backStack)
    }
  }
}

private data class TestState(val value: Int, val eventSink: (NavEvent) -> Unit) : CircuitUiState

private class TestPresenter(private val screen: Screen, private val navigator: Navigator) :
  Presenter<TestState> {

  @Composable
  override fun present(): TestState {
    val value = rememberRetained(key = screen::class.simpleName) { mutableIntStateOf(0) }
    SideEffect { value.value++ }
    return TestState(Snapshot.withoutReadObservation { value.value }) { navigator.onNavEvent(it) }
  }
}
