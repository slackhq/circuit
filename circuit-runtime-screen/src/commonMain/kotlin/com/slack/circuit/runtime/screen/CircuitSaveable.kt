// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

/** A Circuit value whose persisted representation is handled by a [CircuitSaver]. */
// Trivially expect/actual because that's required for KMP when the subtypes are expect/actual
public expect sealed interface CircuitSaveable
