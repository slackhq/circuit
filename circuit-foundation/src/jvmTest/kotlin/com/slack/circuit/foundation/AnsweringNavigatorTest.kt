// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.Turbine
import com.slack.circuit.backstack.AnsweringBackStack
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Test verifying that a [PopResult] is returned to a [rememberAnsweringNavigator] across a
 * navigation event.
 */
@RunWith(ComposeUiTestRunner::class)
class AnsweringNavigatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val state1 = Turbine<State>()
  private val state2 = Turbine<State>()

  private val circuit =
    Circuit.Builder()
      .addPresenter<TestScreen, State> { _, navigator, _ -> AnsweringPresenter(navigator) }
      .addPresenter<TestScreen2, State> { _, navigator, _ -> PopPresenter(navigator) }
      .addUi<TestScreen, State> { state, _ -> SideEffect { state1.add(state) } }
      .addUi<TestScreen2, State> { state, _ -> SideEffect { state2.add(state) } }
      .build()

  @Test
  fun `verify pop result is returned to the answering navigator`() = runTest {
    with(composeTestRule) {
      val backStack = setCircuitContent(circuit)
      assertEquals(listOf(TestScreen), backStack.screens)
      // Go to next screen
      state1.awaitItem().eventSink()
      assertEquals(listOf(TestScreen2, TestScreen), backStack.screens)
      waitForIdle()
      // Pop back to the previous screen
      state2.awaitItem().eventSink()
      assertEquals(listOf(TestScreen), backStack.screens)
      waitForIdle()
      assertEquals(TestPopResult, state1.expectMostRecentItem().resultCount)
    }
  }
}

private fun ComposeContentTestRule.setCircuitContent(circuit: Circuit): AnsweringBackStack<*> {
  lateinit var backStack: AnsweringBackStack<*>
  setContent {
    CircuitCompositionLocals(circuit) {
      backStack = rememberSaveableBackStack(TestScreen)
      val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
      NavigableCircuitContent(navigator = navigator, backStack = backStack)
    }
  }
  return backStack
}

private val BackStack<*>.screens
  get() = map { it.screen }

private data class State(val resultCount: TestPopResult? = null, val eventSink: () -> Unit) :
  CircuitUiState

private class AnsweringPresenter(private val navigator: Navigator) : Presenter<State> {

  @Composable
  override fun present(): State {
    var result by remember { mutableStateOf<TestPopResult?>(null) }
    val answeringNavigator = rememberAnsweringNavigator<TestPopResult>(navigator) { result = it }
    return State(result) { answeringNavigator.goTo(TestScreen2) }
  }
}

private class PopPresenter(private val navigator: Navigator) : Presenter<State> {

  @Composable
  override fun present(): State {
    return State { navigator.pop(TestPopResult) }
  }
}
