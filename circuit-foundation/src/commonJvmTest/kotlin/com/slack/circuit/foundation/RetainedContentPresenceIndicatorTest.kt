// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import com.slack.circuit.retained.NoOpRetainedStateRegistry
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.ui.ui
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

private object UnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertBottomUp(index: Int, instance: Unit) {}
  override fun insertTopDown(index: Int, instance: Unit) {}
  override fun move(from: Int, to: Int, count: Int) {}
  override fun remove(index: Int, count: Int) {}
  override fun onClear() {}
}

private data object IndicatorTestState : CircuitUiState

@OptIn(ExperimentalCircuitRetainedApi::class, ExperimentalCoroutinesApi::class)
@Suppress("UNUSED_EXPRESSION")
class RetainedContentPresenceIndicatorTest {

  @Test
  fun cancelledRecomposer_retainedContentPresenceIndicator_doesNotThrowCancellationException() =
    runTest {
      val clock = BroadcastFrameClock()
      val recomposer = Recomposer(coroutineContext + clock)
      val job = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
      recomposer.cancel()
      job.join()

      val composition = Composition(UnitApplier, recomposer)
      val store = ManagedRetainedValuesStore()

      try {
        composition.setContent {
          val composer = currentComposer
          remember(store) { RetainedContentPresenceIndicator(store, composer) }
            .apply { this.composer = composer }
        }
      } finally {
        composition.dispose()
        store.dispose()
      }
    }

  @Test
  fun cancelledRecomposer_pausableState_doesNotThrowCancellationException() = runTest {
    val previousUseFirstParty = CircuitRetainedSettings.useFirstParty
    CircuitRetainedSettings.useFirstParty = true
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + clock)
    val job = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
    recomposer.cancel()
    job.join()

    val composition = Composition(UnitApplier, recomposer)
    val rootStore = ManagedRetainedValuesStore()

    try {
      composition.setContent {
        CompositionLocalProvider(LocalRetainedValuesStore provides rootStore) {
          val state = pausableState { "test_state" }
          check(state == "test_state")
        }
      }
    } finally {
      CircuitRetainedSettings.useFirstParty = previousUseFirstParty
      composition.dispose()
      rootStore.dispose()
    }
  }

  @Test
  fun cancelledRecomposer_circuitContent_doesNotThrowCancellationException() = runTest {
    val previousUseFirstParty = CircuitRetainedSettings.useFirstParty
    CircuitRetainedSettings.useFirstParty = true
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + clock)
    val job = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
    recomposer.cancel()
    job.join()

    val composition = Composition(UnitApplier, recomposer)
    val rootStore = ManagedRetainedValuesStore()
    val circuit =
      Circuit.Builder()
        .addPresenterFactory { _, _, _ -> presenterOf { IndicatorTestState } }
        .addUiFactory { _, _ -> ui<IndicatorTestState> { _, _ -> } }
        .build()

    try {
      composition.setContent {
        CompositionLocalProvider(LocalRetainedValuesStore provides rootStore) {
          CircuitCompositionLocals(
            circuit = circuit,
            circuitSaver = CircuitSaver.NoOp,
            retainedStateRegistry = NoOpRetainedStateRegistry,
          ) {
            CircuitContent(screen = TestScreen)
          }
        }
      }
    } finally {
      CircuitRetainedSettings.useFirstParty = previousUseFirstParty
      composition.dispose()
      rootStore.dispose()
    }
  }
}
