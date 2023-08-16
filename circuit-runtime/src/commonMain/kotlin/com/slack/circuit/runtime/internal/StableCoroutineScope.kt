// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

/**
 * Returns a [StableCoroutineScope] around a [rememberCoroutineScope]. This is useful for event
 * callback lambdas that capture a local scope variable to launch new coroutines, as it allows them
 * to be stable.
 */
@Composable
public fun rememberStableCoroutineScope(): StableCoroutineScope {
  val scope = rememberCoroutineScope()
  return remember { StableCoroutineScope(scope) }
}

/** @see rememberStableCoroutineScope */
@Stable public class StableCoroutineScope(scope: CoroutineScope) : CoroutineScope by scope
