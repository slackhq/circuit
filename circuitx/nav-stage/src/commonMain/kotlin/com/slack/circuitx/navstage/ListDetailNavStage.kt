// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * Strategy that activates [ListDetailNavStage] when [isMultiPane] is true, the active screen is a
 * detail pane, and the back stack contains a list pane.
 *
 * [isMultiPane] defaults to a medium-or-wider width for the space the decoration is given. Override
 * it to supply your own breakpoint. [listGoTo] decides what navigating from the list pane does to
 * the detail it's showing.
 */
@ExperimentalNavStageApi
public class ListDetailNavStageStrategy(
  private val isListPane: (Screen) -> Boolean,
  private val isDetailPane: (Screen) -> Boolean,
  private val isMultiPane: @Composable () -> Boolean = { DefaultIsMultiPane() },
  private val listTransition: (Screen) -> PaneTransition = { PaneTransition.None },
  private val detailTransition: (Screen) -> PaneTransition = { PaneTransition.Default },
  private val listGoTo: ListDetailNavStage.ListGoTo = ListDetailNavStage.ListGoTo.ReplaceDetail,
) : NavStageStrategy {

  @Composable
  override fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>? {
    if (!isMultiPane()) return null
    if (!isDetailPane(args.active.screen)) return null
    if (args.backwardItems.none { isListPane(it.screen) }) return null
    // Remembered so the stage keeps a stable identity while this layout is in use.
    return remember(isListPane, listTransition, detailTransition, listGoTo) {
      ListDetailNavStage(
        isListPane = isListPane,
        listTransition = listTransition,
        detailTransition = detailTransition,
        listGoTo = listGoTo,
      )
    }
  }

  public companion object {
    /** The default multi-pane gate: at least 600dp of width, per [currentPaneWindowDpSize]. */
    @Composable public fun DefaultIsMultiPane(): Boolean = currentPaneWindowDpSize().width >= 600.dp
  }
}

/**
 * Dual-pane stage that renders a list pane (40%) beside a detail pane (60%) in a horizontal row.
 *
 * Falls back to rendering only the active item when the stack holds no list pane, which happens
 * while a transition renders an older stack than the one this stage was resolved from.
 *
 * Its [navigationPolicy] treats the list pane as the owner of the detail beside it. Popping from
 * the list pane pops the detail stack above it too, and navigating from it follows [listGoTo].
 * Calls from the detail pane pass straight through.
 */
@ExperimentalNavStageApi
public class ListDetailNavStage<T : NavArgument>(
  private val isListPane: (Screen) -> Boolean,
  private val listTransition: (Screen) -> PaneTransition = { PaneTransition.None },
  private val detailTransition: (Screen) -> PaneTransition = { PaneTransition.Default },
  private val listGoTo: ListGoTo = ListGoTo.ReplaceDetail,
) : NavStage<T> {
  override val key: Any = LIST_DETAIL_STAGE_KEY

  override val navigationPolicy: NavStageNavigationPolicy =
    object : NavStageNavigationPolicy {
      override fun goTo(source: NavStagePaneSource, screen: Screen, navigator: Navigator): Boolean {
        if (listGoTo == ListGoTo.ReplaceDetail && source.isListAboveTop()) {
          repeat(source.depth) { navigator.pop() }
        }
        return navigator.goTo(screen)
      }

      override fun pop(
        source: NavStagePaneSource,
        result: PopResult?,
        navigator: Navigator,
      ): Screen? {
        if (source.isListAboveTop()) {
          repeat(source.depth) { navigator.pop() }
        }
        return navigator.pop(result)
      }
    }

  private fun NavStagePaneSource.isListAboveTop(): Boolean =
    paneKey == LIST_DETAIL_LIST_PANE_KEY && depth > 0

  /** What navigating from the list pane does to the detail stack above it. */
  public enum class ListGoTo {
    /** Pushes on top of the detail, keeping it in the back stack. */
    Push,

    /** Pops back to the list first, so the new screen replaces the detail. */
    ReplaceDetail,
  }

  override fun visibleItems(args: NavStackList<T>): List<T> {
    val listItem =
      args.backwardItems.firstOrNull { isListPane(it.screen) } ?: return listOf(args.active)
    return listOf(listItem, args.active)
  }

  @Composable
  override fun Content(items: List<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    if (items.size < 2) {
      val detailItem = items.single()
      Box(modifier.fillMaxSize()) {
        paneScope.Pane(
          key = LIST_DETAIL_DETAIL_PANE_KEY,
          item = detailItem,
          transition = detailTransition(detailItem.screen),
        )
      }
      return
    }
    val (listItem, detailItem) = items
    Row(modifier.fillMaxSize()) {
      paneScope.Pane(
        key = LIST_DETAIL_LIST_PANE_KEY,
        item = listItem,
        modifier = Modifier.weight(0.4f),
        transition = listTransition(listItem.screen),
      )
      paneScope.Pane(
        key = LIST_DETAIL_DETAIL_PANE_KEY,
        item = detailItem,
        modifier = Modifier.weight(0.6f),
        transition = detailTransition(detailItem.screen),
      )
    }
  }
}

private const val LIST_DETAIL_STAGE_KEY = "com.slack.circuitx.navstage.list-detail"
private const val LIST_DETAIL_LIST_PANE_KEY = "com.slack.circuitx.navstage.list-detail.list"
private const val LIST_DETAIL_DETAIL_PANE_KEY = "com.slack.circuitx.navstage.list-detail.detail"
