// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import app.cash.turbine.Turbine
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.retained.retain
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTestApi::class, ExperimentalCircuitApi::class)
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

  @Test
  fun `nested record is inactive while the parent record is inactive`() = runTest {
    var parentActive by mutableStateOf(true)
    var nestedLifecycle: Pair<Boolean, Boolean>? = null
    val nestedCircuit =
      Circuit.Builder()
        .addPresenter<TestScreen, TestState> { _, navigator, _ ->
          TestPresenter(TestScreen, navigator)
        }
        .addUi<TestScreen, TestState> { _, _ ->
          val lifecycle = LocalRecordLifecycle.current
          val activeAndVisible = lifecycle.isActive to lifecycle.isVisible
          SideEffect { nestedLifecycle = activeAndVisible }
        }
        .build()

    runComposeUiTest {
      setContent {
        CircuitCompositionLocals(nestedCircuit, CircuitSaver.NoOp) {
          ProvideRecordLifecycle(isActive = parentActive) {
            val backStack = rememberSaveableBackStack(TestScreen)
            val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
            NavigableCircuitContent(navigator = navigator, backStack = backStack)
          }
        }
      }
      waitForIdle()
      assertEquals(true to true, nestedLifecycle)

      parentActive = false
      waitForIdle()
      assertEquals(false to false, nestedLifecycle)

      parentActive = true
      waitForIdle()
      assertEquals(true to true, nestedLifecycle)
    }
  }

  @Test
  fun `nested record is only visible while current and the parent record is visible`() = runTest {
    var parentVisible by mutableStateOf(true)
    val lifecycles = mutableMapOf<Screen, Pair<Boolean, Boolean>>()

    @Composable
    fun CaptureLifecycle(screen: Screen) {
      val lifecycle = LocalRecordLifecycle.current
      val activeAndVisible = lifecycle.isActive to lifecycle.isVisible
      SideEffect { lifecycles[screen] = activeAndVisible }
    }

    val nestedCircuit =
      Circuit.Builder()
        .addPresenter<TestScreen, TestState> { _, navigator, _ ->
          TestPresenter(TestScreen, navigator)
        }
        .addPresenter<TestScreen2, TestState> { _, navigator, _ ->
          TestPresenter(TestScreen2, navigator)
        }
        .addUi<TestScreen, TestState> { _, _ -> CaptureLifecycle(TestScreen) }
        .addUi<TestScreen2, TestState> { _, _ -> CaptureLifecycle(TestScreen2) }
        .build()

    val visible =
      mapOf<Screen, Pair<Boolean, Boolean>>(
        TestScreen to (false to false),
        TestScreen2 to (true to true),
      )
    val hidden =
      mapOf<Screen, Pair<Boolean, Boolean>>(
        TestScreen to (false to false),
        TestScreen2 to (true to false),
      )
    runComposeUiTest {
      setContent {
        CircuitCompositionLocals(nestedCircuit, CircuitSaver.NoOp) {
          ProvideRecordLifecycle(isActive = true, isVisible = parentVisible) {
            val backStack = rememberSaveableBackStack(listOf(TestScreen, TestScreen2))
            val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
            NavigableCircuitContent(
              navigator = navigator,
              backStack = backStack,
              decoration = AllRecordsDecoration,
            )
          }
        }
      }
      waitForIdle()
      assertEquals(visible, lifecycles)

      parentVisible = false
      waitForIdle()
      assertEquals(hidden, lifecycles)

      parentVisible = true
      waitForIdle()
      assertEquals(visible, lifecycles)
    }
  }
}

private object AllRecordsDecoration : NavDecoration {
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    for (arg in args) key(arg.key) { content(arg) }
  }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setCircuitContent(circuit: Circuit) {
  setContent {
    CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
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
    val value = retain(key = checkNotNull(screen::class.simpleName)) { mutableIntStateOf(0) }
    SideEffect { value.value++ }
    return TestState(Snapshot.withoutReadObservation { value.value }) { navigator.onNavEvent(it) }
  }
}
