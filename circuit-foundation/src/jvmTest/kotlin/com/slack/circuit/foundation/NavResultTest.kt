// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
      .addPresenter<TestResultScreen, TestResultScreen.State> { screen, navigator, _ ->
        TestResultPresenter(navigator, screen)
      }
      .addUi<TestResultScreen, TestResultScreen.State> { state, modifier ->
        TestResultContent(state, modifier)
      }
      .addPresenter<WrapperScreen, WrapperScreen.State> { _, navigator, _ ->
        WrapperPresenter(navigator)
      }
      .addUi<WrapperScreen, WrapperScreen.State> { state, modifier ->
        WrapperContent(state, modifier)
      }
      .build()

  @Test
  fun simplePushAndPop() {
    composeTestRule.run {
      setUpTestContent()

      // Push 10 screens up, incrementing the count and passing on its updated value each time
      // Then pop 10 screens down, decrementing the count and passing on its updated value each time
      // Then do it one more time to make sure we can re-launch from the same screen
      onNodeWithTag(TAG_TEXT).assertTextEquals("root")
      repeat(2) {
        repeat(10) { innerIteration -> goToNext(answer = true, nextCount = innerIteration) }
        repeat(10) { innerIteration -> popBack(expectAnswer = true, prevCount = innerIteration) }
      }
    }
  }

  @Test
  fun mixedAnswers() {
    // Simulate a journey where some screens navigate with answer expectations and some don't
    composeTestRule.run {
      val (backStack, resultHandler) = setUpTestContent()

      onNodeWithTag(TAG_TEXT).assertTextEquals("root")
      goToNext(answer = true, 0)
      goToNext(answer = false, 1)
      goToNext(answer = true, 2)
      goToNext(answer = false, 3)
      dumpState(backStack, resultHandler)
      // Pop back once. No answer expected so its value doesn't update
      popBack(expectAnswer = false, 2)
      dumpState(backStack, resultHandler)
      // Pop again. Answer expected this time, incremented 2 + 1
      popBack(expectAnswer = true, 3)
      dumpState(backStack, resultHandler)
      // Pop again. No answer expected so its value doesn't update
      popBack(expectAnswer = false, 0)
      dumpState(backStack, resultHandler)
      // Last pop. Answer expected, incremented 0 + 1
      popBack(expectAnswer = false, 1)
      dumpState(backStack, resultHandler)
    }
  }

  @Test
  fun onlyTheCallerGetsTheResult() {
    lateinit var backStack: SaveableBackStack
    lateinit var resultHandler: AnsweringResultHandler
    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuit) {
          backStack = rememberSaveableBackStack(WrapperScreen)
          resultHandler = rememberAnsweringResultHandler()
          val navigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = {}, // no-op for tests
            )
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            answeringResultHandler = resultHandler,
          )
        }
      }

      dumpState(backStack, resultHandler)
      goToNext(answer = true, 0)
      dumpState(backStack, resultHandler)
      popBack(expectAnswer = true, 1)
      dumpState(backStack, resultHandler)
    }
  }

  private fun ComposeContentTestRule.setUpTestContent():
    Pair<SaveableBackStack, AnsweringResultHandler> {
    lateinit var backStack: SaveableBackStack
    lateinit var resultHandler: AnsweringResultHandler
    setContent {
      CircuitCompositionLocals(circuit) {
        backStack = rememberSaveableBackStack(TestResultScreen("root", answer = false))
        resultHandler = rememberAnsweringResultHandler()
        val navigator =
          rememberCircuitNavigator(
            backStack = backStack,
            onRootPop = {}, // no-op for tests
          )
        NavigableCircuitContent(
          navigator = navigator,
          backStack = backStack,
          answeringResultHandler = resultHandler,
        )
      }
    }
    return backStack to resultHandler
  }

  private fun ComposeContentTestRule.goToNext(answer: Boolean, nextCount: Int) {
    println("➕ Pushing next from ${nextCount - 1} to $nextCount")
    with(onNodeWithTag(TAG_TEXT)) {
      performTextClearance()
      performTextInput(nextCount.toString())
    }
    if (answer) {
      onNodeWithTag(TAG_GO_NEXT).performClick()
    } else {
      onNodeWithTag(TAG_GO_NEXT_NO_ANSWER).performClick()
    }
    with(onNodeWithTag(TAG_TEXT)) {
      // Assert we got the new count on the other side
      assertTextEquals(nextCount.toString())
      performTextClearance()
      performTextInput((nextCount + 1).toString())
    }
  }

  private fun ComposeContentTestRule.popBack(expectAnswer: Boolean, prevCount: Int) {
    val incremented = getCurrentText().toInt() + 1
    println("➖ Popping back from $prevCount to ${if (expectAnswer) incremented else prevCount}")
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
          assertTextEquals(prevCount.toString())
        }
      } catch (_: Throwable) {
        fail(
          "Expected '${if (expectAnswer) incremented else prevCount}', got '${getCurrentText()}'"
        )
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

  private fun ComposeContentTestRule.dumpState(
    backStack: SaveableBackStack,
    resultHandler: AnsweringResultHandler,
  ) {
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
                val stateString =
                  """
                    ${record.screen::class.simpleName}
                    input=${(record.screen as? TestResultScreen)?.input}
                    ⬅ expectingResult=${resultHandler.expectingResult(record.key)}
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

    // See docs on UnscrupulousResultListenerEffect for details on these surrounding error calls
    rememberAnsweringNavigator<TestResultScreen.Result>(navigator) {
      error("This should never be called")
    }
    UnscrupulousResultListenerEffect()

    // The real next navigator
    val nextNavigator =
      rememberAnsweringNavigator<TestResultScreen.Result>(navigator) { result ->
        text = result.output
      }

    UnscrupulousResultListenerEffect()
    rememberAnsweringNavigator<TestResultScreen.Result>(navigator) {
      error("This should never be called")
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

/**
 * A composable that attempts to listen for a result that it should never receive. We pepper these
 * around in test presenters to ensure that results are only given to the original callers.
 */
@Composable
fun UnscrupulousResultListenerEffect() {
  val backStack = LocalBackStack.current!!
  val resultHandler = LocalAnsweringResultHandler.current!!
  LaunchedEffect(Unit) {
    resultHandler
      .awaitResult(backStack.topRecord?.key!!, "a key that definitely doesn't match")
      ?.let { error("This should never be called") }
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

data object WrapperScreen : Screen {
  data class State(val navigator: Navigator) : CircuitUiState
}

class WrapperPresenter(private val navigator: Navigator) : Presenter<WrapperScreen.State> {
  @Composable
  override fun present(): WrapperScreen.State {
    UnscrupulousResultListenerEffect()
    rememberAnsweringNavigator<TestResultScreen.Result>(navigator) {
      error("This should never be called")
    }
    return WrapperScreen.State(navigator)
  }
}

@Composable
fun WrapperContent(state: WrapperScreen.State, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    BasicText(text = "Wrapper")
    CircuitContent(screen = TestResultScreen("root", answer = true), navigator = state.navigator)
  }
}
