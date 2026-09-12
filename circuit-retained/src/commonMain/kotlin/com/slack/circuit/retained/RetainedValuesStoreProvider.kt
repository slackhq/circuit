// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

/**
 * Installs a retained-values store that survives composition recreation for the lifetime of its
 * [owner].
 *
 * If [owner] is omitted, this uses the current lifecycle ViewModel owner on platforms that provide
 * one. Other platforms require an explicit owner. An already installed non-forgetful retained
 * values store always takes precedence, allowing a platform or application store to control the
 * lifetime instead.
 *
 * This provides in-memory retention only. It does not restore values after process death. Values
 * are kept for the full lifetime of [owner], even while their provider is out of the composition.
 * Independently recreated roots whose order can change should each receive an explicit [owner]
 * instead of sharing one automatic owner.
 *
 * @throws IllegalStateException if no explicit or platform owner is available, or if [owner] has
 *   already been disposed.
 */
@ExperimentalCircuitRetainedApi
@Composable
public fun RetainedValuesStoreProvider(
  owner: RetainedValuesStoreOwner? = null,
  content: @Composable () -> Unit,
) {
  me.stagg.retainx.RetainedValuesStoreProvider(owner?.delegate, content)
}

@OptIn(ExperimentalCircuitRetainedApi::class)
internal inline fun resolveRetainedValuesStoreOwner(
  owner: RetainedValuesStoreOwner?,
  automaticOwner: () -> RetainedValuesStoreOwner?,
): RetainedValuesStoreOwner =
  owner
    ?: automaticOwner()
    ?: error(
      "RetainedValuesStoreProvider requires a RetainedValuesStoreOwner. Pass one explicitly, " +
        "or provide a LocalViewModelStoreOwner on platforms with ViewModel support."
    )

@Composable
@OptIn(ExperimentalCircuitRetainedApi::class)
internal expect fun automaticRetainedValuesStoreOwner(): RetainedValuesStoreOwner?
