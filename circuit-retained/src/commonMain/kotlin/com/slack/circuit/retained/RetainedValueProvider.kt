// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION")

package com.slack.circuit.retained

/**
 * Provides a retained value to a [RetainedStateRegistry].
 *
 * Only defined as a top-level interface to allow non-JS targets to extend `() -> Any?`.
 */
@Deprecated(RETAINED_STATE_REGISTRY_DEPRECATION)
public expect fun interface RetainedValueProvider {
  /**
   * Returns the retained value. If the value is wrapped in a holder class, this class should
   * implement [RetainedValueHolder].
   */
  public fun invoke(): Any?
}

/** Interface that allows [RetainedStateRegistry] to unwrap any values held underneath. */
@Deprecated(RETAINED_STATE_REGISTRY_DEPRECATION)
public interface RetainedValueHolder<T> {
  public val value: T
}
