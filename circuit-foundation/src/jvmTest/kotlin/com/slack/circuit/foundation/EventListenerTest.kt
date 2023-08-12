// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import app.cash.molecule.RecompositionMode.Immediate
import app.cash.molecule.launchMolecule
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import java.time.LocalTime
import java.time.ZoneOffset.UTC
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class EventListenerTest {

  @JvmField
  @Rule
  val timeout: Timeout =
    Timeout.builder().withTimeout(10, TimeUnit.SECONDS).withLookingForStuckThread(true).build()

  @Test
  fun basicEventRecording() = runTest {
    val eventListenerFactory = RecordingEventListener.Factory()
    val state = mutableStateOf("State")

    val presenter = StringPresenter(state)
    val ui = StringUi()
    val circuit =
      Circuit.Builder()
        .addPresenterFactory { _, _, _ -> presenter }
        .addUiFactory { _, _ -> ui }
        .eventListenerFactory(eventListenerFactory)
        .build()

    backgroundScope.launchMolecule(Immediate) {
      CircuitContent(circuit = circuit, screen = TestScreen)
    }
    val (screen, listener) = eventListenerFactory.listeners.entries.first()
    assertThat(screen).isEqualTo(TestScreen)

    assertThat(listener.events.awaitItem()).isEqualTo(Event.Start)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnBeforeCreatePresenter)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnAfterCreatePresenter)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnBeforeCreateUi)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnAfterCreateUi)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnStartPresent)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnStartContent)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnState(StringState("State")))
    state.value = "State2"
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnState(StringState("State2")))

    backgroundScope.cancel()
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnDisposeContent)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.OnDisposePresent)
    assertThat(listener.events.awaitItem()).isEqualTo(Event.Dispose)
    listener.events.awaitComplete()
  }
}

private object TestScreen : Screen

private data class StringState(val value: String) : CircuitUiState

private class StringPresenter(val state: State<String>) : Presenter<StringState> {
  @Composable
  override fun present(): StringState {
    return StringState(state.value)
  }
}

private class StringUi : Ui<StringState> {

  @Composable override fun Content(state: StringState, modifier: Modifier) {}
}

private class RecordingEventListener(private val onDispose: () -> Unit) : EventListener {
  val events = Turbine<Event>(name = "recording callback events")

  override fun start() {
    log("start")
    events.add(Event.Start)
  }

  override fun onState(state: CircuitUiState) {
    log("onState: $state")
    events.add(Event.OnState(state))
  }

  override fun onBeforeCreatePresenter(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ) {
    log("onBeforeCreatePresenter: $screen")
    events.add(Event.OnBeforeCreatePresenter)
  }

  override fun onAfterCreatePresenter(
    screen: Screen,
    navigator: Navigator,
    presenter: Presenter<*>?,
    context: CircuitContext,
  ) {
    log("onAfterCreatePresenter: $screen, $presenter")
    events.add(Event.OnAfterCreatePresenter)
  }

  override fun onBeforeCreateUi(screen: Screen, context: CircuitContext) {
    log("onBeforeCreateUi: $screen")
    events.add(Event.OnBeforeCreateUi)
  }

  override fun onAfterCreateUi(screen: Screen, ui: Ui<*>?, context: CircuitContext) {
    log("onAfterCreateUi: $screen, $ui")
    events.add(Event.OnAfterCreateUi)
  }

  override fun onUnavailableContent(
    screen: Screen,
    presenter: Presenter<*>?,
    ui: Ui<*>?,
    context: CircuitContext,
  ) {
    events.add(Event.OnUnavailableContent)
    error("onUnavailableContent: $screen, $presenter, $ui")
  }

  override fun onStartPresent() {
    log("onStartPresent")
    events.add(Event.OnStartPresent)
  }

  override fun onDisposePresent() {
    log("onDisposePresent")
    events.add(Event.OnDisposePresent)
  }

  override fun onStartContent() {
    log("onStartContent")
    events.add(Event.OnStartContent)
  }

  override fun onDisposeContent() {
    log("onDisposeContent")
    events.add(Event.OnDisposeContent)
  }

  override fun dispose() {
    log("dispose")
    onDispose()
    events.add(Event.Dispose)
    events.close()
  }

  class Factory : EventListener.Factory {
    val listeners = mutableMapOf<Screen, RecordingEventListener>()

    fun get(screen: Screen): RecordingEventListener =
      listeners[screen]
        ?: run {
          log("Creating new RecordingEventListener for $screen")
          RecordingEventListener {
              log("Removing RecordingEventListener for $screen")
              listeners.remove(screen)
            }
            .also { listeners[screen] = it }
        }

    override fun create(screen: Screen, context: CircuitContext) = get(screen)
  }
}

private fun log(message: String) {
  println("${LocalTime.now(UTC).toString().replace("T"," ")}: $message")
}

private sealed interface Event {
  object Start : Event

  data class OnState(val state: CircuitUiState) : Event

  object OnBeforeCreatePresenter : Event

  object OnAfterCreatePresenter : Event

  object OnBeforeCreateUi : Event

  object OnAfterCreateUi : Event

  object OnUnavailableContent : Event

  object OnStartPresent : Event

  object OnDisposePresent : Event

  object OnStartContent : Event

  object OnDisposeContent : Event

  object Dispose : Event
}
