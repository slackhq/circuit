// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import kotlinx.parcelize.Parcelize

internal const val TAG_GO_NEXT = "go"
internal const val TAG_POP = "pop"
internal const val TAG_INCREASE_COUNT = "inc"
internal const val TAG_COUNT = "count"
internal const val TAG_LABEL = "label"

class NavigableCircuitRetainedStateTestActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuit =
      Circuit.Builder()
        .addPresenterFactory { screen, navigator, _ ->
          TestCountPresenter(screen as TestScreen, navigator, useKeys = false)
        }
        .addUiFactory { _, _ -> ui<TestState> { state, modifier -> TestContent(state, modifier) } }
        .build()

    setContent {
      CircuitCompositionLocals(circuit) {
        val backstack = rememberSaveableBackStack { push(TestScreen.ScreenA) }
        val navigator =
          rememberCircuitNavigator(
            backstack = backstack,
            onRootPop = {} // no-op for tests
          )
        NavigableCircuitContent(navigator = navigator, backstack = backstack)
      }
    }
  }

  internal sealed class TestScreen(val label: String) : Screen {

    @Parcelize data object ScreenA : TestScreen("A")

    @Parcelize data object ScreenB : TestScreen("B")

    @Parcelize data object ScreenC : TestScreen("C")
  }

  @Composable
  internal fun TestContent(state: TestState, modifier: Modifier) {
    Column(modifier = modifier) {
      BasicText(text = state.label, modifier = Modifier.testTag(TAG_LABEL))

      BasicText(text = "${state.count}", modifier = Modifier.testTag(TAG_COUNT))

      BasicText(
        text = "Increase count",
        modifier =
          Modifier.testTag(TAG_INCREASE_COUNT).clickable {
            state.eventSink(TestEvent.IncreaseCount)
          }
      )

      BasicText(
        text = "Pop",
        modifier = Modifier.testTag(TAG_POP).clickable { state.eventSink(TestEvent.PopNavigation) }
      )
      BasicText(
        text = "Go to next",
        modifier =
          Modifier.testTag(TAG_GO_NEXT).clickable { state.eventSink(TestEvent.GoToNextScreen) }
      )
    }
  }

  internal class TestCountPresenter(
    private val screen: TestScreen,
    private val navigator: Navigator,
    private val useKeys: Boolean,
  ) : Presenter<TestState> {
    @Composable
    override fun present(): TestState {
      var launchCount by rememberRetained(key = "count".takeIf { useKeys }) { mutableIntStateOf(0) }

      return TestState(launchCount, screen.label) { event ->
        when (event) {
          TestEvent.IncreaseCount -> launchCount++
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
  }

  internal data class TestState(
    val count: Int,
    val label: String,
    val eventSink: (TestEvent) -> Unit
  ) : CircuitUiState

  internal sealed interface TestEvent {
    data object GoToNextScreen : TestEvent

    data object PopNavigation : TestEvent

    data object IncreaseCount : TestEvent
  }
}
