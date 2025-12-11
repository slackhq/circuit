// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Returns whether or not answering navigation is available. This is essentially a proxy for whether
 * or not this composition is running within a [NavigableCircuitContent].
 */
@OptIn(ExperimentalCircuitApi::class)
@Composable
public fun answeringNavigationAvailable(): Boolean =
  LocalBackStack.current != null && LocalAnsweringResultHandler.current != null

/**
 * A reified version of [rememberAnsweringNavigator]. See documented overloads of this function for
 * more information.
 */
@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  noinline block: (result: T) -> Unit,
): GoToNavigator = rememberAnsweringNavigator(fallbackNavigator, T::class, block)

/**
 * Returns a [GoToNavigator] that answers with the given [resultType] or defaults to
 * [fallbackNavigator] if no back stack is available to pass results through.
 */
@OptIn(ExperimentalCircuitApi::class)
@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  resultType: KClass<T>,
  block: (result: T) -> Unit,
): GoToNavigator {
  val backStack = LocalBackStack.current ?: return fallbackNavigator
  val resultHandler = LocalAnsweringResultHandler.current ?: return fallbackNavigator
  return rememberAnsweringNavigator(backStack, resultHandler, resultType, block)
}

/**
 * A reified version of [rememberAnsweringNavigator]. See documented overloads of this function for
 * more information.
 */
@ExperimentalCircuitApi
@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  backStack: BackStack<out BackStack.Record>,
  answeringResultHandler: AnsweringResultHandler,
  noinline block: (result: T) -> Unit,
): GoToNavigator {
  return rememberAnsweringNavigator(backStack, answeringResultHandler, T::class, block)
}

/**
 * Returns a [GoToNavigator] that answers with the given [resultType] using the given [backStack].
 *
 * Handling of the result type [T] should be handled in the [block] parameter and is guaranteed to
 * only be called _at most_ once. It may never be called if the navigated screen never pops with a
 * result (of equivalent type) back.
 *
 * Note that [resultType] is a simple instance check, so subtypes may also be valid answers.
 *
 * ## Example
 *
 * ```kotlin
 * val pickPhotoNavigator = rememberAnsweringNavigator(backStack, PickPhotoScreen.Result::class) { result: PickPhotoScreen.Result ->
 *   // Do something with the result!
 * }
 *
 * return State(...) { event ->
 *   when (event) {
 *     is PickPhoto -> pickPhotoNavigator.goTo(PickPhotoScreen)
 *   }
 * }
 *
 * // In PickPhotoScreen
 * navigator.pop(PickPhotoScreen.Result(...))
 * ```
 */
@ExperimentalCircuitApi
@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  backStack: BackStack<out BackStack.Record>,
  answeringResultHandler: AnsweringResultHandler,
  resultType: KClass<T>,
  block: (result: T) -> Unit,
): GoToNavigator {
  val currentBackStack by rememberUpdatedState(backStack)
  val currentResultType by rememberUpdatedState(resultType)
  val currentAnsweringResultHandler by rememberUpdatedState(answeringResultHandler)

  // Top screen at the start, so we can ensure we only collect the result if
  // we've returned to this screen
  val initialRecordKey = rememberSaveable {
    currentBackStack.topRecord?.key ?: error("Navigator must have a top screen at start.")
  }

  // Key for the resultKey, so we can track who owns this requested result
  val key = rememberSaveable { @OptIn(ExperimentalUuidApi::class) Uuid.random().toString() }

  // Current top record of the navigator
  val currentTopRecordState by remember { derivedStateOf { currentBackStack.topRecord } }

  // Track whether we've actually gone to the next record yet
  var launched by rememberSaveable { mutableStateOf(false) }

  // Collect the result if we've launched and now returned to the initial record
  val currentTopRecord = currentTopRecordState
  if (launched && currentTopRecord != null && currentTopRecord.key == initialRecordKey) {
    LaunchedEffect(key) {
      val result =
        currentAnsweringResultHandler.awaitResult(currentTopRecord.key, key)
          ?: return@LaunchedEffect
      launched = false
      if (currentResultType.isInstance(result)) {
        @Suppress("UNCHECKED_CAST") block(result as T)
      }
    }
  }
  val answeringNavigator = remember {
    object : GoToNavigator {
      override fun goTo(screen: Screen): Boolean {
        val previousTopRecord = currentBackStack.topRecord
        val success = currentBackStack.push(screen)
        if (success) {
          // Clear the cached pending result from the previous top record
          if (previousTopRecord != null) {
            currentAnsweringResultHandler.prepareForResult(previousTopRecord.key, key)
          }
          launched = true
        }
        return success
      }
    }
  }
  return answeringNavigator
}
