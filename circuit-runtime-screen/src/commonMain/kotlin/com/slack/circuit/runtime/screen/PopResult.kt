// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import androidx.compose.runtime.Immutable

/**
 * A result returned while calling `Navigator.pop()`.
 *
 * PopResults can be simple sentinel `data object` types or data classes with information to share.
 *
 * ```
 * @Serializable
 * data class ModalResult(
 *   val accepted: Boolean,
 * ) : PopResult
 * ```
 *
 * Results are then passed as arguments to `Navigator.pop()` to pass them.
 *
 * ```
 * navigator.pop(
 *   ModalResult(
 *     accepted = true
 *   )
 * )
 * ```
 *
 * These are only retrievable when a given presenter has navigated for a result via
 * `rememberAnsweringNavigator`.
 *
 * ```
 * val answeringNavigator = rememberAnsweringNavigator<ModalResult>(navigator) { result ->
 *   // ...
 * }
 * answeringNavigator.goTo(ModalScreen())
 * ```
 *
 * Note that `@Serializable` is not strictly required, and you may bring your own serialization or
 * use [ParcelablePopResult] on Android if you rather. You may also opt for no serialization at all
 * if you do not need it!
 */
@Immutable public expect interface PopResult : CircuitSaveable
