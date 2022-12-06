// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.launchMolecule
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import java.time.ZoneOffset.UTC
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class EventListenerTest {

  @JvmField
  @Rule
  val timeout: Timeout =
    Timeout.builder().withTimeout(10, TimeUnit.SECONDS).withLookingForStuckThread(true).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun basicEventRecording() = runTest {
    val eventListenerFactory = RecordingEventListener.Factory()
    val state = mutableStateOf("State")

    val presenter = StringPresenter(state)
    val ui = StringUi()
    val circuitConfig =
      CircuitConfig.Builder()
        .addPresenterFactory { _, _, _ -> presenter }
        .addUiFactory { _, _ -> ScreenUi(ui) }
        .eventListenerFactory(eventListenerFactory)
        .build()

    backgroundScope.launchMolecule(Immediate) {
      CircuitContent(circuitConfig = circuitConfig, screen = TestScreen)
    }
    val (screen, listener) = eventListenerFactory.listeners.entries.first()
    assertThat(screen).isEqualTo(TestScreen)

    assertThat(listener.states.awaitItem()).isEqualTo(StringState("State"))
    state.value = "State2"
    assertThat(listener.states.awaitItem()).isEqualTo(StringState("State2"))
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

  @Composable override fun Content(state: StringState) {}
}

private class RecordingEventListener : EventListener {
  val states = Turbine<Any>(name = "recording event listener states")

  private fun log(message: String) {
    println("${LocalTime.now(UTC).toString().replace("T"," ")}: $message")
  }

  override fun onState(state: Any) {
    log("onState: $state")
    states.add(state)
  }

  override fun onBeforeCreatePresenter(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ) {
    log("onBeforeCreatePresenter: $screen")
  }

  override fun onAfterCreatePresenter(
    screen: Screen,
    navigator: Navigator,
    presenter: Presenter<*>?,
    context: CircuitContext,
  ) {
    log("onAfterCreatePresenter: $screen, $presenter")
  }

  override fun onBeforeCreateUi(screen: Screen, context: CircuitContext) {
    log("onBeforeCreateUi: $screen")
  }

  override fun onAfterCreateUi(screen: Screen, screenUi: ScreenUi?, context: CircuitContext) {
    log("onAfterCreateUi: $screen, $screenUi")
  }

  override fun onUnavailableContent(
    screen: Screen,
    presenter: Presenter<*>?,
    screenUi: ScreenUi?,
    context: CircuitContext,
  ) {
    error("onUnavailableContent: $screen, $presenter, $screenUi")
  }

  override fun onStartPresent() {
    log("onStartPresent")
  }

  override fun onDisposePresent() {
    log("onDisposePresent")
  }

  override fun onStartContent() {
    log("onStartContent")
  }

  override fun onDisposeContent() {
    log("onDisposeContent")
  }

  class Factory : EventListener.Factory {
    val listeners = mutableMapOf<Screen, RecordingEventListener>()

    fun get(screen: Screen): RecordingEventListener =
      listeners[screen] ?: (RecordingEventListener().also { listeners[screen] = it })

    override fun create(screen: Screen, context: CircuitContext) = get(screen)
  }
}
