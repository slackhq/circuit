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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import com.slack.circuit.backstack.SaveableBackStack
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
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG_TEXT = "text"
private const val TAG_GO_NEXT_NO_ANSWER = "nextNoAnswer"

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

  @Test
  fun simplePushAndPop() {
    composeTestRule.run {
      setUpTestContent()

      // Push 10 screens up, incrementing the count and passing on its updated value each time
      // Then pop 10 screens down, decrementing the count and passing on its updated value each time
      // Then do it one more time to make sure we can re-launch from the same screen
      onNodeWithTag(TAG_TEXT).assertTextEquals("root")
      repeat(2) {
        repeat(10) { innerIteration -> goToNext(answer = true, count = innerIteration) }
        repeat(10) { innerIteration -> popBack(expectAnswer = true, count = innerIteration) }
      }
    }
  }

  @Test
  fun mixedAnswers() {
    // Simulate a journey where some screens navigate with answer expectations and some don't
    composeTestRule.run {
      val backStack = setUpTestContent()

      onNodeWithTag(TAG_TEXT).assertTextEquals("root")
      goToNext(answer = true, 0)
      goToNext(answer = false, 1)
      goToNext(answer = true, 2)
      goToNext(answer = false, 3)
      dumpState(backStack)
      // Pop back once. No answer expected so its value doesn't update
      popBack(expectAnswer = false, 2)
      dumpState(backStack)
      // Pop again. Answer expected this time, incremented 2 + 1
      popBack(expectAnswer = true, 3)
      dumpState(backStack)
      // Pop again. No answer expected so its value doesn't update
      popBack(expectAnswer = false, 0)
      dumpState(backStack)
      // Last pop. Answer expected, incremented 0 + 1
      popBack(expectAnswer = false, 1)
      dumpState(backStack)
    }
  }

  private fun ComposeContentTestRule.setUpTestContent(): SaveableBackStack {
    lateinit var returnedStack: SaveableBackStack
    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack {
          push(TestResultScreen("root", answer = false))
          returnedStack = this
        }
        val navigator =
          rememberCircuitNavigator(
            backStack = backStack,
            onRootPop = {}, // no-op for tests
          )
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
    return returnedStack
  }

  private fun ComposeContentTestRule.goToNext(answer: Boolean, count: Int) {
    println("➕ Pushing next from $count to ${count + 1}")
    with(onNodeWithTag(TAG_TEXT)) {
      performTextClearance()
      performTextInput(count.toString())
    }
    if (answer) {
      onNodeWithTag(TAG_GO_NEXT).performClick()
    } else {
      onNodeWithTag(TAG_GO_NEXT_NO_ANSWER).performClick()
    }
    with(onNodeWithTag(TAG_TEXT)) {
      // Assert we got the new count on the other side
      assertTextEquals(count.toString())
      performTextClearance()
      performTextInput((count + 1).toString())
    }
  }

  private fun ComposeContentTestRule.popBack(expectAnswer: Boolean, count: Int) {
    val incremented = getCurrentText().toInt() + 1
    println("➖ Popping back from $count to ${if (expectAnswer) incremented else count}")
    with(onNodeWithTag(TAG_TEXT)) {
      performTextClearance()
      performTextInput(incremented.toString())
    }
    onNodeWithTag(TAG_POP).performClick()
    with(onNodeWithTag(TAG_TEXT)) {
      try {
        if (expectAnswer) {
          assertTextEquals(incremented.toString())
        } else {
          assertTextEquals(count.toString())
        }
      } catch (_: Throwable) {
        fail("Expected '${if (expectAnswer) incremented else count}', got '${getCurrentText()}'")
      }
    }
  }

  private fun ComposeContentTestRule.getCurrentText(): String {
    onNodeWithTag(TAG_TEXT).apply {
      val node = fetchSemanticsNode()
      val actual = mutableListOf<String>()
      node.config.getOrNull(SemanticsProperties.EditableText)?.let { actual.add(it.text) }
      node.config.getOrNull(SemanticsProperties.Text)?.let {
        actual.addAll(it.map { anStr -> anStr.text })
      }
      return actual.first()
    }
  }

  private fun ComposeContentTestRule.dumpState(backStack: SaveableBackStack) {
    val state = buildString {
      appendLine("BackStack:")
      appendLine("  size: ${backStack.size}")
      appendLine("  top: ${backStack.topRecord?.key}")
      appendLine("  records:")
      // Append the string as a square diagram
      val tableString =
        table {
            cellStyle {
              // These options affect the style of all cells contained within the table.
              border = true
              alignment = TextAlignment.MiddleLeft
            }
            header {
              row {
                cellStyle { alignment = TextAlignment.MiddleCenter }
                for ((i, record) in backStack.iterator().withIndex()) {
                  if (i == 0) {
                    cell("${record.key.take(8)} (top)")
                  } else {
                    cell(record.key.take(8))
                  }
                }
              }
            }
            row {
              for ((i, record) in backStack.iterator().withIndex()) {
                @Suppress("invisible_member", "invisible_reference")
                val stateString =
                  """
                    ${record.screen::class.simpleName}
                    input=${(record.screen as TestResultScreen).input}
                    ⬅ expectingResult=${record.expectingResult()}
                    value=${if (i == 0) getCurrentText() else "undefined"}
                  """
                    .trimIndent()
                cell(stateString)
              }
            }
          }
          .toString()
      appendLine(tableString.prependIndent("    "))
    }
    println(state)
  }
}

data class TestResultScreen(val input: String, val answer: Boolean) : Screen {
  data class State(val text: String, val eventSink: (Event) -> Unit) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class UpdateText(val text: String) : Event

    data object Forward : Event

    data object ForwardNoAnswer : Event

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
          nextNavigator.goTo(TestResultScreen(text, answer = true))
        }
        TestResultScreen.Event.Back -> {
          if (screen.answer) {
            navigator.pop(TestResultScreen.Result(text))
          } else {
            navigator.pop()
          }
        }
        is TestResultScreen.Event.UpdateText -> text = event.text
        TestResultScreen.Event.ForwardNoAnswer -> {
          navigator.goTo(TestResultScreen(text, answer = false))
        }
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
      text = "Forward (no answer)",
      modifier =
        Modifier.testTag(TAG_GO_NEXT_NO_ANSWER).clickable {
          state.eventSink(TestResultScreen.Event.ForwardNoAnswer)
        },
    )
    BasicText(
      text = "Back",
      modifier =
        Modifier.testTag(TAG_POP).clickable { state.eventSink(TestResultScreen.Event.Back) },
    )
  }
}
