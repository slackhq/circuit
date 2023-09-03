// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

/**
 * Provides a retained value to a [RetainedStateRegistry].
 *
 * Only defined as a top-level interface to allow non-JS targets to extend `() -> Any?`.
 */
public actual fun interface RetainedValueProvider {
  public actual fun invoke(): Any?
}
