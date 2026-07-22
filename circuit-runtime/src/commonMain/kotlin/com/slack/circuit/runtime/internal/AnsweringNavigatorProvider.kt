// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.screen.PopResult
import kotlin.reflect.KClass

/** Provides answering navigation for the public runtime APIs. */
@InternalCircuitApi
public interface AnsweringNavigatorProvider {
  /** Creates an answering navigator for [resultType]. */
  @Composable
  public fun <T : PopResult> rememberAnsweringNavigator(
    resultType: KClass<T>,
    block: (result: T) -> Unit,
  ): GoToNavigator
}

/** Holds the answering navigation provider installed by a Circuit content host. */
@InternalCircuitApi
public val LocalAnsweringNavigatorProvider:
  ProvidableCompositionLocal<AnsweringNavigatorProvider?> =
  compositionLocalOf {
    null
  }
