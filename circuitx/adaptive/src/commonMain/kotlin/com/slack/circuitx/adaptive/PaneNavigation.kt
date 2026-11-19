// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.runtime.Navigator
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator

/**
 * The parts of pane navigation that share one [PaneHost]. Pass [navigator] and [decoration] to
 * `NavigableCircuitContent`.
 *
 * Create one with [rememberPaneNavigation].
 */
@ExperimentalCircuitAdaptiveApi
@Stable
public class PaneNavigation
internal constructor(
  /** The state that [decoration] writes. Set [PaneHost.placeholder] here. */
  public val host: PaneHost,
  /** The navigator for `NavigableCircuitContent`. It runs the pane interceptors. */
  public val navigator: Navigator,
  /** The decoration for `NavigableCircuitContent`. */
  public val decoration: NavDecoration,
)

/**
 * Creates a [PaneNavDecoration] and a navigator with a [PaneReplaceInterceptor] that share one
 * [PaneHost].
 *
 * ```kotlin
 * val panes = rememberPaneNavigation(navigator)
 * NavigableCircuitContent(panes.navigator, backStack, decoration = panes.decoration)
 * ```
 *
 * Each parameter is a key of the result, so pass values that stay equal across recompositions. An
 * app that builds its own intercepting navigator can create the [PaneNavDecoration] and the
 * [PaneReplaceInterceptor] itself.
 *
 * @param navigator The navigator of the back stack that the panes render.
 * @param strategy Decides when to split and which records take the start and middle panes.
 * @param delegate Renders the single layout, and the end pane of a split layout. The default is the
 *   default decoration of the `Circuit` in the composition.
 * @param host The state that the decoration and the interceptors share.
 * @param interceptors The interceptors of the returned navigator. To add your own, pass a list that
 *   also has a [PaneReplaceInterceptor] for [host].
 * @param enableBackHandler Sends system back to the returned navigator as a pop, so [interceptors]
 *   see it. Pass false if no interceptor handles a pop.
 * @param window See [PaneNavDecoration].
 * @param divider See [PaneNavDecoration].
 * @param animationSpec See [PaneNavDecoration].
 * @param paneContainer See [PaneNavDecoration].
 */
@ExperimentalCircuitAdaptiveApi
@Composable
public fun rememberPaneNavigation(
  navigator: Navigator,
  strategy: PaneStrategy = remember { ListDetailPaneStrategy() },
  delegate: NavDecoration =
    checkNotNull(LocalCircuit.current) { "No Circuit in the composition" }.defaultNavDecoration,
  host: PaneHost = remember { PaneHost() },
  interceptors: List<NavigationInterceptor> =
    remember(host) { listOf(PaneReplaceInterceptor(host)) },
  enableBackHandler: Boolean = true,
  window: @Composable (DpSize) -> PaneWindow = { rememberPaneWindow(it) },
  divider: @Composable () -> Unit = {},
  animationSpec: FiniteAnimationSpec<Float> = DefaultPaneAnimationSpec,
  paneContainer: @Composable (position: PanePosition, content: @Composable () -> Unit) -> Unit =
    { _, content ->
      content()
    },
): PaneNavigation {
  val decoration =
    remember(strategy, delegate, host, window, divider, animationSpec, paneContainer) {
      PaneNavDecoration(
        strategy = strategy,
        delegate = delegate,
        host = host,
        window = window,
        divider = divider,
        animationSpec = animationSpec,
        paneContainer = paneContainer,
      )
    }
  val paneNavigator =
    rememberInterceptingNavigator(
      navigator = navigator,
      interceptors = interceptors,
      enableBackHandler = enableBackHandler,
    )
  return remember(host, paneNavigator, decoration) {
    PaneNavigation(host = host, navigator = paneNavigator, decoration = decoration)
  }
}
