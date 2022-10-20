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
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

public class Continuity : ViewModel(), RetainedStateRegistry {
  private val delegate = RetainedStateRegistryImpl(null)

  override fun consumeValue(key: String): Any? {
    return delegate.consumeValue(key)
  }

  override fun registerValue(key: String, valueProvider: () -> Any?): RetainedStateRegistry.Entry {
    return delegate.registerValue(key, valueProvider)
  }

  override fun performSave() {
    delegate.performSave()
  }

  override fun forgetUnclaimedValues() {
    delegate.forgetUnclaimedValues()
  }

  override fun onCleared() {
    delegate.retained.clear()
    delegate.valueProviders.clear()
  }

  // TODO find a better approach for this
  public fun peekRetained(): MutableMap<String, List<Any?>> = delegate.retained
  public fun peekProviders(): MutableMap<String, MutableList<() -> Any?>> = delegate.valueProviders

  internal companion object {
    const val KEY = "CircuitContinuity"
  }
}

/**
 * Provides a [RetainedStateRegistry].
 *
 * @param factory an optional [ViewModelProvider.Factory] to use when creating the [Continuity]
 * instance.
 */
@Composable
public fun continuityRetainedStateRegistry(
  factory: ViewModelProvider.Factory? = null
): RetainedStateRegistry {
  SideEffect { println("ZAC: continuityRetainedStateRegistry") }
  val vm = viewModel<Continuity>(key = Continuity.KEY, factory = factory)
  val canRetain = rememberCanRetainChecker()
  remember(canRetain) {
    object : RememberObserver {
      override fun onAbandoned() = unregisterIfNotRetainable()

      override fun onForgotten() = unregisterIfNotRetainable()

      override fun onRemembered() {
        // Do nothing
      }

      fun unregisterIfNotRetainable() {
        if (canRetain()) {
          vm.performSave()
        }
      }
    }
  }
  val localConfig = LocalConfiguration.current
  SideEffect { println("ZAC: setting up unclaimed values stuff") }
  LaunchedEffect(vm, canRetain, localConfig) {
    println("ZAC: Waiting for frame")
    withFrameNanos {}
    println("ZAC: Forgetting unclaimed values")
    // This resumes after the just-composed frame completes drawing. Any unclaimed values at this
    // point can be assumed to be no longer used
    vm.forgetUnclaimedValues()
  }
  return vm
}
