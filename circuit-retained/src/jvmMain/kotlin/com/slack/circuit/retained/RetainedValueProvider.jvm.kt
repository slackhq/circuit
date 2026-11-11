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
public actual fun interface RetainedValueProvider : () -> Any? {
  public actual override fun invoke(): Any?
}
