// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui

object TestContentTags {
  const val TAG_GO_NEXT = "go"
  const val TAG_POP = "pop"
  const val TAG_INCREASE_COUNT = "inc"
  const val TAG_COUNT = "count"
  const val TAG_LABEL = "label"
}

fun createTestCircuit(
  useKeys: Boolean = false,
  rememberType: TestCountPresenter.RememberType = TestCountPresenter.RememberType.Standard,
): Circuit =
  Circuit.Builder()
    .addPresenterFactory { screen, navigator, _ ->
      TestCountPresenter(
        screen = screen as TestScreen,
        navigator = navigator,
        useKeys = useKeys,
        rememberType = rememberType
      )
    }
    .addUiFactory { _, _ -> ui<TestState> { state, modifier -> TestContent(state, modifier) } }
    .build()

sealed class TestScreen(val label: String) : Screen {
  @Parcelize data object ScreenA : TestScreen("A")

  @Parcelize data object ScreenB : TestScreen("B")

  @Parcelize data object ScreenC : TestScreen("C")
}

@Composable
fun TestContent(state: TestState, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    BasicText(text = state.label, modifier = Modifier.testTag(TestContentTags.TAG_LABEL))

    BasicText(text = "${state.count}", modifier = Modifier.testTag(TestContentTags.TAG_COUNT))

    BasicText(
      text = "Increase count",
      modifier =
        Modifier.testTag(TestContentTags.TAG_INCREASE_COUNT).clickable {
          state.eventSink(TestEvent.IncreaseCount)
        }
    )

    BasicText(
      text = "Pop",
      modifier =
        Modifier.testTag(TestContentTags.TAG_POP).clickable {
          state.eventSink(TestEvent.PopNavigation)
        }
    )
    BasicText(
      text = "Go to next",
      modifier =
        Modifier.testTag(TestContentTags.TAG_GO_NEXT).clickable {
          state.eventSink(TestEvent.GoToNextScreen)
        }
    )
  }
}

class TestCountPresenter(
  private val screen: TestScreen,
  private val navigator: Navigator,
  private val useKeys: Boolean,
  private val rememberType: RememberType,
) : Presenter<TestState> {
  @Composable
  override fun present(): TestState {
    var count by
      when (rememberType) {
        RememberType.Standard -> {
          remember { mutableIntStateOf(0) }
        }
        RememberType.Retained -> {
          rememberRetained(key = "count".takeIf { useKeys }) { mutableIntStateOf(0) }
        }
        RememberType.Saveable -> {
          rememberSaveable(key = "count".takeIf { useKeys }) { mutableIntStateOf(0) }
        }
      }

    return TestState(count, screen.label) { event ->
      when (event) {
        TestEvent.IncreaseCount -> count++
        TestEvent.PopNavigation -> navigator.pop()
        TestEvent.GoToNextScreen -> {
          when (screen) {
            is TestScreen.ScreenA -> navigator.goTo(TestScreen.ScreenB)
            is TestScreen.ScreenB -> navigator.goTo(TestScreen.ScreenC)
            else -> error("Can't navigate from $screen")
          }
        }
      }
    }
  }

  enum class RememberType {
    Standard,
    Retained,
    Saveable,
  }
}

data class TestState(val count: Int, val label: String, val eventSink: (TestEvent) -> Unit) :
  CircuitUiState

sealed interface TestEvent {
  data object GoToNextScreen : TestEvent

  data object PopNavigation : TestEvent

  data object IncreaseCount : TestEvent
}
