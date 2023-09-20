// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

/**
 * Provides a retained value to a [RetainedStateRegistry].
 *
 * Only defined as a top-level interface because JS doesn't allow types to extend function types.
 */
public fun interface RetainedValueProvider {
  public operator fun invoke(): Any?
}
