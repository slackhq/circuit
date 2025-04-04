// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.internal.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui

object TestContentTags {
  const val TAG_ROOT = "root"
  const val TAG_GO_NEXT = "go"
  const val TAG_POP = "pop"
  const val TAG_INCREASE_COUNT = "inc"
  const val TAG_RESET_ROOT_ALPHA = "reset_alpha"
  const val TAG_RESET_ROOT_BETA = "reset_beta"
  const val TAG_COUNT = "count"
  const val TAG_LABEL = "label"
}

fun createTestCircuit(
  useKeys: Boolean = false,
  rememberType: TestCountPresenter.RememberType = TestCountPresenter.RememberType.Standard,
  saveStateOnRootChange: Boolean = false,
  restoreStateOnRootChange: Boolean = false,
  presenter: (Screen, Navigator) -> Presenter<*> = { screen, navigator ->
    TestCountPresenter(
      screen = screen as TestScreen,
      navigator = navigator,
      useKeys = useKeys,
      rememberType = rememberType,
      saveStateOnRootChange = saveStateOnRootChange,
      restoreStateOnRootChange = restoreStateOnRootChange,
    )
  },
): Circuit =
  Circuit.Builder()
    .addPresenterFactory { screen, navigator, _ -> presenter(screen, navigator) }
    .addUiFactory { _, _ -> ui<TestState> { state, modifier -> TestContent(state, modifier) } }
    .build()

sealed class TestScreen(val label: String) : Screen {
  @Parcelize data object ScreenA : TestScreen("A")

  @Parcelize data object ScreenB : TestScreen("B")

  @Parcelize data object ScreenC : TestScreen("C")

  @Parcelize data object RootAlpha : TestScreen("Root Alpha")

  @Parcelize data object RootBeta : TestScreen("Root Beta")
}

sealed class TestPopResult : PopResult {

  @Parcelize data object PopResultA : TestPopResult()

  @Parcelize data object PopResultB : TestPopResult()

  @Parcelize data object PopResultC : TestPopResult()

  @Parcelize data object PopResultRootAlpha : TestPopResult()

  @Parcelize data object PopResultRootBeta : TestPopResult()
}

@Composable
fun TestContent(state: TestState, modifier: Modifier = Modifier) {
  Column(modifier = modifier.testTag(TestContentTags.TAG_ROOT)) {
    BasicText(text = state.label, modifier = Modifier.testTag(TestContentTags.TAG_LABEL))

    BasicText(text = "${state.count}", modifier = Modifier.testTag(TestContentTags.TAG_COUNT))

    BasicText(
      text = "Increase count",
      modifier =
        Modifier.testTag(TestContentTags.TAG_INCREASE_COUNT).clickable {
          state.eventSink(TestEvent.IncreaseCount)
        },
    )
    BasicText(
      text = "Pop",
      modifier =
        Modifier.testTag(TestContentTags.TAG_POP).clickable {
          state.eventSink(TestEvent.PopNavigation)
        },
    )
    BasicText(
      text = "Go to next",
      modifier =
        Modifier.testTag(TestContentTags.TAG_GO_NEXT).clickable {
          state.eventSink(TestEvent.GoToNextScreen)
        },
    )

    BasicText(
      text = "Reset to Root Alpha",
      modifier =
        Modifier.testTag(TestContentTags.TAG_RESET_ROOT_ALPHA).clickable {
          state.eventSink(TestEvent.ResetRootAlpha)
        },
    )

    BasicText(
      text = "Reset to Root Beta",
      modifier =
        Modifier.testTag(TestContentTags.TAG_RESET_ROOT_BETA).clickable {
          state.eventSink(TestEvent.ResetRootBeta)
        },
    )
  }
}

class TestCountPresenter(
  private val screen: TestScreen,
  private val navigator: Navigator,
  private val useKeys: Boolean,
  private val rememberType: RememberType,
  private val saveStateOnRootChange: Boolean = false,
  private val restoreStateOnRootChange: Boolean = false,
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
        RememberType.ViewModel -> {
          rememberViewModel(key = "count".takeIf { useKeys })
        }
      }

    return TestState(count, screen.label) { event ->
      when (event) {
        TestEvent.IncreaseCount -> count++
        TestEvent.PopNavigation -> navigator.pop()
        TestEvent.GoToNextScreen -> {
          when (screen) {
            // Root screens all go to ScreenA
            TestScreen.RootAlpha -> navigator.goTo(TestScreen.ScreenA)
            TestScreen.RootBeta -> navigator.goTo(TestScreen.ScreenA)
            // Otherwise each screen navigates to the next screen
            TestScreen.ScreenA -> navigator.goTo(TestScreen.ScreenB)
            TestScreen.ScreenB -> navigator.goTo(TestScreen.ScreenC)
            else -> error("Can't navigate from $screen")
          }
        }
        TestEvent.ResetRootAlpha ->
          navigator.resetRoot(TestScreen.RootAlpha, saveStateOnRootChange, restoreStateOnRootChange)
        TestEvent.ResetRootBeta ->
          navigator.resetRoot(TestScreen.RootBeta, saveStateOnRootChange, restoreStateOnRootChange)
      }
    }
  }

  enum class RememberType {
    Standard,
    Retained,
    Saveable,
    ViewModel,
  }
}

@Composable expect fun rememberViewModel(key: String?): MutableIntState

data class TestState(val count: Int, val label: String, val eventSink: (TestEvent) -> Unit) :
  CircuitUiState

sealed interface TestEvent {
  data object GoToNextScreen : TestEvent

  data object PopNavigation : TestEvent

  data object IncreaseCount : TestEvent

  data object ResetRootAlpha : TestEvent

  data object ResetRootBeta : TestEvent
}
