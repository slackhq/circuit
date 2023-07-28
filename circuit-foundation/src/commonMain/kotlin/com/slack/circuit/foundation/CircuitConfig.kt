// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.runtime.CircuitContext

/*
 * Deprecated type markers for ease of migration.
 */

@Deprecated("Use Circuit instead", ReplaceWith("Circuit", "com.slack.circuit.foundation.Circuit"))
public typealias CircuitConfig = Circuit

@Deprecated(
  "Use CircuitContext.circuit instead",
  ReplaceWith("circuit", "com.slack.circuit.foundation.circuit")
)
public var CircuitContext.config: Circuit
  get() = circuit
  set(value) {
    circuit = value
  }
