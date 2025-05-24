// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass

@Composable
internal fun lifecycleRetainedStateRegistry(
  key: String,
  retainedStateRegistryFactory: ViewModelRetainedStateRegistryFactory<*> =
    RetainedStateRegistryViewModel.Factory,
  canRetainChecker: CanRetainChecker = CanRetainChecker.Always,
): RetainedStateRegistry {
  return viewModelPersistentRetainedStateRegistry(
    key = key,
    retainedStateRegistryFactory = retainedStateRegistryFactory,
    canRetainChecker = canRetainChecker,
  )
}

@Composable
private fun viewModelPersistentRetainedStateRegistry(
  key: String,
  retainedStateRegistryFactory: ViewModelRetainedStateRegistryFactory<*>,
  canRetainChecker: CanRetainChecker,
): RetainedStateRegistry {
  val factory =
    remember(retainedStateRegistryFactory) {
      DelegatingContinuityViewModelProviderFactory(retainedStateRegistryFactory)
    }
  @Suppress("ComposeViewModelInjection")
  val vm = viewModel<ViewModel>(key = key, factory = factory) as UpdatableRetainedStateRegistry
  vm.update(canRetainChecker)
  DisposableEffect(vm) { onDispose { vm.update(CanRetainChecker.Never) } }
  LifecycleStartEffect(vm) { onStopOrDispose { vm.saveAll() } }
  LaunchedEffect(vm) {
    withFrameNanos {}
    // This resumes after the just-composed frame completes drawing. Any unclaimed values at this
    // point can be assumed to be no longer used
    vm.forgetUnclaimedValues()
  }
  return vm
}

/**
 * A [RetainedStateRegistry] used by [lifecycleRetainedStateRegistry] that can update its
 * [CanRetainChecker].
 */
public interface UpdatableRetainedStateRegistry : RetainedStateRegistry {
  public fun update(canRetainChecker: CanRetainChecker)
}

/**
 * A factory for creating a [ViewModel] that implements [UpdatableRetainedStateRegistry] for
 * [lifecycleRetainedStateRegistry].
 */
public interface ViewModelRetainedStateRegistryFactory<T> where
T : ViewModel,
T : UpdatableRetainedStateRegistry {
  public fun create(modelClass: KClass<T>, extras: CreationExtras? = null): T
}

internal class RetainedStateRegistryViewModel :
  ViewModel(), UpdatableRetainedStateRegistry, CanRetainChecker {
  private val delegate = RetainedStateRegistryImpl(this, null)
  private var canRetainChecker: CanRetainChecker = CanRetainChecker.Never

  override fun update(canRetainChecker: CanRetainChecker) {
    this.canRetainChecker = canRetainChecker
  }

  override fun canRetain(): Boolean {
    return canRetainChecker.canRetain()
  }

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

  internal object Factory : ViewModelRetainedStateRegistryFactory<RetainedStateRegistryViewModel> {
    override fun create(
      modelClass: KClass<RetainedStateRegistryViewModel>,
      extras: CreationExtras?,
    ): RetainedStateRegistryViewModel {
      return RetainedStateRegistryViewModel()
    }
  }
}

private class DelegatingContinuityViewModelProviderFactory<VM>(
  private val delegate: ViewModelRetainedStateRegistryFactory<VM>
) : ViewModelProvider.Factory where VM : ViewModel, VM : UpdatableRetainedStateRegistry {

  override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
    @Suppress("UNCHECKED_CAST")
    return delegate.create(modelClass as KClass<VM>, extras) as T
  }
}
