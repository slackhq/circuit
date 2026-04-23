// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG_UI_REMEMBERED = "TAG_UI_REMEMBERED"
private const val TAG_PRESENTER_REMEMBERED = "TAG_PRESENTER_REMEMBERED"
private const val TAG_STATE = "TAG_STATE"

/**
 * This is testing the following use cases for using [CircuitContent] based on
 * https://github.com/slackhq/circuit/issues/1169.
 *
 * Uses:
 * 1. [NavigableCircuitContent]
 * - The ui and presenter are retain based on the record, as the [Screen] can't change without a new
 *   record instance.
 * 2. [CircuitContent]
 * - a) Each [Screen] instance is a new "page" and would behave the same as
 *   [NavigableCircuitContent], by being keyed on the [Screen] instance.
 * - b) The [Screen] is a model, and it's used to update the current [CircuitContent] state, as if
 *   it were another Compose element. This is potentially common with a "widget" sub-circuit case.
 */
@RunWith(ComposeUiTestRunner::class)
class KeyedCircuitContentTests {

  @get:Rule val composeTestRule = createComposeRule()

  private val circuit =
    Circuit.Builder()
      .addPresenter<ScreenA, ScreenA.State> { screen, _, _ -> ScreenAPresenter(screen) }
      .addUi<ScreenA, ScreenA.State> { state, modifier -> ScreenAUi(state, modifier) }
      .addPresenter<ScreenB, ScreenB.State> { screen, _, _ -> ScreenBPresenter(screen) }
      .addUi<ScreenB, ScreenB.State> { state, modifier -> ScreenBUi(state, modifier) }
      .build()

  @Test
  fun one() {
    composeTestRule.run {
      val navigator = setUpTestOneContent()
      // Initial
      onNodeWithTag(TAG_STATE).assertTextEquals("1")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("1")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("1")
      // Push a new ScreenA
      navigator.goTo(ScreenA(2))
      onNodeWithTag(TAG_STATE).assertTextEquals("4")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("4")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("4")
      // Push a new ScreenA
      navigator.goTo(ScreenA(3))
      onNodeWithTag(TAG_STATE).assertTextEquals("9")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("9")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("9")
      // Push a new ScreenB
      navigator.goTo(ScreenB("abc"))
      onNodeWithTag(TAG_STATE).assertTextEquals("cba")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("cba")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("cba")
      // Back one
      navigator.pop()
      onNodeWithTag(TAG_STATE).assertTextEquals("9")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("9")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("9")
      // Back two
      navigator.pop()
      onNodeWithTag(TAG_STATE).assertTextEquals("4")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("4")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("4")
    }
  }

  @Test
  fun twoA() {
    composeTestRule.run {
      var screenState by setUpTestTwoAContent()
      // Initial
      onNodeWithTag(TAG_STATE).assertTextEquals("1")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("1")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("1")
      // Set a new ScreenA
      screenState = ScreenA(2)
      onNodeWithTag(TAG_STATE).assertTextEquals("4")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("4")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("4")
      // Set a new ScreenA
      screenState = ScreenA(3)
      onNodeWithTag(TAG_STATE).assertTextEquals("9")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("9")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("9")
      // Set a new ScreenB
      screenState = ScreenB("abc")
      onNodeWithTag(TAG_STATE).assertTextEquals("cba")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("cba")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("cba")
      // Back to a ScreenA
      screenState = ScreenA(3)
      onNodeWithTag(TAG_STATE).assertTextEquals("9")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("9")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("9")
      // Back to another ScreenA
      screenState = ScreenA(2)
      onNodeWithTag(TAG_STATE).assertTextEquals("4")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("4")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("4")
    }
  }

  @Test
  fun twoB() {
    composeTestRule.run {
      var screenState by setUpTestTwoBContent()
      // Initial
      onNodeWithTag(TAG_STATE).assertTextEquals("1")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("1")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("1")
      // Set a new ScreenA
      screenState = ScreenA(2)
      onNodeWithTag(TAG_STATE).assertTextEquals("4")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("1")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("1")
      // Set a new ScreenA
      screenState = ScreenA(3)
      onNodeWithTag(TAG_STATE).assertTextEquals("9")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("1")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("1")
      // Set a new ScreenB
      screenState = ScreenB("abc")
      onNodeWithTag(TAG_STATE).assertTextEquals("cba")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("cba")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("cba")
      // Back to a ScreenA
      screenState = ScreenA(3)
      onNodeWithTag(TAG_STATE).assertTextEquals("9")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("9")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("9")
      // Back to another ScreenA
      screenState = ScreenA(2)
      onNodeWithTag(TAG_STATE).assertTextEquals("4")
      onNodeWithTag(TAG_UI_REMEMBERED).assertTextEquals("9")
      onNodeWithTag(TAG_PRESENTER_REMEMBERED).assertTextEquals("9")
    }
  }

  private fun ComposeContentTestRule.setUpTestOneContent(screen: Screen = ScreenA(1)): Navigator {
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

  private fun ComposeContentTestRule.setUpTestTwoAContent(
    screen: Screen = ScreenA(1)
  ): MutableState<Screen> {
    val screenState = mutableStateOf(screen)
    setContent { CircuitCompositionLocals(circuit) { CircuitContent(screenState.value) } }
    return screenState
  }

  private fun ComposeContentTestRule.setUpTestTwoBContent(
    screen: Screen = ScreenA(1)
  ): MutableState<Screen> {
    val screenState = mutableStateOf(screen)
    setContent {
      CircuitCompositionLocals(circuit) {
        CircuitContent(screenState.value, key = screenState.value::class)
      }
    }
    return screenState
  }
}

private class ScreenA(val num: Int) : Screen {
  data class State(val numSquare: Int, val rememberedNumSquare: Int) : CircuitUiState
}

private class ScreenAPresenter(val screen: ScreenA) : Presenter<ScreenA.State> {
  @Composable
  override fun present(): ScreenA.State {
    val square = remember { screen.num * screen.num }
    return ScreenA.State(screen.num * screen.num, square)
  }
}

@Composable
private fun ScreenAUi(state: ScreenA.State, modifier: Modifier = Modifier) {
  Column(modifier) {
    val remembered = remember { state.numSquare }
    Text(text = "$remembered", modifier = Modifier.testTag(TAG_UI_REMEMBERED))
    Text(text = "${state.numSquare}", modifier = Modifier.testTag(TAG_STATE))
    Text(
      text = "${state.rememberedNumSquare}",
      modifier = Modifier.testTag(TAG_PRESENTER_REMEMBERED),
    )
  }
}

private class ScreenB(val text: String) : Screen {

  data class State(val textReverse: String, val rememberedTextReverse: String) : CircuitUiState
}

private class ScreenBPresenter(val screen: ScreenB) : Presenter<ScreenB.State> {
  @Composable
  override fun present(): ScreenB.State {
    val textReverse = remember { screen.text.reversed() }
    return ScreenB.State(screen.text.reversed(), textReverse)
  }
}

@Composable
private fun ScreenBUi(state: ScreenB.State, modifier: Modifier = Modifier) {
  Column(modifier) {
    val remembered = remember { state.textReverse }
    Text(text = remembered, modifier = Modifier.testTag(TAG_UI_REMEMBERED))
    Text(text = state.textReverse, modifier = Modifier.testTag(TAG_STATE))
    Text(text = state.rememberedTextReverse, modifier = Modifier.testTag(TAG_PRESENTER_REMEMBERED))
  }
}
