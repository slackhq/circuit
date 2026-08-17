// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import androidx.compose.runtime.Stable

/**
 * Marker interface for screens used in SubCircuits.
 *
 * SubScreens are not navigation records, and SubCircuit does not persist them. They are designed to
 * be used within a parent component that handles navigation and other cross-cutting concerns.
 *
 * @param OuterEvent The type of events that will be sent to the outer event sink. Should implement
 *   [SubCircuitOuterEvent].
 * @see SubPresenter
 * @see SubCircuitContent
 * @see SubCircuitOuterEvent
 */
@Stable public interface SubScreen<OuterEvent : SubCircuitOuterEvent>
