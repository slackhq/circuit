// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.Saver
import com.slack.circuit.retained.DelicateCircuitRetainedApi
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.retained.retain
import com.slack.circuit.retained.retainSaveable

@Composable
private fun compileUnkeyedRememberRetained(
  inputs: Array<out Any?>,
  init: () -> Any,
): Any = rememberRetained(*inputs, init = init)

@Suppress("DEPRECATION")
@Composable
private fun compileKeyedRememberRetainedReplacement(
  inputs: Array<out Any?>,
  key: String?,
  init: () -> Any,
): Any {
  rememberRetained(*inputs, key = key, init = init)
  return key.let { retainedKey ->
    if (retainedKey.isNullOrEmpty()) {
      rememberRetained(*inputs, init = init)
    } else {
      retain(*inputs, key = retainedKey, calculation = init)
    }
  }
}

@Composable
private fun compileRetainCalls(
  inputs: Array<out Any?>,
  key: String,
  init: () -> Any,
): Any {
  retain { init() }
  retain(*inputs) { init() }
  return retain(*inputs, key = key, calculation = init)
}

@OptIn(DelicateCircuitRetainedApi::class)
@Composable
private fun <T : Any> compileSupportedSaveableCalls(
  inputs: Array<out Any?>,
  saver: Saver<T, out Any>,
  key: String?,
  init: () -> T,
): T {
  rememberRetained(*inputs, saver = saver, key = key, init = init)
  rememberRetainedSaveable(*inputs, saver = saver, key = key, init = init)
  return retainSaveable(*inputs, saver = saver, key = key, calculation = init)
}

@OptIn(DelicateCircuitRetainedApi::class)
@Composable
private fun <T> compileSupportedStateSaveableCalls(
  inputs: Array<out Any?>,
  stateSaver: Saver<T, out Any>,
  key: String?,
  init: () -> MutableState<T>,
): MutableState<T> {
  rememberRetained(*inputs, stateSaver = stateSaver, key = key, init = init)
  rememberRetainedSaveable(*inputs, stateSaver = stateSaver, key = key, init = init)
  return retainSaveable(
    *inputs,
    stateSaver = stateSaver,
    key = key,
    calculation = init,
  )
}
