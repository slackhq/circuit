// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sharedelements

/**
 * Marker type to help coordinate keys used by shared element transitions.
 *
 * **Definition**
 *
 * ```kotlin
 * /** Transition key used for shared bounds transitions of a Pets screen. */
 * data class PetCardBoundsKey(val id: Long) : SharedTransitionKey
 * ```
 *
 * **Grid**
 *
 * ```kotlin
 * ElevatedCard(
 *   modifier =
 *     modifier.sharedBounds(
 *       sharedContentState = rememberSharedContentState(key = PetCardBoundsKey(animal.id))
 *     )
 * ```
 *
 * **Detail**
 *
 * ```kotlin
 * Scaffold(
 *   modifier =
 *     modifier.sharedBounds(
 *       sharedContentState = rememberSharedContentState(key = PetCardBoundsKey(animalId))
 *     )
 * ```
 */
public interface SharedTransitionKey
