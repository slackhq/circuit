// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.stateprototyping

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.star.stateprototyping.CounterState.AsAbstractClassWithEventSink
import com.slack.circuit.star.stateprototyping.CounterState.AsDataClass
import com.slack.circuit.star.stateprototyping.CounterState.AsInterface
import com.slack.circuit.star.stateprototyping.CounterState.AsInterfaceWithEventSink
import com.slack.circuit.star.stateprototyping.CounterState.AsPokoClass
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner

sealed interface CounterState : CircuitUiState {
  val count: Int

  /** An interface-only approach where all the events are defined as functions on the interface. */
  interface AsInterface : CounterState {
    // TODO should these have empty defaults for easier testing?
    fun increment() {}

    fun decrement() {}
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

  /**
   * As a Poko class with a helper constructor param for funneling events and properties.
   *
   * Note that Poko isn't actually enabled on the project, but would use a class like this.
   *
   * See https://github.com/drewhamilton/Poko
   */
  // @Poko
  class AsPokoClass(
    override val count: Int,
    eventSink: (Event) -> Unit = {},
  ) : CounterState, EventSink<Event> by eventSink(eventSink)

  /** As a standard data class implementation of a canonical Circuit state. */
  data class AsDataClass(
    override val count: Int,
    val eventSink: (Event) -> Unit = {},
  ) : CounterState
}

sealed interface Event : CircuitUiEvent {
  data object Increment : Event

  data object Decrement : Event
}

class AsInterfacePresenter : Presenter<AsInterface> {
  @Composable
  override fun present(): AsInterface {
    return remember {
      object : AsInterface {
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

class AsInterfaceWithEventSinkPresenter : Presenter<AsInterfaceWithEventSink> {
  @Composable
  override fun present(): AsInterfaceWithEventSink {
    return remember {
      object : AsInterfaceWithEventSink {
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

class AsAbstractClassWithEventSinkPresenter : Presenter<AsAbstractClassWithEventSink> {
  @Composable
  override fun present(): AsAbstractClassWithEventSink {
    return remember {
      object : AsAbstractClassWithEventSink() {
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

class AsPokoClassPresenter : Presenter<AsPokoClass> {
  @Composable
  override fun present(): AsPokoClass {
    var count by remember { mutableIntStateOf(0) }
    // TODO could this be remembered too? Does the State get captured and thus keep it live?
    return AsPokoClass(count) { event ->
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

class AsDataClassPresenter : Presenter<AsDataClass> {
  @Composable
  override fun present(): AsDataClass {
    var count by remember { mutableIntStateOf(0) }
    return AsDataClass(
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

// Robolectric is necessary because ComposerImpl touches android.os.Trace
@RunWith(RobolectricTestRunner::class)
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
  fun asPokoClass() = runTest {
    val presenter = AsPokoClassPresenter()
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

abstract class BaseUiTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Composable
  fun Counter(state: CounterState, modifier: Modifier = Modifier) {
    Column(modifier) {
      Button(
        onClick = {
          when (state) {
            is AsInterface -> state.increment()
            is AsInterfaceWithEventSink -> state.send(Event.Increment)
            is AsAbstractClassWithEventSink -> state.send(Event.Increment)
            is AsPokoClass -> state.send(Event.Increment)
            is AsDataClass -> state.eventSink(Event.Increment)
          }
        }
      ) {
        Text(text = "Increment")
      }
      Text(text = "Count: ${state.count}", modifier = Modifier.testTag("COUNT"))
      Button(
        onClick = {
          when (state) {
            is AsInterface -> state.decrement()
            is AsInterfaceWithEventSink -> state.send(Event.Decrement)
            is AsAbstractClassWithEventSink -> state.send(Event.Decrement)
            is AsPokoClass -> state.send(Event.Decrement)
            is AsDataClass -> state.eventSink(Event.Decrement)
          }
        }
      ) {
        Text(text = "Decrement")
      }
    }
  }
}

/**
 * Tests that demonstrate what it's like standing up new functional [Counter] UIs with presenters.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class FunctionalUiTest(private val presenter: Presenter<CounterState>) : BaseUiTest() {
  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun data(): List<Array<Any>> {
      return listOf(
        arrayOf(AsInterfacePresenter()),
        arrayOf(AsInterfaceWithEventSinkPresenter()),
        arrayOf(AsAbstractClassWithEventSinkPresenter()),
        arrayOf(AsPokoClassPresenter()),
        arrayOf(AsDataClassPresenter()),
      )
    }
  }

  @Test
  fun functionalTest() {
    composeTestRule.run {
      setContent {
        val state = presenter.present()
        Counter(state)
      }

      println(onRoot().printToString())

      onNodeWithText("Count").assertTextContains("0")
      onNodeWithText("Increment").performClick()
      onNodeWithText("Count").assertTextContains("1")
      onNodeWithText("Decrement").performClick()
      onNodeWithText("Count").assertTextContains("0")
    }
  }
}

/**
 * Tests that demonstrate what it's like standing up new [CounterState] instances manually. This
 * reflects how simple UI tests without a presenter, previews, or snapshot tests would work.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class StaticUiTest(val state: (count: Int) -> CounterState) : BaseUiTest() {
  companion object {
    @Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun data(): List<Array<Any>> {
      return listOf(
        arrayOf({ count: Int ->
          object : AsInterface {
            override val count = count
          }
        }),
        arrayOf({ count: Int ->
          object : AsInterfaceWithEventSink, EventSink<Event> by fakeSink() {
            override val count = count
          }
        }),
        arrayOf({ count: Int ->
          object : AsAbstractClassWithEventSink(), EventSink<Event> by fakeSink() {
            override val count = count
          }
        }),
        arrayOf(::AsPokoClass),
        // Standard data class state. Simplest to stand up
        arrayOf(::AsDataClass),
      )
    }
  }

  @Test
  fun simpleTest() {
    composeTestRule.run {
      var count by mutableStateOf(state(0))
      setContent { Counter(count) }

      println(onRoot().printToString())

      onNodeWithText("Count").assertTextContains("0")
      count = state(1)
      onNodeWithText("Count").assertTextContains("1")
    }
  }
}
