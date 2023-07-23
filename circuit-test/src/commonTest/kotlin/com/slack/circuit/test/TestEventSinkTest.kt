package com.slack.circuit.test

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.slack.circuit.runtime.CircuitUiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(TestParameterInjector::class)
class TestEventSinkTest {
  @Test
  fun events() {
    TestEventSink<Event>().run {
      invoke(Event1)
      invoke(Event2)
      invoke(Event3)

      assertThat(events).isEqualTo(listOf(Event1, Event2, Event3))
    }
  }

  @Test
  fun `awaitFirstEvent - verify timeout`() = runTest {
    try {
      TestEventSink<Event>().awaitFirstEvent(TestEventSink.DEFAULT_TIMEOUT)
      fail("Timeout failed to interrupt await")
    } catch (_: TimeoutCancellationException) {
      // Nothing to do!
    }
  }

  @Test
  fun `awaitFirstEvent - return if first event received before timeout`() = runTest {
    val sink = TestEventSink<Event>()

    launch {
      delay(1.toDuration(DurationUnit.SECONDS))
      sink.awaitFirstEvent(TestEventSink.DEFAULT_TIMEOUT)
    }

    advanceTimeBy(2.toDuration(DurationUnit.SECONDS))

    sink(Event1)

    assertThat(sink.events).isEqualTo(listOf(Event1))
  }

  @Test
  fun assertEventCount() {
    TestEventSink<Event>().run {
      invoke(Event1)
      invoke(Event2)
      invoke(Event3)

      assertEventCount(3)
      assertThat(events).isEqualTo(listOf(Event1, Event2, Event3))
    }
  }

  @Test
  fun assertNoEvents() {
    TestEventSink<Event>().assertNoEvents()
  }

  @Test
  fun `assertEvent - verify single event`() {
    TestEventSink<Event>().run {
      invoke(Event1)

      assertEvent(Event1)
    }
  }

  @Test
  fun `assertEvent - fails if no events have been received`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().assertEvent(Event1)
    }
  }

  @Test
  fun `assertEvent - fails if too many events have been received`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)
        invoke(Event2)

        assertEvent(Event1)
      }
    }
  }

  @Test
  fun `assertEvent - fails if received and expected fail equality check`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)

        assertEvent(Event2)
      }
    }
  }

  @Test
  fun `assertEvent - verify single event using predicate`() {
    TestEventSink<Event>().run {
      invoke(Event1)

      assertEvent { actual -> Event1 == actual }
    }
  }

  @Test
  fun `assertEvent - fails using predicate if too many events have been received`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)
        invoke(Event2)

        assertEvent { actual -> Event1 == actual }
      }
    }
  }

  @Test
  fun `assertEvent - fails when predicate returns false`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)

        assertEvent { false }
      }
    }
  }

  @Test
  fun `assertEventAt - verify event at index`() {
    TestEventSink<Event>().run {
      invoke(Event1)
      invoke(Event2)
      invoke(Event3)

      assertEventAt(0, Event1)
      assertEventAt(1, Event2)
      assertEventAt(2, Event3)
    }
  }

  @Test
  fun `assertEventAt - verify event at index using predicate`() {
    val events = listOf(Event1, Event2, Event3)

    TestEventSink<Event>().run {
      events.forEach { invoke(it) }

      events.indices.forEach { i ->
        assertEventAt(i) { actual -> events[i] == actual }
      }
    }
  }

  @Test
  fun `assertEventAt - fails with predicate when no events have been received`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().assertEventAt(0) { actual -> Event1 == actual }
    }
  }

  @Test
  fun `assertEventAt - fails with predicate when called with an invalid index`(
    @TestParameter("-1","1") index: Int
  ) {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)
        assertEventAt(index) { actual -> Event1 == actual }
      }
    }
  }

  @Test
  fun `assertEventAt - fails when predicate returns false`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run{
        invoke(Event1)
        assertEventAt(0) { false }
      }
    }
  }

  @Test
  fun `assertEvents - verify multiple events`() {
    TestEventSink<Event>().run {
      invoke(Event1)
      invoke(Event2)
      invoke(Event3)

      assertEvents(Event1, Event2, Event3)
    }
  }

  @Test
  fun `assertEvents - fails when number of received and expected differ`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)
        invoke(Event2)
        invoke(Event3)

        assertEvents(Event1, Event2)
      }
    }
  }

  @Test
  fun `assertEvents - fails when received and expected fail equality check`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)
        invoke(Event2)
        invoke(Event3)

        assertEvents(Event1, Event2, Event2)
      }
    }
  }

  @Test
  fun `assertEvents - verify multiple events using predicate`() {
    val events = listOf(Event1, Event2, Event3)

    TestEventSink<Event>().run {
      events.forEach { invoke(it) }

      assertEvents { i, actual -> events[i] == actual }
    }
  }

  @Test
  fun `assertEvents - fails when predicate returns false`() {
    assertThrows(AssertionError::class.java) {
      TestEventSink<Event>().run {
        invoke(Event1)

        assertEvents { _,_ -> false }
      }
    }
  }
}

private sealed interface Event : CircuitUiEvent
private object Event1 : Event
private object Event2 : Event
private object Event3 : Event
