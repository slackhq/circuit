// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for the rules in the [NavDecoration] KDoc. */
@OptIn(ExperimentalTestApi::class, ExperimentalCircuitApi::class)
@RunWith(ComposeUiTestRunner::class)
class NavDecorationContractTest {

  /** What each screen's UI last reported, by screen. */
  private val reports = mutableMapOf<Screen, Report>()
  private var nextToken = 0
  private lateinit var navigator: Navigator

  private val circuit =
    Circuit.Builder()
      .addPresenter<TestScreen, ContractState> { screen, _, _ -> ContractPresenter(screen) }
      .addPresenter<TestScreen2, ContractState> { screen, _, _ -> ContractPresenter(screen) }
      .addUi<TestScreen, ContractState> { state, _ -> ReportingUi(state.screen) }
      .addUi<TestScreen2, ContractState> { state, _ -> ReportingUi(state.screen) }
      .build()

  @Test
  fun `record keeps its state when its content call moves`() = runComposeUiTest {
    var split by mutableStateOf(false)
    setNavContent(SideBySideDecoration(split = { split }, rootActive = true))
    waitForIdle()
    val before = reports.getValue(TestScreen)

    // The root now renders from the first call site.
    split = true
    navigator.goTo(TestScreen2)
    waitForIdle()
    assertEquals(before.remembered, reports.getValue(TestScreen).remembered)
    assertEquals(before.saved, reports.getValue(TestScreen).saved)

    // The root moves back to the second call site.
    navigator.pop()
    waitForIdle()
    assertEquals(before.remembered, reports.getValue(TestScreen).remembered)
    assertEquals(before.saved, reports.getValue(TestScreen).saved)
  }

  @Test
  fun `record below the current record is paused by default`() = runComposeUiTest {
    setNavContent(SideBySideDecoration(split = { true }, rootActive = null))
    navigator.goTo(TestScreen2)
    waitForIdle()
    assertFalse(reports.getValue(TestScreen).active)
    assertTrue(reports.getValue(TestScreen2).active)
  }

  @Test
  fun `ProvideRecordLifecycle keeps a record below the current record active`() = runComposeUiTest {
    setNavContent(SideBySideDecoration(split = { true }, rootActive = true))
    navigator.goTo(TestScreen2)
    waitForIdle()
    assertTrue(reports.getValue(TestScreen).active)
    assertTrue(reports.getValue(TestScreen2).active)
  }

  @Test
  fun `wrapped and synthetic arguments render through another decoration`() = runComposeUiTest {
    var syntheticShown = false
    val decoration =
      WrappingDecoration(
        delegate = AnimatedNavDecoration(emptyMap(), NavigatorDefaults.DefaultDecoratorFactory),
        synthetic = { SideEffect { syntheticShown = true } },
      )
    setNavContent(decoration)
    waitForIdle()
    // Only the synthetic argument is in the delegate's stack while the root is alone.
    assertTrue(syntheticShown)
    assertFalse(TestScreen in reports)

    navigator.goTo(TestScreen2)
    waitForIdle()
    assertTrue(TestScreen2 in reports)

    syntheticShown = false
    navigator.pop()
    waitForIdle()
    assertTrue(syntheticShown)
  }

  private fun ComposeUiTest.setNavContent(decoration: NavDecoration) {
    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen)
        navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        NavigableCircuitContent(
          navigator = navigator,
          backStack = backStack,
          decoration = decoration,
        )
      }
    }
  }

  @Composable
  private fun ReportingUi(screen: Screen) {
    val remembered = remember { nextToken++ }
    val saved = rememberSaveable { nextToken++ }
    val active = LocalRecordLifecycle.current.isActive
    SideEffect { reports[screen] = Report(remembered, saved, active) }
  }
}

private data class Report(val remembered: Int, val saved: Int, val active: Boolean)

private data class ContractState(val screen: Screen) : CircuitUiState

private fun ContractPresenter(screen: Screen) = presenterOf { ContractState(screen) }

/**
 * Shows the current record alone, or the root beside it while [split] returns true. The current
 * record always renders from the second call site, so the root changes call site when it stops
 * being the current record.
 *
 * @param rootActive The lifecycle to set on the root in the first call site, or null to set none.
 */
private class SideBySideDecoration(
  private val split: () -> Boolean,
  private val rootActive: Boolean?,
) : NavDecoration {
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    Row(modifier) {
      if (split() && args.root.key != args.active.key) {
        Box {
          if (rootActive != null) {
            ProvideRecordLifecycle(isActive = rootActive) { content(args.root) }
          } else {
            content(args.root)
          }
        }
      }
      Box { content(args.active) }
    }
  }
}

/**
 * Passes every record above the root to [delegate] in a wrapper, with a synthetic argument in place
 * of the root.
 */
private class WrappingDecoration(
  private val delegate: NavDecoration,
  private val synthetic: @Composable () -> Unit,
) : NavDecoration {
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val wrapped =
      remember(args) {
        val aboveRoot = (listOf(args.active) + args.backwardItems).dropLast(1)
        val placeholder: WrappedArg<T> = WrappedArg.Synthetic
        if (aboveRoot.isEmpty()) {
          navStackListOf(activeItem = placeholder)
        } else {
          navStackListOf(
            activeItem = WrappedArg.Record(aboveRoot.first()),
            backwardItems = aboveRoot.drop(1).map { WrappedArg.Record(it) } + placeholder,
          )
        }
      }
    delegate.DecoratedContent(wrapped, navigator, modifier) { arg ->
      when (arg) {
        is WrappedArg.Record -> content(arg.record)
        WrappedArg.Synthetic -> synthetic()
      }
    }
  }
}

private sealed interface WrappedArg<out T : NavArgument> : NavArgument {
  data class Record<T : NavArgument>(val record: T) : WrappedArg<T> {
    override val key: String
      get() = record.key

    override val screen: Screen
      get() = record.screen
  }

  data object Synthetic : WrappedArg<Nothing> {
    override val key: String = "synthetic"
    override val screen: Screen = SyntheticScreen
  }
}

private data object SyntheticScreen : Screen
