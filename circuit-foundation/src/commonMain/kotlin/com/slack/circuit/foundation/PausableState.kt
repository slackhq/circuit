// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("NOTHING_TO_INLINE")

package com.slack.circuit.foundation

import androidx.compose.runtime.CancellationHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.retain.retainManagedRetainedValuesStore
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter

/**
 * By default [CircuitContent] will wrap presenters so that the last emitted [CircuitUiState] is
 * replayed when the presenter is paused. If this behavior is not wanted, the [Presenter] should
 * implement this interface to disable the behavior.
 */
@Stable public interface NonPausablePresenter<UiState : CircuitUiState> : Presenter<UiState>

/**
 * Presents the UI state when the lifecycle is resumed, otherwise will replay the last emitted UI
 * state.
 *
 * The [CircuitUiState] class returned by the [Presenter] is required to implement [equals] and
 * [hashCode] methods correctly, otherwise this function can create composition loops.
 *
 * @param key A unique key for the pausable state
 * @param isActive Whether the presenter is active or not.
 */
@Composable
public inline fun <UiState : CircuitUiState> Presenter<UiState>.presentWithLifecycle(
  key: String? = null,
  isActive: Boolean = LocalRecordLifecycle.current.isActive,
): UiState = pausableState(key, isActive) { present() }

/**
 * Wraps a composable state producer, which will replay the last emitted state instance when
 * [isActive] is `false`.
 *
 * The class [T] returned from [produceState] is required to implement [equals] and [hashCode]
 * methods correctly, otherwise this function can create composition loops.
 *
 * @param key A unique key for the pausable state.
 * @param isActive Whether the state producer should be active or not.
 * @param produceState A composable lambda function which produces the state
 */
@Composable
public fun <T> pausableState(
  key: String? = null,
  isActive: Boolean = LocalRecordLifecycle.current.isActive,
  produceState: @Composable () -> T,
): T {
  // This is explicitly not snapshot (or state) backed as don't want it to influence composition.
  // It is here to only store the last emitted state value. Only the caller, or the produceState
  // should be triggering recompositions.
  val state = remember(key) { MutableRef<T>(null) }

  val saveableStateHolder = rememberSaveableStateHolderWithReturn()
  val retainedStateHolder = rememberRetainedStateHolderWithReturn()
  @OptIn(ExperimentalCircuitRetainedApi::class)
  val retainedValuesStore =
    if (CircuitRetainedSettings.useFirstParty) {
      key(key) { retainManagedRetainedValuesStore() }
    } else {
      null
    }

  return if (isActive || state.value == null) {
    val finalKey = key ?: "pausable_state"
    val producedState =
      if (retainedValuesStore != null) {
        withRetainedValuesStoreProvider(retainedValuesStore) {
          saveableStateHolder.SaveableStateProvider(finalKey) {
            retainedStateHolder.RetainedStateProvider(key = finalKey, content = produceState)
          }
        }
      } else {
        saveableStateHolder.SaveableStateProvider(finalKey) {
          retainedStateHolder.RetainedStateProvider(key = finalKey, content = produceState)
        }
      }
    state.value = producedState
    producedState
  } else {
    // Else, we just emit the last stored state instance
    state.value!!
  }
}

internal data class MutableRef<R>(var value: R?)

/**
 * Copy of [androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider], tweaked to return the
 * value produced by [content]. The upstream `Unit` return would allow the producer to recompose
 * without propagating its updated value to this function's caller.
 *
 * TODO: Replace this copy with the upstream return-valued provider once
 *   https://issuetracker.google.com/issues/542168593 is available.
 */
@Composable
private fun <T> withRetainedValuesStoreProvider(
  store: RetainedValuesStore,
  content: @Composable () -> T,
): T {
  return withCompositionLocal(LocalRetainedValuesStore provides store) {
    val result = content()

    // This must come after content so the store starts retaining before its values are forgotten.
    val composer = currentComposer
    remember(store) { RetainedContentPresenceIndicator(store, composer) }
      .apply { this.composer = composer }

    result
  }
}

private class RetainedContentPresenceIndicator(
  private val store: RetainedValuesStore,
  composer: Composer,
) : RememberObserver {
  var composer by mutableStateOf(composer)

  private var didEnterComposition = false
  private var enterCompositionCancellationHandle: CancellationHandle? = null
    set(value) {
      field?.cancel()
      field = value
    }

  override fun onRemembered() {
    enterCompositionCancellationHandle = composer.scheduleFrameEndCallback {
      didEnterComposition = true
      store.onContentEnteredComposition()
    }
  }

  override fun onForgotten() {
    enterCompositionCancellationHandle?.cancel()
    if (didEnterComposition) {
      store.onContentExitComposition()
      didEnterComposition = false
    }
  }

  override fun onAbandoned() {
    enterCompositionCancellationHandle?.cancel()
  }
}
