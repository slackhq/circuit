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
import com.benasher44.uuid.uuid4
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

@Composable public fun answeringNavigationAvailable(): Boolean = LocalBackStack.current != null

@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  noinline block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator = rememberAnsweringNavigator(fallbackNavigator, T::class, block)

@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  fallbackNavigator: Navigator,
  resultType: KClass<T>,
  block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {
  val backStack = LocalBackStack.current ?: return fallbackNavigator
  return rememberAnsweringNavigator(backStack, resultType, block)
}

@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  backStack: BackStack<out BackStack.Record>,
  noinline block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {
  return rememberAnsweringNavigator(backStack, T::class, block)
}

@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  backStack: BackStack<out BackStack.Record>,
  resultType: KClass<T>,
  block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {
  val currentBackStack by rememberUpdatedState(backStack)
  val currentResultType by rememberUpdatedState(resultType)

  // Top screen at the start, so we can ensure we only collect the result if
  // we've returned to this screen
  val initialRecordKey = rememberSaveable {
    currentBackStack.topRecord?.key ?: error("Navigator must have a top screen at start.")
  }

  // Key for the resultKey, so we can track who owns this requested result
  val key = rememberSaveable { uuid4().toString() }

  // Current top record of the navigator
  val currentTopRecordKey by remember { derivedStateOf { currentBackStack.topRecord!!.key } }

  // Track whether we've actually gone to the next record yet
  var launched by rememberSaveable { mutableStateOf(false) }

  // Collect the result if we've launched and now returned to the initial record
  if (launched && currentTopRecordKey == initialRecordKey) {
    val record = currentBackStack.topRecord!!
    LaunchedEffect(key) {
      val result = record.awaitResult(key) ?: return@LaunchedEffect
      if (currentResultType.isInstance(result)) {
        @Suppress("UNCHECKED_CAST") block(result as T)
      }
    }
  }
  val answeringNavigator = remember {
    object : GoToNavigator {
      override fun goTo(screen: Screen) {
        currentBackStack.push(screen, key)
        launched = true
      }
    }
  }
  return answeringNavigator
}
