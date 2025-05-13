// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * An abstract [ViewModel] instance that implements [RetainedStateRegistry] and is used in
 * [continuityRetainedStateRegistry].
 */
public abstract class ContinuityViewModel : ViewModel(), RetainedStateRegistry {
  /** Update the [CanRetainChecker] used by the [ContinuityViewModel]. */
  public abstract fun update(canRetainChecker: CanRetainChecker)
}

internal class ContinuityViewModelImpl :
  ContinuityViewModel(), RetainedStateRegistry, CanRetainChecker {
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

  object Factory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      @Suppress("UNCHECKED_CAST")
      return ContinuityViewModelImpl() as T
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
  continuityRetainedStateRegistry(key, ContinuityViewModelImpl.Factory, canRetainChecker)

/**
 * Provides a [RetainedStateRegistry].
 *
 * @param key the key to use when creating the [Continuity] instance.
 * @param factory an optional [ViewModelProvider.Factory] to use when creating the [Continuity]
 *   instance. This factory should create a subclass of [ContinuityViewModel].
 * @param canRetainChecker an optional [CanRetainChecker] to use when determining whether to retain.
 */
@Composable
public fun continuityRetainedStateRegistry(
  key: String = Continuity.KEY,
  factory: ViewModelProvider.Factory = ContinuityViewModelImpl.Factory,
  canRetainChecker: CanRetainChecker = rememberContinuityCanRetainChecker(),
): RetainedStateRegistry {
  @Suppress("ComposeViewModelInjection")
  val vm = viewModel<ContinuityViewModel>(key = key, factory = factory)
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

/** On Android, we retain only if the activity is changing configurations. */
@Composable
public actual fun rememberContinuityCanRetainChecker(): CanRetainChecker {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  return remember { CanRetainChecker { activity?.isChangingConfigurations == true } }
}

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}
