// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import com.slack.circuit.runtime.CircuitUiEvent
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

/**
 * A test event sink that records events from a Circuit UI and allows making assertions about them.
 *
 * Note: this class was heavily influenced by RxJava3's
 * [BaseTestConsumer][https://github.com/ReactiveX/RxJava/blob/v3.1.6/src/main/java/io/reactivex/rxjava3/observers/BaseTestConsumer.java].
 *
 * @see CircuitUiEvent
 */
public class TestEventSink<Event : CircuitUiEvent> : (Event) -> Unit {
  private val receivedEvents = mutableListOf<Event>()
  private val channel = Channel<Unit>(Channel.CONFLATED)

  /** The list of received events */
  public val events: List<Event>
    get() = receivedEvents

  /**
   * Sends the specified [event] to this TestEventSink.
   *
   * @param event the [Event] being added to the sink
   */
  public override fun invoke(event: Event) {
    channel.trySend(Unit)
    receivedEvents.add(event)
  }

  /**
   * Awaits the specified [duration] for the sink to receive an event. If an event is not received
   * within [duration], this method will throw a [TimeoutCancellationException].
   *
   * Note: this method has a default duration of [DEFAULT_TIMEOUT].
   *
   * @see DEFAULT_TIMEOUT
   * @see TimeoutCancellationException
   */
  public suspend fun awaitFirstEvent(duration: Duration = DEFAULT_TIMEOUT): TestEventSink<Event> =
    withTimeout(duration) {
      channel.receive()
      this@TestEventSink
    }

  /**
   * Assert that this TestEventSink has received [count] events.
   *
   * @param count the number of expected events
   * @return this
   */
  public fun assertEventCount(count: Int): TestEventSink<Event> {
    if (receivedEvents.size != count) {
      throw AssertionError(
        """
          expected: $count
          got: ${receivedEvents.size}; Value counts differ
        """
          .trimIndent()
      )
    }

    return this
  }

  /**
   * Assert that this TestEventSink has not received any events.
   *
   * @return this
   */
  public fun assertNoEvents(): TestEventSink<Event> = assertEventCount(0)

  /**
   * Assert that this TestEventSink as received exactly one [Event] and that it equals [event].
   *
   * @param event the expected [Event]
   * @return this
   */
  public fun assertEvent(event: Event): TestEventSink<Event> {
    if (receivedEvents.size != 1) {
      throw AssertionError(
        """
          expected: ${valueAndClass(event)}
          got: $receivedEvents
        """
          .trimIndent()
      )
    }

    val received = receivedEvents[0]
    if (received != event) {
      throw AssertionError(
        """
          expected: ${valueAndClass(event)}
          got: ${valueAndClass(received)}
        """
          .trimIndent()
      )
    }

    return this
  }

  /**
   * Assert that this TestEventSink has received exactly one [Event] and that running [predicate]
   * with that event returns true.
   *
   * @param predicate the function used to determine equality with the received event
   * @return this
   */
  public fun assertEvent(predicate: (Event) -> Boolean): TestEventSink<Event> {
    assertEventAt(0, predicate)

    if (receivedEvents.size > 1) {
      throw AssertionError(
        "The first value passed the predicate but this sink received more than one event"
      )
    }

    return this
  }

  /**
   * Assert that this TestEventSink received an event at [index] which is equal to the given
   * [event].
   *
   * @param index the index of the received event to be asserted
   * @param event the expected [Event] to be compared to the received event at [index]
   * @return this
   */
  public fun assertEventAt(index: Int, event: Event): TestEventSink<Event> =
    assertEventAt(index) { received -> received == event }

  /**
   * Assert this TestEventSink received an event at [index] and that running [predicate] with that
   * event returns true.
   *
   * @param index the index of the received event to be asserted
   * @param predicate the function used to determine equality with the received event at [index]
   * @return this
   */
  public fun assertEventAt(index: Int, predicate: (Event) -> Boolean): TestEventSink<Event> {
    if (receivedEvents.isEmpty()) throw AssertionError("No events")

    if (index < 0 || receivedEvents.size <= index) {
      throw AssertionError("Index $index is out of range [0, ${receivedEvents.size})")
    }

    val received = receivedEvents[index]
    if (!predicate(received)) {
      throw AssertionError(
        "Value ${valueAndClass(received)} at index $index did not pass the predicate check"
      )
    }

    return this
  }

  /**
   * Assert that this TestEventSink received only the specified [events] in the specified order.
   *
   * @param events the list of expected [events][Event] that is compared to the received events
   * @return this
   */
  public fun assertEvents(vararg events: Event): TestEventSink<Event> {
    if (receivedEvents.size != events.size) {
      throw AssertionError(
        """
          expected: ${events.size} ${events.contentToString()}
          got: ${receivedEvents.size} $receivedEvents; Value count differs
        """
          .trimIndent()
      )
    }

    receivedEvents.forEachIndexed { i, received ->
      val event = events[i]
      if (received != event) {
        throw AssertionError(
          """
            expected: ${valueAndClass(event)}
            got: ${valueAndClass(received)}; Values at index $i differ
          """
            .trimIndent()
        )
      }
    }

    return this
  }

  /**
   * Assert that [predicate] returns true for each event received by this TestEventSink.
   *
   * @param predicate the function used to determine equality with each received event
   * @return this
   */
  public fun assertEvents(predicate: (Int, Event) -> Boolean): TestEventSink<Event> {
    receivedEvents.forEachIndexed { i, received ->
      if (!predicate(i, received)) {
        throw AssertionError(
          "Value ${valueAndClass(received)} at index $i did not pass the predicate check"
        )
      }
    }

    return this
  }

  public companion object {
    public val DEFAULT_TIMEOUT: Duration = 10.toDuration(DurationUnit.SECONDS)

    private fun valueAndClass(obj: Any): String = "$obj (${obj.javaClass.simpleName})"
  }
}
