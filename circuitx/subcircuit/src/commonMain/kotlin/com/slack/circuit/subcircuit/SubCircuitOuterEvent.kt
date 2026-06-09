// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

/**
 * Marker interface for event types that are delegated to an outer component.
 *
 * Outer events represent actions that cannot be handled within the SubCircuit itself and must be
 * delegated to a parent component. Common examples include navigation events, showing dialogs, or
 * triggering actions that affect state outside the SubCircuit's scope.
 *
 * Example:
 * ```kotlin
 * sealed interface MyOuterEvent : SubCircuitOuterEvent {
 *   data class NavigateToDetails(val id: String) : MyOuterEvent
 *   data object ShowConfirmationDialog : MyOuterEvent
 * }
 * ```
 *
 * @see SubPresenter
 * @see SubScreen
 */
public interface SubCircuitOuterEvent
