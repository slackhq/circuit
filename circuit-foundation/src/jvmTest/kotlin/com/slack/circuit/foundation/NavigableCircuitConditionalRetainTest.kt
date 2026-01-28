// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG_SHOW_CHILD_BUTTON = "TAG_SHOW_CHILD_BUTTON"
private const val TAG_HIDE_CHILD_BUTTON = "TAG_HIDE_CHILD_BUTTON"
private const val TAG_INC_BUTTON = "TAG_INC_BUTTON"
private const val TAG_GOTO_BUTTON = "TAG_GOTO_BUTTON"
private const val TAG_POP_BUTTON = "TAG_POP_BUTTON"
private const val TAG_CONDITIONAL_RETAINED = "TAG_CONDITIONAL_RETAINED"
private const val TAG_UI_RETAINED = "TAG_UI_RETAINED"
private const val TAG_PRESENTER_RETAINED = "TAG_PRESENTER_RETAINED"
private const val TAG_STATE = "TAG_STATE"

@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitConditionalRetainTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val dataSource = DataSource()

  private val circuit =
    Circuit.Builder()
      .addPresenter<ScreenA, ScreenA.State> { _, _, _ -> ScreenAPresenter() }
      .addUi<ScreenA, ScreenA.State> { _, modifier -> ScreenAUi(modifier) }
      .addPresenter<ScreenB, ScreenB.State> { _, _, _ -> ScreenBPresenter(dataSource) }
      .addUi<ScreenB, ScreenB.State> { state, modifier -> ScreenBUi(state, modifier) }
      .addPresenter<ScreenC, ScreenC.State> { _, navigator, _ -> ScreenCPresenter(navigator) }
      .addUi<ScreenC, ScreenC.State> { state, modifier -> ScreenCUi(state, modifier) }
      .addPresenter<ScreenD, ScreenD.State> { _, navigator, _ -> ScreenDPresenter(navigator) }
      .addUi<ScreenD, ScreenD.State> { state, modifier -> ScreenDUi(state, modifier) }
      .build()

  @Test
  fun nestedCircuitContentWithPresentWithLifecycle() {
    nestedCircuitContent(presentWithLifecycle = true)
  }

  @Test
  fun nestedCircuitContentWithoutPresentWithLifecycle() {
    nestedCircuitContent(presentWithLifecycle = false)
  }

  @Test
  fun removedConditionalRetainWithPresentWithLifecycle() {
    removedConditionalRetain(presentWithLifecycle = true)
  }

  @Test
  fun removedConditionalRetainWithoutPresentWithLifecycle() {
    removedConditionalRetain(presentWithLifecycle = false)
  }

  @Test
  fun addedConditionalRetainWithPresentWithLifecycle() {
    addedConditionalRetain(presentWithLifecycle = true)
  }

  @Test
  fun addedConditionalRetainWithoutPresentWithLifecycle() {
    addedConditionalRetain(presentWithLifecycle = false)
  }

  /** Nested circuit content should not be retained if it is removed */
  private fun nestedCircuitContent(presentWithLifecycle: Boolean) {
    composeTestRule.run {
      val modifiedCircuit = circuit.newBuilder().presentWithLifecycle(presentWithLifecycle).build()
      setUpTestContent(modifiedCircuit, ScreenA)

      onNodeWithTag(TAG_STATE).assertDoesNotExist()
      onNodeWithTag(TAG_PRESENTER_RETAINED).assertDoesNotExist()
      onNodeWithTag(TAG_UI_RETAINED).assertDoesNotExist()

      dataSource.value = 1

      // Show child
      onNodeWithTag(TAG_SHOW_CHILD_BUTTON).performClick()

      onNodeWithTag(TAG_STATE).assertTextEquals("1")
      onNodeWithTag(TAG_UI_RETAINED).assertTextEquals("1")
      onNodeWithTag(TAG_PRESENTER_RETAINED).assertTextEquals("1")

      // Hide child
      onNodeWithTag(TAG_HIDE_CHILD_BUTTON).performClick()

      onNodeWithTag(TAG_STATE).assertDoesNotExist()
      onNodeWithTag(TAG_PRESENTER_RETAINED).assertDoesNotExist()
      onNodeWithTag(TAG_UI_RETAINED).assertDoesNotExist()

      dataSource.value = 2

      // Show child
      onNodeWithTag(TAG_SHOW_CHILD_BUTTON).performClick()

      // Retained reset
      onNodeWithTag(TAG_STATE).assertTextEquals("2")
      onNodeWithTag(TAG_UI_RETAINED).assertTextEquals("2")
      onNodeWithTag(TAG_PRESENTER_RETAINED).assertTextEquals("2")
    }
  }

  /**
   * Conditional rememberRetained should not be retained if it is removed no matter current
   * RetainedStateRegistry is saved or not.
   */
  private fun removedConditionalRetain(presentWithLifecycle: Boolean) {
    composeTestRule.run {
      val modifiedCircuit = circuit.newBuilder().presentWithLifecycle(presentWithLifecycle).build()
      setUpTestContent(modifiedCircuit, ScreenC)

      onNodeWithTag(TAG_STATE).assertDoesNotExist()
      onNodeWithTag(TAG_PRESENTER_RETAINED).assertDoesNotExist()
      onNodeWithTag(TAG_UI_RETAINED).assertDoesNotExist()

      // Show child
      onNodeWithTag(TAG_SHOW_CHILD_BUTTON).performClick()

      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("0")
      onNodeWithTag(TAG_INC_BUTTON).performClick()
      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("1")

      // Hide child
      onNodeWithTag(TAG_HIDE_CHILD_BUTTON).performClick()

      // Navigate other screen and pop for saving ScreenC's state
      onNodeWithTag(TAG_GOTO_BUTTON).performClick()
      onNodeWithTag(TAG_POP_BUTTON).performClick()

      // Show child
      onNodeWithTag(TAG_SHOW_CHILD_BUTTON).performClick()

      // Child's retained state should not be retained
      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("0")
    }
  }

  /**
   * Conditional rememberRetained should be retained if it is added and current
   * RetainedStateRegistry is saved
   */
  private fun addedConditionalRetain(presentWithLifecycle: Boolean) {
    composeTestRule.run {
      val modifiedCircuit = circuit.newBuilder().presentWithLifecycle(presentWithLifecycle).build()
      setUpTestContent(modifiedCircuit, ScreenC)

      onNodeWithTag(TAG_STATE).assertDoesNotExist()
      onNodeWithTag(TAG_PRESENTER_RETAINED).assertDoesNotExist()
      onNodeWithTag(TAG_UI_RETAINED).assertDoesNotExist()

      // Show child
      onNodeWithTag(TAG_SHOW_CHILD_BUTTON).performClick()

      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("0")
      onNodeWithTag(TAG_INC_BUTTON).performClick()
      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("1")

      // Navigate other screen and pop for saving ScreenC's state
      onNodeWithTag(TAG_GOTO_BUTTON).performClick()
      onNodeWithTag(TAG_POP_BUTTON).performClick()

      // Child's retained state should be retained
      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("1")

      // Hide child
      onNodeWithTag(TAG_HIDE_CHILD_BUTTON).performClick()
      // Show child
      onNodeWithTag(TAG_SHOW_CHILD_BUTTON).performClick()

      // Child's retained state should not be retained
      onNodeWithTag(TAG_CONDITIONAL_RETAINED).assertTextEquals("0")
    }
  }

  private fun ComposeContentTestRule.setUpTestContent(circuit: Circuit, screen: Screen): Navigator {
    lateinit var navigator: Navigator
    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(screen)
        navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
    return navigator
  }

  private data object ScreenA : Screen {
    data object State : CircuitUiState
  }

  private class ScreenAPresenter : Presenter<ScreenA.State> {
    @Composable
    override fun present(): ScreenA.State {
      return ScreenA.State
    }
  }

  @Composable
  private fun ScreenAUi(modifier: Modifier = Modifier) {
    Column(modifier) {
      val isChildVisible = remember { mutableStateOf(false) }
      Button(
        modifier = Modifier.testTag(TAG_SHOW_CHILD_BUTTON),
        onClick = { isChildVisible.value = true },
      ) {
        Text("show")
      }
      Button(
        modifier = Modifier.testTag(TAG_HIDE_CHILD_BUTTON),
        onClick = { isChildVisible.value = false },
      ) {
        Text("hide")
      }
      if (isChildVisible.value) {
        CircuitContent(screen = ScreenB)
      }
    }
  }

  private data object ScreenB : Screen {

    data class State(val count: Int, val retainedCount: Int) : CircuitUiState
  }

  private class ScreenBPresenter(private val source: DataSource) : Presenter<ScreenB.State> {

    @Composable
    override fun present(): ScreenB.State {
      val count = source.fetch()
      val retained = rememberRetained { count }
      return ScreenB.State(count, retained)
    }
  }

  @Composable
  private fun ScreenBUi(state: ScreenB.State, modifier: Modifier = Modifier) {
    Column(modifier) {
      val retained = rememberRetained { state.count }
      Text(text = retained.toString(), modifier = Modifier.testTag(TAG_UI_RETAINED))
      Text(text = state.count.toString(), modifier = Modifier.testTag(TAG_STATE))
      Text(
        text = state.retainedCount.toString(),
        modifier = Modifier.testTag(TAG_PRESENTER_RETAINED),
      )
    }
  }

  private data object ScreenC : Screen {

    data class State(val eventSink: (Event) -> Unit) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
      data class GoTo(val screen: Screen) : Event
    }
  }

  private class ScreenCPresenter(private val navigator: Navigator) : Presenter<ScreenC.State> {
    @Composable
    override fun present(): ScreenC.State {
      return ScreenC.State { event ->
        when (event) {
          is ScreenC.Event.GoTo -> navigator.goTo(event.screen)
        }
      }
    }
  }

  @Composable
  private fun ScreenCUi(state: ScreenC.State, modifier: Modifier = Modifier) {
    Column(modifier) {
      Button(
        modifier = Modifier.testTag(TAG_GOTO_BUTTON),
        onClick = { state.eventSink(ScreenC.Event.GoTo(ScreenD)) },
      ) {
        Text("goto")
      }
      val isVisible = rememberRetained { mutableStateOf(false) }
      Button(
        modifier = Modifier.testTag(TAG_SHOW_CHILD_BUTTON),
        onClick = { isVisible.value = true },
      ) {
        Text("show")
      }
      Button(
        modifier = Modifier.testTag(TAG_HIDE_CHILD_BUTTON),
        onClick = { isVisible.value = false },
      ) {
        Text("hide")
      }
      if (isVisible.value) {
        val count = rememberRetained { mutableStateOf(0) }
        Button(modifier = Modifier.testTag(TAG_INC_BUTTON), onClick = { count.value += 1 }) {
          Text("inc")
        }
        Text(modifier = Modifier.testTag(TAG_CONDITIONAL_RETAINED), text = count.value.toString())
      }
    }
  }

  private data object ScreenD : Screen {

    data class State(val eventSink: (Event) -> Unit) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
      data object Pop : Event
    }
  }

  private class ScreenDPresenter(private val navigator: Navigator) : Presenter<ScreenD.State> {

    @Composable
    override fun present(): ScreenD.State {
      return ScreenD.State { event ->
        when (event) {
          is ScreenD.Event.Pop -> navigator.pop()
        }
      }
    }
  }

  @Composable
  private fun ScreenDUi(state: ScreenD.State, modifier: Modifier = Modifier) {
    Column(modifier) {
      Button(
        onClick = { state.eventSink(ScreenD.Event.Pop) },
        modifier = Modifier.testTag(TAG_POP_BUTTON),
      ) {
        Text(text = "pop")
      }
    }
  }

  private class DataSource {
    var value: Int = 0

    fun fetch(): Int = value
  }
}
