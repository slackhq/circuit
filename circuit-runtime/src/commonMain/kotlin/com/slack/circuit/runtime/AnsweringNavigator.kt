// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.internal.LocalAnsweringNavigatorProvider
import com.slack.circuit.runtime.screen.PopResult
import kotlin.reflect.KClass

/** Returns whether answering navigation is available in the current composition. */
@Composable
public fun answeringNavigationAvailable(): Boolean =
  @OptIn(InternalCircuitApi::class) LocalAnsweringNavigatorProvider.current != null

/**
 * Returns a [GoToNavigator] that receives a [T] when its destination returns one, or
 * [fallbackNavigator] when answering navigation is unavailable.
 */
@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  noinline block: (result: T) -> Unit,
): GoToNavigator = rememberAnsweringNavigator(fallbackNavigator, T::class, block)

/**
 * Returns a [GoToNavigator] that receives a [resultType] when its destination returns one, or
 * [fallbackNavigator] when answering navigation is unavailable.
 *
 * [block] is not called if the destination does not return a result assignable to [resultType].
 */
@Composable
@OptIn(InternalCircuitApi::class)
public fun <T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  resultType: KClass<T>,
  block: (result: T) -> Unit,
): GoToNavigator {
  val provider = LocalAnsweringNavigatorProvider.current ?: return fallbackNavigator
  return provider.rememberAnsweringNavigator(resultType, block)
}
