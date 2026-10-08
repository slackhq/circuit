// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * Where a navigation call came from: the record shown in pane [paneKey] of stage [stageKey].
 *
 * [depth] is the record's position in the host stack when the call was made, `0` for the top and
 * `-1` if it's no longer in the back stack (e.g. still animating out).
 */
@ExperimentalNavStageApi
public class NavStagePaneSource
internal constructor(
  public val stageKey: Any,
  public val paneKey: Any,
  public val screen: Screen,
  public val depth: Int,
) {
  /** Whether the record is the host's top record. */
  public val isTop: Boolean
    get() = depth == 0

  override fun toString(): String =
    "NavStagePaneSource(stageKey=$stageKey, paneKey=$paneKey, screen=$screen, depth=$depth)"
}

/**
 * Decides what navigation calls from a [NavStage]'s panes do to the host stack.
 *
 * Each record rendered in a pane gets its own [Navigator] whose calls land here with the
 * [NavStagePaneSource] they came from, along with the host [navigator] to apply them to. `peek`
 * calls aren't routed and always see the real host stack. System back and
 * [GestureNavStageTransition] go through [pop] with the top pane as the source.
 *
 * Every method defaults to passing the call straight to [navigator].
 */
@Stable
@ExperimentalNavStageApi
public interface NavStageNavigationPolicy {
  public fun goTo(source: NavStagePaneSource, screen: Screen, navigator: Navigator): Boolean =
    navigator.goTo(screen)

  public fun pop(source: NavStagePaneSource, result: PopResult?, navigator: Navigator): Screen? =
    navigator.pop(result)

  public fun resetRoot(
    source: NavStagePaneSource,
    newRoot: Screen,
    options: StateOptions,
    navigator: Navigator,
  ): List<Screen> = navigator.resetRoot(newRoot, options)

  public fun forward(source: NavStagePaneSource, navigator: Navigator): Boolean =
    navigator.forward()

  public fun backward(source: NavStagePaneSource, navigator: Navigator): Boolean =
    navigator.backward()

  public companion object {
    /** Passes every call straight to the host navigator. */
    public val Passthrough: NavStageNavigationPolicy = object : NavStageNavigationPolicy {}
  }
}

/**
 * The [Navigator] handed to the record [itemKey]. Stays the same instance wherever the record is
 * rendered, since presenters are keyed on their navigator, and routes calls through the policy of
 * the pane that last placed it.
 */
@OptIn(ExperimentalNavStageApi::class)
internal class PaneNavigator<T : NavArgument>(
  private val itemKey: Any,
  private val navigators: PaneNavigators<T>,
) : Navigator {
  private var placement: Placement? = null

  fun place(stageKey: Any, paneKey: Any, screen: Screen, policy: NavStageNavigationPolicy) {
    placement = Placement(stageKey, paneKey, screen, policy)
  }

  private val host: Navigator
    get() = navigators.host

  private inline fun <R> route(
    passthrough: () -> R,
    block: NavStageNavigationPolicy.(NavStagePaneSource) -> R,
  ): R {
    val placement = placement ?: return passthrough()
    val depth = navigators.args?.let { depthOf(it, itemKey) } ?: -1
    val source = NavStagePaneSource(placement.stageKey, placement.paneKey, placement.screen, depth)
    return placement.policy.block(source)
  }

  override fun goTo(screen: Screen): Boolean =
    route({ host.goTo(screen) }) { goTo(it, screen, host) }

  override fun pop(result: PopResult?): Screen? =
    route({ host.pop(result) }) { pop(it, result, host) }

  override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> =
    route({ host.resetRoot(newRoot, options) }) { resetRoot(it, newRoot, options, host) }

  override fun forward(): Boolean = route({ host.forward() }) { forward(it, host) }

  override fun backward(): Boolean = route({ host.backward() }) { backward(it, host) }

  override fun peek(): Screen? = host.peek()

  override fun peekBackStack(): List<Screen> = host.peekBackStack()

  override fun peekNavStack(): NavStackList<Screen>? = host.peekNavStack()

  private class Placement(
    val stageKey: Any,
    val paneKey: Any,
    val screen: Screen,
    val policy: NavStageNavigationPolicy,
  )
}

/** Index of [itemKey] in [args]'s active and backward items, or `-1` if it isn't there. */
internal fun <T : NavArgument> depthOf(args: NavStackList<T>, itemKey: Any): Int {
  if (args.active.key == itemKey) return 0
  args.backwardItems.forEachIndexed { index, item -> if (item.key == itemKey) return index + 1 }
  return -1
}

/** One [PaneNavigator] per record, kept until the record leaves both the stack and composition. */
internal class PaneNavigators<T : NavArgument> {
  private val navigators = mutableMapOf<Any, PaneNavigator<T>>()
  var host: Navigator = Navigator.NoOp
  var args: NavStackList<T>? = null

  fun navigatorFor(itemKey: Any): PaneNavigator<T> =
    navigators.getOrPut(itemKey) { PaneNavigator(itemKey, this) }

  fun prune(keep: (Any) -> Boolean) {
    navigators.keys.retainAll(keep)
  }
}
