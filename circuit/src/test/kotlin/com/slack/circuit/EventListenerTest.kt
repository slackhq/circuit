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
import app.cash.molecule.moleculeFlow
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 32, manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class EventListenerTest {

  @JvmField
  @Rule
  val timeout: Timeout =
    Timeout.builder().withTimeout(10, TimeUnit.SECONDS).withLookingForStuckThread(true).build()

  @Test
  fun basicEventRecording() = runTest {
    val eventListenerFactory = RecordingEventListener.Factory()
    val state = mutableStateOf("State")
    val events = MutableSharedFlow<String>()

    val presenter = StringPresenter(state)
    val ui = StringUi(events)
    val circuit =
      Circuit.Builder()
        .addPresenterFactory { _, _ -> presenter }
        .addUiFactory { ScreenView(ui) }
        .eventListenerFactory(eventListenerFactory)
        .build()

    moleculeFlow(Immediate) {
        CircuitProvider(circuit) { CircuitContent(TestScreen) }
        presenter.present(events)
      }
      .test {
        awaitItem()
        events.emit("Event")
        state.value = "State2"
        awaitItem()
        val (screen, listener) = eventListenerFactory.listeners.entries.first()
        assertThat(screen).isEqualTo(TestScreen)

        assertThat(listener.states.awaitItem()).isEqualTo("State")
        assertThat(listener.states.awaitItem()).isEqualTo("State2")
        assertThat(listener.events.awaitItem()).isEqualTo("Event")
      }
  }
}

@Parcelize private object TestScreen : Screen

private class StringPresenter(val state: State<String>) : Presenter<String, String> {
  @Composable
  override fun present(events: Flow<String>): String {
    return state.value
  }
}

private class StringUi(val eventsSource: Flow<String>) : Ui<String, String> {

  @Composable
  override fun Render(state: String, events: (String) -> Unit) {
    collectEvents(eventsSource) { events(it) }
  }
}

private class RecordingEventListener : EventListener {
  val states = Turbine<Any>()
  val events = Turbine<Any>()
  override fun onState(state: Any) {
    states.add(state)
  }

  override fun onEvent(event: Any) {
    events.add(event)
  }

  class Factory : EventListener.Factory {
    private val listeners = mutableMapOf<Screen, RecordingEventListener>()
    
    fun get(screen: Screen): RecordingEventListener =
      listeners[screen] ?: (RecordingEventListener().also { listeners[screen] = it })
      
    override fun create(screen: Screen): EventListener = get(screen)
  }
}
