// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass

private const val AutomaticRetainedValuesStoreOwnerViewModelKey =
  "com.slack.circuit.retained.RetainedValuesStoreOwner"

// Compose's non-Android default store loses retained values when the composition is destroyed,
// just like remember. This ViewModel preserves them until its ViewModelStore is cleared.
// https://issuetracker.google.com/issues/467397537
@Composable
@OptIn(ExperimentalCircuitRetainedApi::class)
internal actual fun automaticRetainedValuesStoreOwner(): RetainedValuesStoreOwner? {
  val viewModelStoreOwner = LocalViewModelStoreOwner.current ?: return null
  @Suppress("ComposeViewModelInjection")
  return viewModel<RetainedValuesStoreOwnerViewModel>(
      viewModelStoreOwner = viewModelStoreOwner,
      key = AutomaticRetainedValuesStoreOwnerViewModelKey,
      factory = RetainedValuesStoreOwnerViewModel.Factory,
    )
    .owner
}

@OptIn(ExperimentalCircuitRetainedApi::class)
private class RetainedValuesStoreOwnerViewModel : ViewModel() {
  val owner = RetainedValuesStoreOwner()

  override fun onCleared() {
    owner.dispose()
  }

  object Factory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
      @Suppress("UNCHECKED_CAST")
      return RetainedValuesStoreOwnerViewModel() as T
    }
  }
}
