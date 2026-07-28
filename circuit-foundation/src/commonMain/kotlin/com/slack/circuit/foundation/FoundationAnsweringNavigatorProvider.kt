// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.AnsweringResultHandler
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.GoToNavigator
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.AnsweringNavigatorProvider
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.NavStack.Record
import com.slack.circuit.runtime.screen.PopResult
import kotlin.reflect.KClass

@OptIn(ExperimentalCircuitApi::class, InternalCircuitApi::class)
internal class FoundationAnsweringNavigatorProvider(
  private val navStack: NavStack<out Record>,
  private val answeringResultHandler: AnsweringResultHandler,
) : AnsweringNavigatorProvider {
  @Composable
  override fun <T : PopResult> rememberAnsweringNavigator(
    resultType: KClass<T>,
    block: (result: T) -> Unit,
  ): GoToNavigator = rememberAnsweringNavigator(navStack, answeringResultHandler, resultType, block)
}
