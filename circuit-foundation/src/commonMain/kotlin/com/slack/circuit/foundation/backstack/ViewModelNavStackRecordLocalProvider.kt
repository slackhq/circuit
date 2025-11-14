// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.backstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavStack
import com.slack.circuit.backstack.NavStackRecordLocalProvider
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.retained.rememberRetained
import kotlin.reflect.KClass

/**
 * Returns a [ViewModel] using a [ViewModelStoreOwner] resolved from the back stack host context.
 *
 * This is useful for obtaining ViewModels that are scoped to the host of the back stack.
 *
 * @param VM The type of the ViewModel.
 * @param viewModelStoreOwner The [ViewModelStoreOwner] that will host this ViewModel. Defaults to
 *   the current [LocalBackStackHostViewModelStoreOwner] if available, otherwise falls back to the
 *   current [LocalViewModelStoreOwner].
 * @param key An optional key to differentiate amongst ViewModels of the same type.
 * @param factory An optional [ViewModelProvider.Factory] to use for creating the ViewModel.
 * @param extras The [CreationExtras] to use for creating the ViewModel. Defaults to
 *   [HasDefaultViewModelProviderFactory.defaultViewModelCreationExtras] if the resolved
 *   [viewModelStoreOwner] implements it, otherwise [CreationExtras.Empty].
 * @return An instance of the specified [ViewModel] [VM].
 */
@Composable
public inline fun <reified VM : ViewModel> backStackHostViewModel(
  viewModelStoreOwner: ViewModelStoreOwner =
    checkNotNull(backStackHostViewModelStoreOwner()) {
      "No ViewModelStoreOwner was provided for backStackHostViewModel"
    },
  key: String? = null,
  factory: ViewModelProvider.Factory? = null,
  extras: CreationExtras =
    if (viewModelStoreOwner is HasDefaultViewModelProviderFactory) {
      viewModelStoreOwner.defaultViewModelCreationExtras
    } else {
      CreationExtras.Empty
    },
): VM = viewModel(VM::class, viewModelStoreOwner, key, factory, extras)

/**
 * Returns the [ViewModelStoreOwner] of the component hosting the back stack, populated by
 * [ViewModelNavStackRecordLocalProvider] or the current [LocalViewModelStoreOwner].
 *
 * @return The [ViewModelStoreOwner], or `null` if none is set.
 */
@Composable
public fun backStackHostViewModelStoreOwner(): ViewModelStoreOwner? =
  LocalBackStackHostViewModelStoreOwner.current ?: LocalViewModelStoreOwner.current

@Suppress("ComposeCompositionLocalUsage")
private val LocalBackStackHostViewModelStoreOwner:
  ProvidableCompositionLocal<ViewModelStoreOwner?> =
  compositionLocalOf {
    null
  }

/**
 * A [NavStackRecordLocalProvider] that provides a [LocalViewModelStoreOwner] for each record in the
 * back stack.
 *
 * This allows [ViewModel] instances to be scoped to the lifecycle of a specific [BackStack.Record].
 * It also provides [LocalBackStackHostViewModelStoreOwner] with the [ViewModelStoreOwner] of the
 * host of the back stack.
 */
public object ViewModelNavStackRecordLocalProvider : NavStackRecordLocalProvider<NavStack.Record> {
  /**
   * Provides [LocalViewModelStoreOwner] scoped to the given [record] and
   * [LocalBackStackHostViewModelStoreOwner] scoped to the host of the back stack.
   */
  @Composable
  override fun providedValuesFor(record: NavStack.Record): ProvidedValues {
    // Gracefully fail if we don't have a host ViewModelStoreOwner.
    val backStackHostViewModelStoreOwner =
      LocalViewModelStoreOwner.current ?: return EMPTY_PROVIDED_VALUES
    // Implementation note: providedValuesFor stays in the composition as long as the
    // back stack entry is present, which makes it safe for us to use composition
    // forget/abandon to clear the associated ViewModelStore.
    val containerViewModel =
      viewModel<BackStackRecordLocalProviderViewModel>(
        viewModelStoreOwner = backStackHostViewModelStoreOwner,
        factory = BackStackRecordLocalProviderViewModel.Factory,
      )
    val viewModelStore = containerViewModel.viewModelStoreForKey(record.key)
    // This will retain the observer in NavigableCircuitContent using its outerRegistry.
    val observer =
      rememberRetained(record, viewModelStore) {
        NestedRememberObserver {
          containerViewModel.removeViewModelStoreOwnerForKey(record.key)?.clear()
        }
      }
    return remember(viewModelStore) {
      val list =
        listOf<ProvidedValue<*>>(
          LocalBackStackHostViewModelStoreOwner provides backStackHostViewModelStoreOwner,
          LocalViewModelStoreOwner provides
            object : ViewModelStoreOwner {
              override val viewModelStore: ViewModelStore = viewModelStore
            },
        )
      ProvidedValues {
        remember { observer.UiRememberObserver() }
        list
      }
    }
  }
}

private val EMPTY_PROVIDED_VALUES = ProvidedValues { emptyList() }

/**
 * A [ViewModel] responsible for holding and managing [ViewModelStore] instances for each record in
 * a back stack.
 *
 * This ViewModel is typically scoped to the host of the back stack.
 */
internal class BackStackRecordLocalProviderViewModel : ViewModel() {
  private val owners = mutableMapOf<String, ViewModelStore>()

  /**
   * Returns an existing [ViewModelStore] for the given [key], or creates and stores a new one if it
   * doesn't exist.
   */
  internal fun viewModelStoreForKey(key: String): ViewModelStore =
    owners.getOrPut(key) { ViewModelStore() }

  /** Removes and returns the [ViewModelStore] associated with the given [key]. */
  internal fun removeViewModelStoreOwnerForKey(key: String): ViewModelStore? = owners.remove(key)

  /** Clears all stored [ViewModelStore] instances. */
  override fun onCleared() {
    owners.forEach { (_, store) -> store.clear() }
  }

  /** Factory for creating [BackStackRecordLocalProviderViewModel] instances. */
  object Factory : ViewModelProvider.Factory {
    /** Creates a new instance of [BackStackRecordLocalProviderViewModel]. */
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
      @Suppress("UNCHECKED_CAST")
      return BackStackRecordLocalProviderViewModel() as T
    }
  }
}

/**
 * A [RememberObserver] that triggers [onCompletelyForgotten] only when both its nested UI and stack
 * observers have been forgotten.
 *
 * This is used to clear a [ViewModelStore] associated with a back stack record only when that
 * record is truly gone from both the UI composition and the back stack state.
 */
private class NestedRememberObserver(private val onCompletelyForgotten: () -> Unit) :
  RememberObserver {
  private var isRememberedForStack: Boolean = false
    set(value) {
      field = value
      recomputeState()
    }

  private var isRememberedForUi: Boolean = false
    set(value) {
      field = value
      recomputeState()
    }

  private fun recomputeState() {
    if (!isRememberedForUi && !isRememberedForStack) {
      onCompletelyForgotten()
    }
  }

  /** Observes the lifecycle of the UI composition part for a back stack record. */
  inner class UiRememberObserver : RememberObserver {
    override fun onRemembered() {
      isRememberedForUi = true
    }

    override fun onAbandoned() = onForgotten()

    override fun onForgotten() {
      isRememberedForUi = false
    }
  }

  override fun onRemembered() {
    isRememberedForStack = true
  }

  override fun onAbandoned() = onForgotten()

  override fun onForgotten() {
    isRememberedForStack = false
  }
}
