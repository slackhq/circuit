// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.Turbine
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Test verifying that a [PopResult] is returned to a [rememberAnsweringNavigator] across a
 * navigation event.
 */
@RunWith(ComposeUiTestRunner::class)
class AnsweringNavigatorTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val state1 = Turbine<State>()
  private val state2 = Turbine<State>()
  private val state3 = Turbine<State>()
  private val resultTracker = Turbine<PopResult?>()

  private val circuit =
    Circuit.Builder()
      .addPresenter<TestScreen, State> { _, navigator, _ -> AnsweringPresenter(navigator, resultTracker) }
      .addPresenter<TestScreen2, State> { _, navigator, _ -> NormalPresenter(navigator) }
      .addPresenter<TestScreen3, State> { _, navigator, _ -> NormalPresenter(navigator) }
      .addUi<TestScreen, State> { state, _ -> SideEffect { state1.add(state) } }
      .addUi<TestScreen2, State> { state, _ -> SideEffect { state2.add(state) } }
      .addUi<TestScreen3, State> { state, _ -> SideEffect { state3.add(state) } }
      .build()

  @Test
  fun `verify pop result is returned to the answering navigator`() = runTest {
    with(composeTestRule) {
      val backStack = setCircuitContent(circuit)
      assertEquals(listOf(TestScreen), backStack.screens)
      // Go to next screen
      state1.awaitItem().eventSink(NavEvent.GoTo(TestScreen2))
      waitForIdle()
      assertEquals(listOf(TestScreen2, TestScreen), backStack.screens)
      // Pop back to the previous screen
      val expectedResult = TestValuePopResult("test")
      state2.awaitItem().eventSink(NavEvent.Pop(expectedResult))
      waitForIdle()
      assertEquals(listOf(TestScreen), backStack.screens)
      assertEquals(expectedResult, resultTracker.awaitItem())
    }
  }

  @Test
  fun `answeringNavigationAvailable is true in NavigableCircuitContent`() = runTest {
    var navigationAvailable = true
    val testCircuit =
      Circuit.Builder()
        .addStaticUi<TestStaticScreen, CircuitUiState> { _, _ ->
          navigationAvailable = answeringNavigationAvailable()
        }
        .build()
    composeTestRule.setContent {
      CircuitCompositionLocals(testCircuit) {
        val backStack = rememberSaveableBackStack(TestStaticScreen)
        val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
    composeTestRule.waitForIdle()
    assertEquals(true, navigationAvailable)
  }

  @Test
  fun `answeringNavigationAvailable returns false when locals are not available`() {
    var navigationAvailable = true
    composeTestRule.setContent { navigationAvailable = answeringNavigationAvailable() }
    composeTestRule.waitForIdle()
    assertEquals(false, navigationAvailable)
  }

  @Test
  fun `fallback navigator is used when answering navigation is not available`() {
    var fallbackCalled: Screen? = null
    val fallbackNavigator =
      object : Navigator by Navigator(SaveableBackStack(TestScreen), {}) {
        override fun goTo(screen: Screen): Boolean {
          fallbackCalled = screen
          return true
        }
      }

    composeTestRule.setContent {
      val answeringNavigator =
        rememberAnsweringNavigator<TestPopResult>(fallbackNavigator) {
          error("Result handler should not be called")
        }
      SideEffect { answeringNavigator.goTo(TestScreen2) }
    }
    composeTestRule.waitForIdle()
    assertEquals(TestScreen2, fallbackCalled)
  }

  @Test
  fun `result type filtering - wrong type is ignored`() = runTest {
    with(composeTestRule) {
      val backStack = setCircuitContent(circuit)
      assertEquals(listOf(TestScreen), backStack.screens)
      // Go to next screen
      state1.awaitItem().eventSink(NavEvent.GoTo(TestScreen2))
      waitForIdle()
      assertEquals(listOf(TestScreen2, TestScreen), backStack.screens)
      // Pop back with wrong result type
      state2.awaitItem().eventSink(NavEvent.Pop(OtherPopResult))
      waitForIdle()
      assertEquals(listOf(TestScreen), backStack.screens)
      // Result should be null because wrong type was returned
      resultTracker.expectNoEvents()
    }
  }

  @Test
  fun `multiple navigations with results`() = runTest {
    with(composeTestRule) {
      val backStack = setCircuitContent(circuit)
      assertEquals(listOf(TestScreen), backStack.screens)
      waitForIdle()

      // First navigation
      state1.awaitItem().eventSink(NavEvent.GoTo(TestScreen2))
      waitForIdle()
      assertEquals(listOf(TestScreen2, TestScreen), backStack.screens)

      val firstPop = TestValuePopResult("1")
      state2.awaitItem().eventSink(NavEvent.Pop(firstPop))
      waitForIdle()
      assertEquals(listOf(TestScreen), backStack.screens)
      assertEquals(firstPop, resultTracker.awaitItem())

      // Second navigation
      state1.awaitItem().eventSink(NavEvent.GoTo(TestScreen3))
      waitForIdle()
      assertEquals(listOf(TestScreen3, TestScreen), backStack.screens)

      val secondPop = TestValuePopResult("2")
      state2.awaitItem().eventSink(NavEvent.Pop(secondPop))
      waitForIdle()
      assertEquals(listOf(TestScreen), backStack.screens)
      assertEquals(secondPop, resultTracker.awaitItem())
    }
  }

  @Test
  fun `pop without result does not invoke result handler`() = runTest {
    with(composeTestRule) {
      val backStack = setCircuitContent(circuit)
      assertEquals(listOf(TestScreen), backStack.screens)
      // Go to next screen
      state1.awaitItem().eventSink(NavEvent.GoTo(TestScreen2))
      waitForIdle()
      assertEquals(listOf(TestScreen2, TestScreen), backStack.screens)
      // Pop back without result
      state2.awaitItem().eventSink(NavEvent.Pop())
      waitForIdle()
      assertEquals(listOf(TestScreen), backStack.screens)
      // Result should be null because no result was provided
      resultTracker.expectNoEvents()
    }
  }

  @Test
  fun `subtype result is accepted`() = runTest {

    val typeCircuit = Circuit.Builder()
      .addPresenter<TestScreen, State> { _, navigator, _ -> TypePresenter(navigator, resultTracker) }
      .addPresenter<TestScreen2, State> { _, navigator, _ -> NormalPresenter(navigator) }
      .addUi<TestScreen, State> { state, _ -> SideEffect { state1.add(state) } }
      .addUi<TestScreen2, State> { state, _ -> SideEffect { state2.add(state) } }
      .build()

    with(composeTestRule) {
      val backStack = setCircuitContent(typeCircuit)
      assertEquals(listOf(TestScreen), backStack.screens)
      // Go to next screen
      state1.awaitItem().eventSink(NavEvent.GoTo(TestScreen2))
      waitForIdle()
      assertEquals(listOf(TestScreen2, TestScreen), backStack.screens)
      // Pop back with result
      state2.awaitItem().eventSink(NavEvent.Pop(SuperPopResult.SubPopResult()))
      waitForIdle()
      assertEquals(listOf(TestScreen), backStack.screens)
      assertIs<SuperPopResult.SubPopResult>(resultTracker.awaitItem())
    }
  }
}

private open class SuperPopResult : PopResult {
  class SubPopResult : SuperPopResult()
}

private fun ComposeContentTestRule.setCircuitContent(circuit: Circuit): SaveableBackStack {
  lateinit var backStack: SaveableBackStack
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

private data class State(val eventSink: (NavEvent) -> Unit) :
  CircuitUiState

private class AnsweringPresenter(
  private val navigator: Navigator,
  private val resultTracker: Turbine<PopResult?>,
) : Presenter<State> {

  @Composable
  override fun present(): State {
    val answeringNavigator = rememberAnsweringNavigator<TestValuePopResult>(navigator) {
      resultTracker.add(it)
    }
    return State {
      when (it) {
        is NavEvent.GoTo -> answeringNavigator.goTo(it.screen)
        else -> error("Unexpected event: $it")
      }
    }
  }
}

private class TypePresenter(
  private val navigator: Navigator,
  private val resultTracker: Turbine<PopResult?>,
) : Presenter<State> {

  @Composable
  override fun present(): State {
    val answeringNavigator = rememberAnsweringNavigator<SuperPopResult>(navigator) {
      resultTracker.add(it)
    }
    return State {
      when (it) {
        is NavEvent.GoTo -> answeringNavigator.goTo(it.screen)
        else -> error("Unexpected event: $it")
      }
    }
  }
}

private class NormalPresenter(private val navigator: Navigator) : Presenter<State> {

  @Composable
  override fun present(): State {
    return State {
      navigator.onNavEvent(it)
    }
  }
}