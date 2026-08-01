// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import com.slack.circuit.runtime.internal.AnsweringNavigatorProvider
import com.slack.circuit.runtime.internal.LocalAnsweringNavigatorProvider
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@OptIn(InternalCircuitApi::class)
class AnsweringNavigatorTest {

  @Test
  fun `uses exact fallback and reports unavailable without provider`() = runTest {
    val fallbackNavigator = object : Navigator by Navigator.NoOp {}

    val state =
      moleculeFlow(RecompositionMode.Immediate) {
          AnsweringNavigationState(
            available = answeringNavigationAvailable(),
            navigator =
              rememberAnsweringNavigator<TestPopResult>(fallbackNavigator) {
                error("Result handler should not be called")
              },
          )
        }
        .first()

    assertFalse(state.available)
    assertSame(fallbackNavigator, state.navigator)
  }

  @Test
  fun `both overloads dispatch to provider`() = runTest {
    val provider = FakeAnsweringNavigatorProvider()
    val reifiedBlock: (TestPopResult) -> Unit = { error("Result handler should not be called") }
    val classBlock: (OtherTestPopResult) -> Unit = {
      error("Result handler should not be called")
    }

    val state =
      moleculeFlow(RecompositionMode.Immediate) {
          lateinit var reifiedNavigator: GoToNavigator
          lateinit var classNavigator: GoToNavigator
          var available = false
          CompositionLocalProvider(LocalAnsweringNavigatorProvider provides provider) {
            available = answeringNavigationAvailable()
            reifiedNavigator =
              rememberAnsweringNavigator(
                fallbackNavigator = Navigator.NoOp,
                block = reifiedBlock,
              )
            classNavigator =
              rememberAnsweringNavigator(
                fallbackNavigator = Navigator.NoOp,
                resultType = OtherTestPopResult::class,
                block = classBlock,
              )
          }
          HostedAnsweringNavigationState(available, reifiedNavigator, classNavigator)
        }
        .first()

    assertTrue(state.available)
    assertSame(provider.navigator, state.reifiedNavigator)
    assertSame(provider.navigator, state.classNavigator)
    assertEquals(
      listOf<KClass<*>>(TestPopResult::class, OtherTestPopResult::class),
      provider.resultTypes,
    )
    assertSame(reifiedBlock, provider.blocks[0])
    assertSame(classBlock, provider.blocks[1])
  }

  private data class AnsweringNavigationState(
    val available: Boolean,
    val navigator: GoToNavigator,
  )

  private data class HostedAnsweringNavigationState(
    val available: Boolean,
    val reifiedNavigator: GoToNavigator,
    val classNavigator: GoToNavigator,
  )

  private class FakeAnsweringNavigatorProvider : AnsweringNavigatorProvider {
    val navigator = FakeGoToNavigator()
    val resultTypes = mutableListOf<KClass<*>>()
    val blocks = mutableListOf<Any>()

    @Composable
    override fun <T : PopResult> rememberAnsweringNavigator(
      resultType: KClass<T>,
      block: (result: T) -> Unit,
    ): GoToNavigator {
      resultTypes += resultType
      blocks += block
      return navigator
    }
  }

  private data object TestPopResult : PopResult

  private data object OtherTestPopResult : PopResult
}

private class FakeGoToNavigator : GoToNavigator {
  override fun goTo(screen: Screen): Boolean = true
}
