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
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

// TODO what about one that takes an AnsweringScreen<T> where T is the PopResult?
@Composable
public fun <T : PopResult> rememberAnsweringNavigator(
  navigator: Navigator,
  resultType: KClass<T>,
  block: suspend CoroutineScope.(result: T) -> Unit,
): GoToNavigator {
  // Key for the resultKey, so we can track who owns this requested result
  val key = rememberSaveable { uuid4().toString() }

  // Top screen at the start, so we can ensure we only collect the result if
  // we've returned to this screen
  val topAtStart = rememberSaveable {
    navigator.peek() ?: error("Navigator must have a top screen at start.")
  }

  // Current top screen of the navigator
  val currentTopScreen by remember { derivedStateOf { navigator.peek() } }

  // Track whether we've actually gone to the next screen yet
  var launched by rememberSaveable { mutableStateOf(false) }

  if (launched && currentTopScreen == topAtStart) {
    // Collect the result
    LaunchedEffect(key) {
      // If we get a null result here, it's because either the real navigator
      // doesn't support results or something's wrong
      // TODO better to crash here?
      val result = navigator.awaitResult(key) ?: return@LaunchedEffect
      if (resultType.isInstance(result)) {
        @Suppress("UNCHECKED_CAST") block(result as T)
      }
    }
  }
  val answeringNavigator = remember {
    object : GoToNavigator {
      override fun goTo(screen: Screen) {
        navigator.goToForResult(screen, key)
        launched = true
      }
    }
  }
  return answeringNavigator
}
