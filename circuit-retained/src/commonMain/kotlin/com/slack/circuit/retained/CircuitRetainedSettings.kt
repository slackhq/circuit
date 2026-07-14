// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

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
   * On platforms where no first-party store is installed by default (everything other than Android
   * currently), retained values only survive as long as the store provided in the composition
   * allows. This flag currently only affects targets where the ViewModel-backed registry exists
   * (Android, JVM, iOS, macOS, web).
   */
  @Volatile public var useFirstParty: Boolean = false
}
