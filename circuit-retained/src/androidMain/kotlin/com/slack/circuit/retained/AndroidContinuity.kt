// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel

internal class ContinuityViewModel : ViewModel(), RetainedStateRegistry {
  private val delegate = RetainedStateRegistryImpl(null)

  override fun consumeValue(key: String): Any? {
    return delegate.consumeValue(key)
  }

  override fun registerValue(
    key: String,
    valueProvider: RetainedValueProvider,
  ): RetainedStateRegistry.Entry {
    return delegate.registerValue(key, valueProvider)
  }

  override fun saveAll(): Map<String, List<Any?>> {
    return delegate.saveAll()
  }

  override fun saveValue(key: String) {
    delegate.saveValue(key)
  }

  override fun forgetUnclaimedValues() {
    delegate.forgetUnclaimedValues()
  }

  override fun onCleared() {
    delegate.forgetUnclaimedValues()
    delegate.valueProviders.clear()
  }

  @VisibleForTesting fun peekRetained(): Map<String, List<Any?>> = delegate.retained.toMap()

  @VisibleForTesting
  fun peekProviders(): Map<String, MutableList<RetainedValueProvider>> =
    delegate.valueProviders.toMap()

  object Factory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      @Suppress("UNCHECKED_CAST")
      return ContinuityViewModel() as T
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
      return create(modelClass)
    }
  }
}

@Composable
public actual fun continuityRetainedStateRegistry(
  key: String,
  canRetainChecker: CanRetainChecker,
): RetainedStateRegistry =
  continuityRetainedStateRegistry(key, ContinuityViewModel.Factory, canRetainChecker)

/**
 * Provides a [RetainedStateRegistry].
 *
 * @param key the key to use when creating the [Continuity] instance.
 * @param factory an optional [ViewModelProvider.Factory] to use when creating the [Continuity]
 *   instance.
 * @param canRetainChecker an optional [CanRetainChecker] to use when determining whether to retain.
 */
@Composable
public fun continuityRetainedStateRegistry(
  key: String = Continuity.KEY,
  factory: ViewModelProvider.Factory = ContinuityViewModel.Factory,
  canRetainChecker: CanRetainChecker = LocalCanRetainChecker.current ?: rememberCanRetainChecker(),
): RetainedStateRegistry {
  @Suppress("ComposeViewModelInjection")
  val vm = viewModel<ContinuityViewModel>(key = key, factory = factory)
  val lastCanRetainChecker by rememberUpdatedState(canRetainChecker)

  LifecycleStartEffect(vm) {
    onStopOrDispose {
      if (lastCanRetainChecker.canRetain(vm)) {
        vm.saveAll()
      }
    }
  }

  LaunchedEffect(vm) {
    withFrameNanos {}
    // This resumes after the just-composed frame completes drawing. Any unclaimed values at this
    // point can be assumed to be no longer used
    vm.forgetUnclaimedValues()
  }

  return vm
}
