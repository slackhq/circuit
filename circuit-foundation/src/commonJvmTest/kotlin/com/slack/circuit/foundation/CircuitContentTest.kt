// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class CircuitContentTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Regression test for https://github.com/slackhq/circuit/pull/799
  @Test
  fun contentShouldRecomposeStateWhenPresenterInstanceChanges() {
    composeTestRule.run {
      val circuit =
        Circuit.Builder()
          .addPresenterFactory { screen, _, _ -> CountPresenter(screen as CountScreen) }
          .addUiFactory { _, _ ->
            ui<CountScreen.State> { state, modifier -> Count(state, modifier) }
          }
          .build()
      setContent { CircuitCompositionLocals(circuit) { CountStateBug() } }

      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
    }
  }

  @Parcelize
  private data class CountScreen(val count: Int) : Screen {
    data class State(val count: Int) : CircuitUiState
  }

  @Composable
  private fun CountStateBug(modifier: Modifier = Modifier) {
    var count by remember { mutableIntStateOf(0) }
    val screen = CountScreen(count)

    Column(modifier) {
      BasicText(
        text = "Increment count",
        modifier = Modifier.clickable { count++ }.testTag(TAG_INCREASE_COUNT),
      )
      CircuitContent(screen)
    }
  }

  @Composable
  private fun Count(state: CountScreen.State, modifier: Modifier) {
    BasicText(text = state.count.toString(), modifier = modifier.testTag(TAG_COUNT))
  }

  private class CountPresenter(private val screen: CountScreen) : Presenter<CountScreen.State> {
    @Composable
    override fun present(): CountScreen.State {
      // This is a contrived example to force the edge case.
      // TL;DR Keyless remembers remain bound across presenter instances in the eyes of compose.
      val myCount = remember { screen.count }
      return CountScreen.State(myCount)
    }
  }
}
