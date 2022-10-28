/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.launchMolecule
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
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

  override fun onState(state: Any) {
    states.add(state)
  }

  class Factory : EventListener.Factory {
    val listeners = mutableMapOf<Screen, RecordingEventListener>()

    fun get(screen: Screen): RecordingEventListener =
      listeners[screen] ?: (RecordingEventListener().also { listeners[screen] = it })

    override fun create(screen: Screen): EventListener = get(screen)
  }
}
