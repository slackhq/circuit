// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG_TEXT = "text"

@RunWith(ComposeUiTestRunner::class)
class NavResultTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, navigator, _ ->
        TestResultPresenter(navigator, screen as TestResultScreen)
      }
      .addUiFactory { _, _ ->
        ui<TestResultScreen.State> { state, modifier -> TestResultContent(state, modifier) }
      }
      .build()

  // TODO
  //  - Only caller gets the result back
  //  - Preserved on config changes
  //  - ???

  private fun ComposeContentTestRule.goToNext(count: AtomicInteger) {
    with(onNodeWithTag(TAG_TEXT)) {
      performTextClearance()
      performTextInput(count.toString())
    }
    onNodeWithTag(TAG_GO_NEXT).performClick()
    with(onNodeWithTag(TAG_TEXT)) {
      assertTextEquals(count.toString())
      performTextClearance()
      count.incrementAndGet()
      performTextInput(count.toString())
    }
  }

  private fun ComposeContentTestRule.popBack(count: AtomicInteger) {
    with(onNodeWithTag(TAG_TEXT)) {
      performTextClearance()
      count.incrementAndGet()
      performTextInput(count.toString())
    }
    onNodeWithTag(TAG_POP).performClick()
    with(onNodeWithTag(TAG_TEXT)) { assertTextEquals(count.toString()) }
  }

  @Test
  fun simplePushAndPop() {
    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack { push(TestResultScreen("Initial")) }
          val navigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = {}, // no-op for tests
            )
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }

      // Push 10 screens up, incrementing the count and passing on its updated value each time
      // Then pop 10 screens down, decrementing the count and passing on its updated value each time
      // Then do it one more time to make sure we can re-launch from the same screen
      val count = AtomicInteger()
      onNodeWithTag(TAG_TEXT).assertTextEquals("Initial")
      repeat(2) {
        repeat(10) { goToNext(count) }
        repeat(10) { popBack(count) }
      }
    }
  }
}

data class TestResultScreen(val input: String) : Screen {
  data class State(val text: String, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class UpdateText(val text: String) : Event

    data object Forward : Event

    data object Back : Event
  }

  data class Result(val output: String) : PopResult
}

class TestResultPresenter(private val navigator: Navigator, private val screen: TestResultScreen) :
  Presenter<TestResultScreen.State> {

  @Composable
  override fun present(): TestResultScreen.State {
    var text by remember { mutableStateOf(screen.input) }
    val nextNavigator =
      rememberAnsweringNavigator(navigator, TestResultScreen.Result::class) { result ->
        text = result.output
      }

    return TestResultScreen.State(text) { event ->
      when (event) {
        TestResultScreen.Event.Forward -> {
          nextNavigator.goTo(TestResultScreen(text))
        }
        TestResultScreen.Event.Back -> {
          navigator.pop(TestResultScreen.Result(text))
        }
        is TestResultScreen.Event.UpdateText -> text = event.text
      }
    }
  }
}

@Composable
fun TestResultContent(state: TestResultScreen.State, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    BasicTextField(
      value = state.text,
      onValueChange = { value -> state.eventSink(TestResultScreen.Event.UpdateText(value)) },
      modifier = Modifier.testTag(TAG_TEXT),
    )

    BasicText(
      text = "Forward",
      modifier =
        Modifier.testTag(TAG_GO_NEXT).clickable { state.eventSink(TestResultScreen.Event.Forward) },
    )
    BasicText(
      text = "Back",
      modifier =
        Modifier.testTag(TAG_POP).clickable { state.eventSink(TestResultScreen.Event.Back) },
    )
  }
}
