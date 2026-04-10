// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_TAG = "count"
private const val EXPECTED_RECOMPOSITIONS = 3

@RunWith(ComposeUiTestRunner::class)
class PausableStateTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun presentWithLifecycle_dataClass() = runTest {
    val presenter = DataClassPresenter()
    with(composeTestRule) {
      // https://github.com/androidx/androidx/pull/800
      @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
      setContent {
        val state = presenter.presentWithLifecycle()
        BasicText(
          text = "${state.count}",
          modifier = Modifier.testTag(TEST_TAG).clickable { state.eventSink() },
        )
      }
      with(onNodeWithTag(TEST_TAG)) {
        assertTextEquals("0")
        performClick()
        assertTextEquals("1")
        performClick()
        assertTextEquals("2")
      }
      assertEquals(EXPECTED_RECOMPOSITIONS, presenter.compositionCount)
    }
  }

  @Test
  fun presentWithLifecycle_class() = runTest {
    val presenter = ClassPresenter()
    with(composeTestRule) {
      // https://github.com/androidx/androidx/pull/800
      @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
      setContent {
        val state = presenter.presentWithLifecycle()
        BasicText(
          text = "${state.count}",
          modifier = Modifier.testTag(TEST_TAG).clickable { state.eventSink() },
        )
      }
      with(onNodeWithTag(TEST_TAG)) {
        assertTextEquals("0")
        performClick()
        assertTextEquals("1")
        performClick()
        assertTextEquals("2")
      }
      assertEquals(EXPECTED_RECOMPOSITIONS, presenter.compositionCount)
    }
  }

  class DataClassPresenter : Presenter<DataClassPresenter.TestState> {
    var compositionCount = 0

    @Composable
    override fun present(): TestState {
      var count by remember { mutableIntStateOf(0) }
      SideEffect { compositionCount++ }
      return TestState(count = count) { count++ }
    }

    data class TestState(val count: Int, val eventSink: () -> Unit) : CircuitUiState
  }

  class ClassPresenter : Presenter<ClassPresenter.TestState> {
    var compositionCount = 0

    @Composable
    override fun present(): TestState {
      var count by remember { mutableIntStateOf(0) }
      SideEffect { compositionCount++ }
      return TestState(count = count) { count++ }
    }

    class TestState(val count: Int, val eventSink: () -> Unit) : CircuitUiState
  }
}
