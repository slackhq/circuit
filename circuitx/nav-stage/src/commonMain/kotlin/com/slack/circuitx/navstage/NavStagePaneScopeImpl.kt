// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.foundation.ProvideRecordNavigator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedTransitionKey

/**
 * Internal key type for shared element transitions between nav stage pane layouts. Keyed by the
 * navigation item's key so matching items in outgoing/incoming stages share the same bounds.
 */
internal data class NavStagePaneKey(val itemKey: Any) : SharedTransitionKey

/**
 * Records currently composed by pane sites, including a pane transition's exit slot. A record can
 * only be composed in one place, so outside the target's entering sites the first site to claim one
 * keeps it and later sites render a placeholder.
 *
 * Claims are taken during composition rather than in an effect, so two sites composed in the same
 * pass cannot both claim a record that neither held before it.
 */
internal class ComposedRecords {
  private val claims = mutableListOf<Claim>()
  private var releases by mutableIntStateOf(0)

  fun isComposedByOther(itemKey: Any, site: Any): Boolean {
    releases
    return claims.any { it.itemKey == itemKey && it.site != site }
  }

  fun isClaimed(itemKey: Any): Boolean = claims.any { it.itemKey == itemKey }

  @Composable
  fun Track(itemKey: Any, site: Any) {
    remember(itemKey, site) { Claim(itemKey, site) }
  }

  private inner class Claim(val itemKey: Any, val site: Any) : RememberObserver {
    init {
      claims += this
    }

    override fun onRemembered() = Unit

    override fun onForgotten() {
      claims -= this
      releases++
    }

    override fun onAbandoned() {
      claims -= this
    }
  }
}

/**
 * Places items for one rendered stage. Only the [isPrimary] state composes records the target
 * shows, and only from the pane transition child entering with them, so a record moving between
 * panes or stage slots moves in the same composition pass and keeps its state. Every other site
 * renders those records as placeholders, and composes the rest unless [composedRecords] reports
 * another site already holds them.
 */
@OptIn(
  ExperimentalNavStageApi::class,
  ExperimentalSharedTransitionApi::class,
  ExperimentalCircuitApi::class,
)
internal class NavStagePaneScopeImpl<T : NavArgument>(
  private val content: @Composable (T) -> Unit,
  private val navEvent: AnimatedNavEvent,
  private val stageKey: Any,
  private val itemKeys: Set<Any>,
  private val isPrimary: Boolean,
  private val targetItemKeys: Set<Any>,
  private val composedRecords: ComposedRecords,
  private val paneNavigators: PaneNavigators<T>,
  private val navigationPolicy: NavStageNavigationPolicy,
) : NavStagePaneScope<T> {

  @Composable
  override fun Pane(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    require(item.key in itemKeys) {
      "NavStage '$stageKey' placed item '${item.key}' in pane '$key', but it isn't one of the " +
        "items passed to Content."
    }
    key(key) { KeyedPane(key, item, modifier, transition) }
  }

  @Composable
  private fun KeyedPane(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    val backMotion = LocalPaneBackMotion.current
    val baseModifier =
      if (backMotion != null && item.key !in backMotion.unchangedItemKeys) {
        modifier.backMotion(backMotion.motion)
      } else {
        modifier
      }
    val isPlaceholder = !isPrimary && item.key in targetItemKeys

    if (!SharedElementTransitionScope.isAvailable) {
      if (isPlaceholder) {
        Box(baseModifier.fillMaxSize())
      } else {
        RealPaneContent(key, item, baseModifier, transition)
      }
      return
    }

    SharedElementTransitionScope {
      val animatedScope = findAnimatedScope(Navigation)
      val paneModifier =
        if (animatedScope != null) {
          baseModifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = NavStagePaneKey(item.key)),
            animatedVisibilityScope = animatedScope,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
          )
        } else {
          baseModifier
        }

      if (isPlaceholder) {
        // Empty box with shared bounds — the shared element system animates these bounds
        // to/from the matching pane in the incoming/outgoing stage.
        Box(paneModifier.fillMaxSize())
      } else {
        RealPaneContent(key, item, paneModifier, transition)
      }
    }
  }

  @Composable
  private fun RealPaneContent(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    val currentItemKey by rememberUpdatedState(item.key)
    ProvidePaneWindowInfo(modifier) {
      transition.AnimatedPaneContent(targetItem = item, paneKey = key, navEvent = navEvent) {
        targetItem ->
        val site = remember { Any() }
        val isCurrent =
          isPrimary &&
            targetItem.key == currentItemKey &&
            this.transition.targetState == EnterExitState.Visible
        val composes =
          isCurrent ||
            (targetItem.key !in targetItemKeys &&
              !composedRecords.isComposedByOther(targetItem.key, site))
        if (!composes) {
          Box(Modifier.fillMaxSize())
          return@AnimatedPaneContent
        }
        composedRecords.Track(targetItem.key, site)
        val navigator = paneNavigators.navigatorFor(targetItem.key)
        if (isCurrent) {
          SideEffect { navigator.place(stageKey, key, targetItem.screen, navigationPolicy) }
        }
        ProvideAnimatedTransitionScope(PaneAnimatedScope, this) {
          // Every pane of the target is on screen, so each is active, not just the host's current
          // record. Previews and exit slots stay inactive.
          ProvideRecordLifecycle(isActive = isCurrent) {
            ProvideRecordNavigator(navigator) {
              CompositionLocalProvider(LocalPaneBackMotion provides null) { content(targetItem) }
            }
          }
        }
      }
    }
  }
}
