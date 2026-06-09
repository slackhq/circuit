// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SubPresenterTestExtensionsTest {

  sealed interface TestOuterEvent : SubCircuitOuterEvent {
    data class Navigate(val destination: String) : TestOuterEvent

    data object Dismiss : TestOuterEvent
  }

  sealed interface TestEvent {
    data object IncrementCounter : TestEvent

    data object TriggerNavigation : TestEvent

    data object TriggerDismiss : TestEvent
  }

  data class TestState(val counter: Int, val eventSink: (TestEvent) -> Unit) : SubCircuitUiState

  class TestSubPresenter : SubPresenter<TestOuterEvent, TestState> {
    @Composable
    override fun present(outerEventSink: (TestOuterEvent) -> Unit): TestState {
      var counter by remember { mutableIntStateOf(0) }

      return TestState(
        counter = counter,
        eventSink = { event ->
          when (event) {
            TestEvent.IncrementCounter -> counter++
            TestEvent.TriggerNavigation -> outerEventSink(TestOuterEvent.Navigate("details"))
            TestEvent.TriggerDismiss -> outerEventSink(TestOuterEvent.Dismiss)
          }
        },
      )
    }
  }

  @Test
  fun testExtensionProvidesInitialState() = runTest {
    val presenter = TestSubPresenter()

    presenter.test {
      val state = awaitItem()
      assertEquals(0, state.counter)
    }
  }

  @Test
  fun testExtensionReceivesStateUpdates() = runTest {
    val presenter = TestSubPresenter()

    presenter.test {
      val initialState = awaitItem()
      assertEquals(0, initialState.counter)

      initialState.eventSink(TestEvent.IncrementCounter)

      val updatedState = awaitItem()
      assertEquals(1, updatedState.counter)
    }
  }

  @Test
  fun testExtensionCapturesOuterEvents() = runTest {
    val presenter = TestSubPresenter()

    presenter.test {
      val state = awaitItem()

      state.eventSink(TestEvent.TriggerNavigation)

      assertEquals(TestOuterEvent.Navigate("details"), outerEvents.awaitEvent())
    }
  }

  @Test
  fun testExtensionCapturesMultipleOuterEvents() = runTest {
    val presenter = TestSubPresenter()

    presenter.test {
      val state = awaitItem()

      state.eventSink(TestEvent.TriggerNavigation)
      state.eventSink(TestEvent.TriggerDismiss)

      assertEquals(TestOuterEvent.Navigate("details"), outerEvents.awaitEvent())
      assertEquals(TestOuterEvent.Dismiss, outerEvents.awaitEvent())
    }
  }

  @Test
  fun testExtensionWithCustomOuterEventSink() = runTest {
    val presenter = TestSubPresenter()
    val customSink = TestOuterEventSink<TestOuterEvent>()

    presenter.test(outerEventSink = customSink) {
      val state = awaitItem()

      state.eventSink(TestEvent.TriggerNavigation)

      assertEquals(TestOuterEvent.Navigate("details"), customSink.awaitEvent())
    }
  }

  @Test
  fun subPresenterTestOfWorksWithCustomPresentFunction() = runTest {
    val presenter = TestSubPresenter()

    subPresenterTestOf<TestOuterEvent, TestState>(presentFunction = { presenter.present {} }) {
      val state = awaitItem()
      assertEquals(0, state.counter)
    }
  }
}
