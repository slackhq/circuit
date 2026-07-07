// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.lifecycle.compose.LifecycleStartEffect

/**
 * A [RetainedStateRegistry] whose survival is delegated to Compose's first-party `retain` API.
 *
 * The registry object is retained as a single root value. The
 * [androidx.compose.runtime.retain.RetainedValuesStore] installed in the composition decides
 * whether it survives leaving composition, such as across configuration changes via the
 * lifecycle-aware store Compose UI installs on Android.
 *
 * Must be called outside any `movableContentOf` so its retain slot has a stable position.
 */
@Composable
internal fun retainBackedRetainedStateRegistry(
  key: String,
  canRetainChecker: CanRetainChecker = CanRetainChecker.Always,
): RetainedStateRegistry {
  val registry = retain(key) { RetainBackedRetainedStateRegistry() }
  registry.update(canRetainChecker)
  DisposableEffect(registry) { onDispose { registry.update(CanRetainChecker.Never) } }

  // Values must be saved into the registry before composition teardown so they survive while the
  // store is retaining. Stop covers both config changes and backgrounding.
  LifecycleStartEffect(registry) { onStopOrDispose { registry.saveAll() } }

  val composer = currentComposer
  DisposableEffect(registry) {
    val cancellationHandle = composer.scheduleFrameEndCallback {
      // This resumes after the just-composed frame completes drawing. Any unclaimed values at
      // this point can be assumed to be no longer used
      registry.forgetUnclaimedValues()
    }
    onDispose { cancellationHandle.cancel() }
  }
  return registry
}

private class RetainBackedRetainedStateRegistry(
  private val delegate: RetainedStateRegistryImpl =
    RetainedStateRegistryImpl(CanRetainChecker.Never, null)
) :
  UpdatableRetainedStateRegistry,
  RetainedStateRegistry by delegate,
  RetainObserver,
  CanRetainChecker {

  private var canRetainChecker: CanRetainChecker = CanRetainChecker.Never

  init {
    delegate.update(this)
  }

  override fun update(canRetainChecker: CanRetainChecker) {
    this.canRetainChecker = canRetainChecker
  }

  override fun canRetain(): Boolean = canRetainChecker.canRetain()

  override fun onRetained() {}

  override fun onEnteredComposition() {}

  override fun onExitedComposition() {}

  override fun onRetired() = clear()

  override fun onUnused() = clear()

  private fun clear() {
    delegate.forgetUnclaimedValues()
    delegate.valueProviders.clear()
  }

  @VisibleForTesting fun peekRetained(): Map<String, List<Any?>> = delegate.retained.toMap()

  @VisibleForTesting
  fun peekProviders(): Map<String, MutableList<RetainedValueProvider>> =
    delegate.valueProviders.toMap()
}
