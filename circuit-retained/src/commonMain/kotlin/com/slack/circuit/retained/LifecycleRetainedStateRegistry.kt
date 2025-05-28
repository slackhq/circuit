// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

/**
 * Returns a [RetainedStateRegistry] that is lifecycle-aware, allowing state to be retained across
 * recompositions.
 *
 * The underlying mechanism typically involves a platform-specific lifecycle-scoped object, such as
 * a `ViewModel`.
 *
 * @param key A unique key to identify this registry. This key is used to scope the retained state.
 *   Using different keys allows for multiple independent registries within the same lifecycle.
 *   Defaults to [LifecycleRetainedStateRegistry.KEY].
 * @return A [RetainedStateRegistry] instance associated with the given [key] and the current
 *   lifecycle.
 */
@Composable
public expect fun lifecycleRetainedStateRegistry(
  key: String = LifecycleRetainedStateRegistry.KEY
): RetainedStateRegistry

public object LifecycleRetainedStateRegistry {
  public const val KEY: String = "CircuitLifecycleRetainedStateRegistry"
}
