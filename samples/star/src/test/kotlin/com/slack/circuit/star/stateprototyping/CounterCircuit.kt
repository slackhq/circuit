// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.stateprototyping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

sealed interface CounterState : CircuitUiState {
  val count: Int

  /** An interface-only approach where all the events are defined as functions on the interface. */
  interface AsInterface : CounterState {
    fun increment()

    fun decrement()
  }

  /**
   * As an interface but implementing a common [EventSink] interface and funneling all events as
   * concrete [Event] classes
   */
  interface AsInterfaceWithEventSink : CounterState, EventSink<Event>

  /** As an abstract class with a helper constructor param for funneling events. */
  abstract class AsAbstractClassWithEventSink(
    eventSink: (Event) -> Unit = {},
  ) : CounterState, EventSink<Event> by eventSink(eventSink)

  /** As an abstract class with a helper constructor param for funneling events and properties. */
  abstract class AsAbstractClassWithEventSinkAndProperties(
    final override val count: Int,
    eventSink: (Event) -> Unit = {},
  ) : CounterState, EventSink<Event> by eventSink(eventSink)

  /** A standard data class implementation of a canonical Circuit state. */
  data class AsDataClass(
    override val count: Int,
    val eventSink: (Event) -> Unit = {},
  ) : CounterState
}

sealed interface Event : CircuitUiEvent {
  data object Increment : Event

  data object Decrement : Event
}

class AsInterfacePresenter : Presenter<CounterState.AsInterface> {
  @Composable
  override fun present(): CounterState.AsInterface {
    return remember {
      object : CounterState.AsInterface {
        override var count: Int by mutableIntStateOf(0)
          private set

        override fun increment() {
          count++
        }

        override fun decrement() {
          count--
        }
      }
    }
  }
}

class AsInterfaceWithEventSinkPresenter : Presenter<CounterState.AsInterfaceWithEventSink> {
  @Composable
  override fun present(): CounterState.AsInterfaceWithEventSink {
    return remember {
      object : CounterState.AsInterfaceWithEventSink {
        override var count: Int by mutableIntStateOf(0)
          private set

        override fun send(event: Event) {
          when (event) {
            is Event.Increment -> {
              count++
            }
            is Event.Decrement -> {
              count--
            }
          }
        }
      }
    }
  }
}

class AsAbstractClassWithEventSinkPresenter : Presenter<CounterState.AsAbstractClassWithEventSink> {
  @Composable
  override fun present(): CounterState.AsAbstractClassWithEventSink {
    return remember {
      object : CounterState.AsAbstractClassWithEventSink() {
        override var count: Int by mutableIntStateOf(0)
          private set

        override fun send(event: Event) {
          when (event) {
            is Event.Increment -> {
              count++
            }
            is Event.Decrement -> {
              count--
            }
          }
        }
      }
    }
  }
}

class AsAbstractClassWithEventSinkAndPropertiesPresenter :
  Presenter<CounterState.AsAbstractClassWithEventSinkAndProperties> {
  @Composable
  override fun present(): CounterState.AsAbstractClassWithEventSinkAndProperties {
    var count by remember { mutableIntStateOf(0) }
    // TODO could this be remembered too? Does the State get captured and thus keep it live?
    return object : CounterState.AsAbstractClassWithEventSinkAndProperties(count) {
      override fun send(event: Event) {
        when (event) {
          is Event.Increment -> {
            count++
          }
          is Event.Decrement -> {
            count--
          }
        }
      }
    }
  }
}

class AsDataClassPresenter : Presenter<CounterState.AsDataClass> {
  @Composable
  override fun present(): CounterState.AsDataClass {
    var count by remember { mutableIntStateOf(0) }
    return CounterState.AsDataClass(
      count = count,
      eventSink = { event ->
        when (event) {
          is Event.Increment -> {
            count++
          }
          is Event.Decrement -> {
            count--
          }
        }
      }
    )
  }
}

@RunWith(RobolectricTestRunner::class) // Necessary because ComposerImpl touches android.os.Trace
class Tests {
  @Test
  fun asInterface() = runTest {
    val presenter = AsInterfacePresenter()
    presenter.test {
      // Only ever get one item
      val state = awaitItem()
      assertThat(state.count).isEqualTo(0)
      state.increment()
      assertThat(state.count).isEqualTo(1)
      state.decrement()
      assertThat(state.count).isEqualTo(0)
    }
  }

  @Test
  fun asInterfaceWithEventSink() = runTest {
    val presenter = AsInterfaceWithEventSinkPresenter()
    presenter.test {
      // Only ever get one item
      val state = awaitItem()
      assertThat(state.count).isEqualTo(0)
      state.send(Event.Increment)
      assertThat(state.count).isEqualTo(1)
      state.send(Event.Decrement)
      assertThat(state.count).isEqualTo(0)
    }
  }

  @Test
  fun asAbstractClassWithEventSink() = runTest {
    val presenter = AsAbstractClassWithEventSinkPresenter()
    presenter.test {
      // Only ever get one item
      val state = awaitItem()
      assertThat(state.count).isEqualTo(0)
      state.send(Event.Increment)
      assertThat(state.count).isEqualTo(1)
      state.send(Event.Decrement)
      assertThat(state.count).isEqualTo(0)
    }
  }

  @Test
  fun asAbstractClassWithEventSinkAndProperties() = runTest {
    val presenter = AsAbstractClassWithEventSinkAndPropertiesPresenter()
    presenter.test {
      val state = awaitItem()
      assertThat(state.count).isEqualTo(0)
      state.send(Event.Increment)
      val state2 = awaitItem()
      assertThat(state2.count).isEqualTo(1)
      state.send(Event.Decrement)
      val state3 = awaitItem()
      assertThat(state3.count).isEqualTo(0)
    }
  }

  @Test
  fun asDataClass() = runTest {
    val presenter = AsDataClassPresenter()
    presenter.test {
      val state = awaitItem()
      assertThat(state.count).isEqualTo(0)
      state.eventSink(Event.Increment)
      val state2 = awaitItem()
      assertThat(state2.count).isEqualTo(1)
      state.eventSink(Event.Decrement)
      val state3 = awaitItem()
      assertThat(state3.count).isEqualTo(0)
    }
  }
}
