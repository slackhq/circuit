// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.benasher44.uuid.uuid4
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

@Composable
public inline fun <reified T : PopResult> rememberAnsweringNavigator(
  navigator: Navigator,
  noinline block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator = rememberAnsweringNavigator(navigator, T::class, block)

@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  navigator: Navigator,
  resultType: KClass<T>,
  block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {
  // This only works with SaveableBackStack
  // TODO error?
  val backStack = LocalBackStack.current ?: return navigator
  if (backStack !is SaveableBackStack) return navigator
  return rememberAnsweringNavigator(backStack, SaveableBackStack.Record.Saver, resultType, block)
}

@Composable
public fun <T : PopResult, R : BackStack.Record, B : BackStack<R>> rememberAnsweringNavigator(
  backStack: B,
  recordSaver: Saver<R, Any>,
  resultType: KClass<T>,
  block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {

  // Top screen at the start, so we can ensure we only collect the result if
  // we've returned to this screen
  val initialRecord =
    rememberSaveable(saver = recordSaver) {
      backStack.topRecord ?: error("Navigator must have a top screen at start.")
    }

  // Key for the resultKey, so we can track who owns this requested result
  val key = rememberSaveable { uuid4().toString() }

  // Current top record of the navigator
  val currentTopRecord by remember { derivedStateOf { backStack.topRecord } }

  // Track whether we've actually gone to the next record yet
  var launched by rememberSaveable { mutableStateOf(false) }

  // Collect the result if we've launched and now returned to the initial record
  if (launched && currentTopRecord == initialRecord) {
    LaunchedEffect(key) {
      val result = initialRecord.awaitResult(key) ?: return@LaunchedEffect
      if (resultType.isInstance(result)) {
        @Suppress("UNCHECKED_CAST") block(result as T)
      }
    }
  }
  val answeringNavigator = remember {
    object : GoToNavigator {
      override fun goTo(screen: Screen) {
        backStack.push(screen, key)
        launched = true
      }
    }
  }
  return answeringNavigator
}
