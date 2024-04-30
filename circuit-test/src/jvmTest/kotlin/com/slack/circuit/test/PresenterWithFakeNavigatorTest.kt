// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import androidx.compose.runtime.Composable
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PresenterWithFakeNavigatorTest {
  @Test
  fun tacoPresenter() = runTest {
    val navigator = FakeNavigator(TestScreen.ScreenA)
    val presenter = TacoPresenter(navigator = navigator)
    presenter.test {
      val eventSink = awaitItem().eventSink
      eventSink.invoke(TacoEvent.TacoNoOp)
      navigator.expectNoGoToEvents()
      navigator.expectNoPopEvents()
      navigator.expectNoResetRootEvents()

      eventSink.invoke(TacoEvent.TacoPop)
      val pop = navigator.awaitPop()
      assertThat(pop.poppedScreen).isNull() // Already at root
      navigator.expectNoGoToEvents()
      navigator.expectNoPopEvents()
      navigator.expectNoResetRootEvents()

      eventSink.invoke(TacoEvent.TacoGoTo(TestScreen.ScreenB))
      val goTo = navigator.awaitNextGoTo()
      assertThat(goTo.screen).isEqualTo(TestScreen.ScreenB)
      assertThat(goTo.success).isTrue()
      navigator.expectNoGoToEvents()
      navigator.expectNoPopEvents()
      navigator.expectNoResetRootEvents()

      eventSink.invoke(TacoEvent.TacoResetRoot(TestScreen.ScreenC))
      val resetRoot = navigator.awaitResetRoot()
      assertThat(resetRoot.newRoot).isEqualTo(TestScreen.ScreenC)
      navigator.expectNoGoToEvents()
      navigator.expectNoPopEvents()
      navigator.expectNoResetRootEvents()
    }
  }
}

private sealed interface TacoEvent : CircuitUiEvent {
  data object TacoNoOp : TacoEvent

  data object TacoPop : TacoEvent

  data class TacoGoTo(val screen: Screen) : TacoEvent

  data class TacoResetRoot(val screen: Screen) : TacoEvent
}

private data class TacoState(val eventSink: (TacoEvent) -> Unit) : CircuitUiState

private class TacoPresenter(val navigator: Navigator) : Presenter<TacoState> {
  @Composable override fun present(): TacoState = TacoState(eventSink = ::handleEvent)

  private fun handleEvent(event: TacoEvent) {
    when (event) {
      TacoEvent.TacoPop -> navigator.pop()
      TacoEvent.TacoNoOp -> Unit
      is TacoEvent.TacoGoTo -> navigator.goTo(event.screen)
      is TacoEvent.TacoResetRoot -> navigator.resetRoot(event.screen)
    }
  }
}
