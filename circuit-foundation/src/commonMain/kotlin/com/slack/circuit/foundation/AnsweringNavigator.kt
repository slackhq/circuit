// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.benasher44.uuid.uuid4
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.DelicateCircuitApi
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

/**
 * Because we can't return early or return null inside the rememberSaveable block in
 * [rememberAnsweringNavigator], we use this instance as a marker for when we can't use it.
 */
private val UnusableRecord =
  object : BackStack.Record {
    override val key: String
      get() = "empty"

    override val screen: Screen
      get() = error("No screen")

    override suspend fun awaitResult(key: String) = null
  }

// TODO what about one that takes an AnsweringScreen<T> where T is the PopResult?
@OptIn(DelicateCircuitApi::class)
@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  navigator: Navigator,
  resultType: KClass<T>,
  block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {
  val backStack = navigator.backStack ?: return navigator

  // Top screen at the start, so we can ensure we only collect the result if
  // we've returned to this screen
  val initialRecord = rememberSaveable {
    // TODO is gracefully degrading the right thing to do here?
    when (val peeked = backStack.topRecord) {
      null -> {
        println("Navigator must have a top screen at start.")
        UnusableRecord
      }
      else -> peeked
    }
  }
  if (initialRecord == UnusableRecord) return navigator

  // Key for the resultKey, so we can track who owns this requested result
  val key = rememberSaveable { uuid4().toString() }

  // Current top record of the navigator
  val currentTopRecord by remember { derivedStateOf { backStack.topRecord } }

  // Track whether we've actually gone to the next record yet
  var launched by rememberSaveable { mutableStateOf(false) }

  // Collect the result if we've launched and now returned to the initial record
  if (launched && currentTopRecord == initialRecord) {
    LaunchedEffect(key) {
      // If we get a null result here, it's because either the real navigator
      // doesn't support results or something's wrong
      // TODO better to crash here?
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
