// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.retain.RetainObserver
import kotlin.concurrent.Volatile

/** Global settings for circuit-retained behavior. */
@ExperimentalCircuitRetainedApi
public object CircuitRetainedSettings {
  /**
   * When true, [lifecycleRetainedStateRegistry] is backed by Compose's first-party
   * [`retain`](https://developer.android.com/develop/ui/compose/state-lifespans) API instead of a
   * Circuit-managed `ViewModel`. Retention is then driven by the `RetainedValuesStore` installed in
   * the composition, such as the lifecycle-aware store Compose UI installs on Android.
   *
   * This also enables per-record scoping of first-party `retain {}` calls inside
   * `NavigableCircuitContent`: values retained by a record's content survive while the record is in
   * the nav stack (including across configuration changes) and are retired when the record is
   * popped.
   *
   * Set this before the first composition. It is not a runtime toggle, registries created under one
   * backing do not migrate their state to the other.
   *
   * This defaults to true on Android and false on other platforms. On non-Android platforms,
   * Compose's default store loses values when the composition is destroyed, just like `remember`.
   * [RetainedValuesStoreProvider] preserves them until its `ViewModelStore` is cleared or its
   * explicit [RetainedValuesStoreOwner] is disposed. This flag only changes the backing on Android,
   * JVM, iOS, macOS, and web.
   */
  @Volatile public var useFirstParty: Boolean = useFirstPartyByDefault

  /**
   * When true, Circuit retention APIs reject [RememberObserver] values that do not also implement
   * [RetainObserver], matching AndroidX `retain`.
   *
   * Enable this before migrating call sites to AndroidX `retain`. Defaults to false.
   */
  @Volatile public var enforceRetainObserverCompatibility: Boolean = false
}

// Circuit's ViewModel-backed registry survives composition teardown while its ViewModelStore is
// still alive. Compose Multiplatform does not yet install a corresponding RetainedValuesStore, so
// first-party retain behaves like remember and would lose that state when the composition is
// recreated. Keep first-party backing disabled off Android until that is addressed.
// https://issuetracker.google.com/issues/467397537
internal expect val useFirstPartyByDefault: Boolean
