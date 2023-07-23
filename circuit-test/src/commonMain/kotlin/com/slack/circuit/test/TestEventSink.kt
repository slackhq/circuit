package com.slack.circuit.test

import com.slack.circuit.runtime.CircuitUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * A test event sink that records events from a Circuit UI and allows making assertions about them.
 *
 * @see CircuitUiEvent
 * Type parameters: <Event> - the event value type
 */
public class TestEventSink<Event : CircuitUiEvent> : (Event) -> Unit {
  private val receivedEvents = mutableListOf<Event>()
  private val channel = Channel<Unit>(Channel.CONFLATED)

  public val events: List<Event>
    get() = receivedEvents

  public override fun invoke(event: Event) {
    channel.trySend(Unit)
    receivedEvents.add(event)
  }

  public suspend fun awaitFirstEvent(duration: Duration = DEFAULT_TIMEOUT): TestEventSink<Event> =
    withTimeout(duration) {
      channel.receive()
      this@TestEventSink
    }

  public fun assertEventCount(count: Int): TestEventSink<Event> {
    if (receivedEvents.size != count) {
      throw AssertionError(
        """
          expected: $count
          got: ${receivedEvents.size}; Value counts differ
        """.trimIndent()
      )
    }

    return this
  }

  public fun assertNoEvents(): TestEventSink<Event> = assertEventCount(0)

  public fun assertEvent(event: Event): TestEventSink<Event> {
    if (receivedEvents.size != 1) {
      throw AssertionError(
        """
          expected: ${valueAndClass(event)}
          got: $receivedEvents
        """.trimIndent()
      )
    }

    val received = receivedEvents[0]
    if (received != event) {
      throw AssertionError(
        """
          expected: ${valueAndClass(event)}
          got: ${valueAndClass(received)}
        """.trimIndent()
      )
    }

    return this
  }

  public fun assertEvent(predicate: (Event) -> Boolean): TestEventSink<Event> {
    assertEventAt(0, predicate)

    if (receivedEvents.size > 1) {
      throw AssertionError("The first value passed the predicate but this sink received more than one event")
    }

    return this
  }

  public fun assertEventAt(index: Int, event: Event): TestEventSink<Event> =
    assertEventAt(index) { received -> received == event }

  public fun assertEventAt(index: Int, predicate: (Event) -> Boolean): TestEventSink<Event> {
    if (receivedEvents.isEmpty()) throw AssertionError("No events")

    if (index < 0 || receivedEvents.size <= index) {
      throw AssertionError("Index $index is out of range [0, ${receivedEvents.size})")
    }

    val received = receivedEvents[index]
    if (!predicate(received)) {
      throw AssertionError("Value ${valueAndClass(received)} at index $index did not pass the predicate check")
    }

    return this
  }

  public fun assertEvents(vararg events: Event): TestEventSink<Event> {
    if (receivedEvents.size != events.size) {
      throw AssertionError(
        """
          expected: ${events.size} ${events.contentToString()}
          got: ${receivedEvents.size} $receivedEvents; Value count differs
        """.trimIndent()
      )
    }

    receivedEvents.forEachIndexed { i, received ->
      val event = events[i]
      if (received != event) {
        throw AssertionError(
          """
            expected: ${valueAndClass(event)}
            got: ${valueAndClass(received)}; Values at index $i differ
          """.trimIndent()
        )
      }
    }

    return this
  }

  public fun assertEvents(predicate: (Int, Event) -> Boolean): TestEventSink<Event> {
    receivedEvents.forEachIndexed { i, received ->
      if (!predicate(i, received)) {
        throw AssertionError("Value ${valueAndClass(received)} at index $i did not pass the predicate check")
      }
    }

    return this
  }

  private fun valueAndClass(obj: Any?): String =
    when (obj) {
      null -> "null"
      else -> "$obj (${obj.javaClass.simpleName})"
    }

  public companion object {
    public val DEFAULT_TIMEOUT: Duration = 10.toDuration(DurationUnit.SECONDS)
  }
}