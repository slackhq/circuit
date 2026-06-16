// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.test

import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class TestOuterEventSinkTest {

  sealed interface TestOuterEvent : SubCircuitOuterEvent {
    data class Navigate(val destination: String) : TestOuterEvent

    data object Dismiss : TestOuterEvent
  }

  @Test
  fun awaitEventReturnsTheEmittedEvent() = runTest {
    val sink = TestOuterEventSink<TestOuterEvent>()

    sink(TestOuterEvent.Navigate("details"))

    val event = sink.awaitEvent()
    assertEquals(TestOuterEvent.Navigate("details"), event)
  }

  @Test
  fun multipleEventsCanBeAwaitedInOrder() = runTest {
    val sink = TestOuterEventSink<TestOuterEvent>()

    sink(TestOuterEvent.Navigate("first"))
    sink(TestOuterEvent.Dismiss)
    sink(TestOuterEvent.Navigate("second"))

    assertEquals(TestOuterEvent.Navigate("first"), sink.awaitEvent())
    assertEquals(TestOuterEvent.Dismiss, sink.awaitEvent())
    assertEquals(TestOuterEvent.Navigate("second"), sink.awaitEvent())
  }

  @Test
  fun assertNoEventsPassesWhenNoEventsEmitted() {
    val sink = TestOuterEventSink<TestOuterEvent>()

    sink.assertNoEvents()
  }

  @Test
  fun assertNoEventsFailsWhenEventsWereEmitted() {
    val sink = TestOuterEventSink<TestOuterEvent>()

    sink(TestOuterEvent.Dismiss)

    assertFailsWith<AssertionError> { sink.assertNoEvents() }
  }
}
