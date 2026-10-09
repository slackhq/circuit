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
import com.slack.circuit.runtime.AnsweringResultHandler as RuntimeAnsweringResultHandler
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.answeringNavigationAvailable as runtimeAnsweringNavigationAvailable
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.rememberAnsweringNavigator as rememberRuntimeAnsweringNavigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Returns whether or not answering navigation is available. This is essentially a proxy for whether
 * or not this composition is running within a [NavigableCircuitContent].
 */
@Deprecated(
  "Moved to com.slack.circuit.runtime.",
  ReplaceWith(
    "answeringNavigationAvailable()",
    "com.slack.circuit.runtime.answeringNavigationAvailable",
  ),
)
@Composable
public fun answeringNavigationAvailable(): Boolean = runtimeAnsweringNavigationAvailable()

/**
 * A reified version of [rememberAnsweringNavigator]. See documented overloads of this function for
 * more information.
 */
@Deprecated(
  "Moved to com.slack.circuit.runtime.",
  ReplaceWith(
    "rememberAnsweringNavigator<T>(fallbackNavigator, block)",
    "com.slack.circuit.runtime.rememberAnsweringNavigator",
  ),
)
@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  noinline block: (result: T) -> Unit,
): GoToNavigator = rememberRuntimeAnsweringNavigator<T>(fallbackNavigator, block)

/**
 * Returns a [GoToNavigator] that answers with the given [resultType] or defaults to
 * [fallbackNavigator] if no back stack is available to pass results through.
 */
@Deprecated(
  "Moved to com.slack.circuit.runtime.",
  ReplaceWith(
    "rememberAnsweringNavigator(fallbackNavigator, resultType, block)",
    "com.slack.circuit.runtime.rememberAnsweringNavigator",
  ),
)
@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  resultType: KClass<T>,
  block: (result: T) -> Unit,
): GoToNavigator = rememberRuntimeAnsweringNavigator(fallbackNavigator, resultType, block)

/**
 * A reified version of [rememberAnsweringNavigator]. See documented overloads of this function for
 * more information.
 */
@ExperimentalCircuitApi
@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  navStack: NavStack<out NavStack.Record>,
  answeringResultHandler: RuntimeAnsweringResultHandler,
  noinline block: (result: T) -> Unit,
): GoToNavigator {
  return rememberAnsweringNavigator(navStack, answeringResultHandler, T::class, block)
}

/**
 * Returns a [GoToNavigator] that answers with the given [resultType] using the given [navStack].
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
  navStack: NavStack<out NavStack.Record>,
  answeringResultHandler: RuntimeAnsweringResultHandler,
  resultType: KClass<T>,
  block: (result: T) -> Unit,
): GoToNavigator =
  rememberAnsweringNavigator(
    navStack = navStack,
    answeringResultHandler = answeringResultHandler,
    resultType = resultType,
    block = block,
    ownerRecordKey = null,
    navigator = null,
  )

/**
 * [ownerRecordKey] is the record that asked for the result, and gets it back from whichever record
 * it launched. Null means the top record at first composition, which only receives the result once
 * it's top again. A non-null [navigator] performs the `goTo` instead of pushing on [navStack].
 */
@OptIn(ExperimentalCircuitApi::class)
@Composable
internal fun <T : PopResult> rememberAnsweringNavigator(
  navStack: NavStack<out NavStack.Record>,
  answeringResultHandler: RuntimeAnsweringResultHandler,
  resultType: KClass<T>,
  block: (result: T) -> Unit,
  ownerRecordKey: String?,
  navigator: Navigator?,
): GoToNavigator {
  val currentBackStack by rememberUpdatedState(navStack)
  val currentResultType by rememberUpdatedState(resultType)
  val currentAnsweringResultHandler by rememberUpdatedState(answeringResultHandler)
  val currentNavigator by rememberUpdatedState(navigator)

  // Top screen at the start, so we can ensure we only collect the result if
  // we've returned to this screen
  val initialRecordKey = rememberSaveable {
    ownerRecordKey
      ?: currentBackStack.currentRecord?.key
      ?: error("Navigator must have a top screen at start.")
  }
  val tracksLaunchedRecord = ownerRecordKey != null

  // Key for the resultKey, so we can track who owns this requested result
  val key = rememberSaveable { @OptIn(ExperimentalUuidApi::class) Uuid.random().toString() }

  // Current top record of the navigator
  val currentRecordState by remember { derivedStateOf { currentBackStack.currentRecord } }

  // Track whether we've actually gone to the next record yet
  var launched by rememberSaveable { mutableStateOf(false) }
  var launchedRecordKey by rememberSaveable { mutableStateOf<String?>(null) }
  val launchedRecordGone by remember {
    derivedStateOf {
      val launchedKey = launchedRecordKey ?: return@derivedStateOf false
      currentBackStack.snapshot()?.none { it.key == launchedKey } ?: true
    }
  }

  // Collect the result if we've launched and now returned to the initial record, or the record we
  // launched has left while the owner is still composed
  val currentRecord = currentRecordState
  if (
    launched &&
      currentRecord != null &&
      (currentRecord.key == initialRecordKey || (tracksLaunchedRecord && launchedRecordGone))
  ) {
    LaunchedEffect(key) {
      val result = currentAnsweringResultHandler.awaitResult(initialRecordKey, key)
      launched = false
      if (currentResultType.isInstance(result)) {
        @Suppress("UNCHECKED_CAST") block(result as T)
      }
    }
  }
  val answeringNavigator = remember {
    object : GoToNavigator {
      override fun goTo(screen: Screen): Boolean {
        val previousRecord = currentBackStack.currentRecord
        val previousKeys =
          if (tracksLaunchedRecord) currentBackStack.snapshot()?.mapTo(HashSet()) { it.key }
          else null
        val success = currentNavigator?.goTo(screen) ?: currentBackStack.push(screen)
        if (success) {
          if (tracksLaunchedRecord) {
            val launchedKey = currentBackStack.currentRecord?.key
            if (launchedKey == null || previousKeys?.contains(launchedKey) == true) return true
            launchedRecordKey = launchedKey
            currentAnsweringResultHandler.prepareForResult(initialRecordKey, key, launchedKey)
          } else if (previousRecord != null) {
            // Clear the cached pending result from the previous top record
            currentAnsweringResultHandler.prepareForResult(previousRecord.key, key)
          }
          launched = true
        }
        return success
      }
    }
  }
  return answeringNavigator
}
