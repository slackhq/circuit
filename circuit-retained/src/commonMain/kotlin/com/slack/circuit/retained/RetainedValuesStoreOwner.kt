// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

/**
 * Owns the retained-value stores used by [RetainedValuesStoreProvider] to preserve values across
 * composition recreation.
 *
 * The owner must outlive every composition that should share retained values. Call [dispose] when
 * that lifetime permanently ends. Values are held in memory and do not survive process death. This
 * class is not thread-safe. Calls to it and callbacks from its leases must be serialized.
 *
 * Providers that share an owner use positional identity. Providers at the same position are matched
 * in acquisition order when recreated. Matching requires the previous composition to be disposed
 * before its replacement composes, so a provider that overlaps its predecessor receives a new empty
 * store. Use a separate owner for independently recreated roots whose order can change.
 *
 * Stores keep their values until [dispose], including when their provider permanently leaves the
 * composition.
 */
@ExperimentalCircuitRetainedApi
public class RetainedValuesStoreOwner {
  internal val delegate = me.stagg.retainx.RetainedValuesStoreOwner()

  /**
   * Permanently releases every retained value held by this owner.
   *
   * Calling this function more than once has no effect. After disposal, this owner cannot be used
   * again.
   */
  public fun dispose() {
    delegate.dispose()
  }
}
