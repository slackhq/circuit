// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedTransitionKey

/**
 * Internal key type for shared element transitions between nav stage pane layouts. Keyed by the
 * navigation item's key so matching items in outgoing/incoming stages share the same bounds.
 */
internal data class NavStagePaneKey(val itemKey: Any) : SharedTransitionKey

/**
 * Records currently composed by panes, including those still held by a pane transition's exit slot.
 * A record can only be composed in one place, so other non-primary states render these as
 * placeholders.
 *
 * Claims are taken during composition rather than in an effect, so two states composed in the same
 * pass cannot both claim a record that neither held before it.
 */
internal class ComposedRecords {
  private val claims = mutableListOf<Claim>()
  private var releases by mutableIntStateOf(0)

  fun isComposedByOther(itemKey: Any, owner: Any): Boolean {
    releases
    return claims.any { it.itemKey == itemKey && it.owner != owner }
  }

  @Composable
  fun Track(itemKey: Any, owner: Any) {
    remember(itemKey, owner) { Claim(itemKey, owner) }
  }

  private inner class Claim(val itemKey: Any, val owner: Any) : RememberObserver {
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
 * Places items for one rendered stage, identified by [owner]. Only the [isPrimary] state composes
 * records the target shows. Any other state renders those, and records [composedRecords] reports,
 * as placeholders.
 */
@OptIn(ExperimentalNavStageApi::class, ExperimentalSharedTransitionApi::class)
internal class NavStagePaneScopeImpl<T : NavArgument>(
  private val content: @Composable (T) -> Unit,
  private val navEvent: AnimatedNavEvent,
  private val owner: Any,
  private val isPrimary: Boolean,
  private val targetItemKeys: Set<Any>,
  private val composedRecords: ComposedRecords,
) : NavStagePaneScope<T> {

  @Composable
  override fun Pane(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    val backMotion = LocalPaneBackMotion.current
    val baseModifier =
      if (backMotion != null && item.key !in backMotion.unchangedItemKeys) {
        modifier.backMotion(backMotion.motion)
      } else {
        modifier
      }
    val isPlaceholder = isPlaceholder(item)

    if (!SharedElementTransitionScope.isAvailable) {
      // No shared elements available — render without shared bounds.
      // Placeholder mode still prevents the movableContent crash.
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

  private fun isPlaceholder(item: T): Boolean =
    !isPrimary && (item.key in targetItemKeys || composedRecords.isComposedByOther(item.key, owner))

  @Composable
  private fun RealPaneContent(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    val currentItemKey by rememberUpdatedState(item.key)
    transition.AnimatedPaneContent(
      targetItem = item,
      paneKey = key,
      navEvent = navEvent,
      modifier = modifier,
    ) { targetItem ->
      if (isPlaceholder(targetItem)) {
        Box(Modifier.fillMaxSize())
        return@AnimatedPaneContent
      }
      composedRecords.Track(targetItem.key, owner)
      // Every pane of the target is on screen, so each is active, not just the host's current
      // record. Previews and exit slots stay inactive.
      ProvideRecordLifecycle(isActive = isPrimary && targetItem.key == currentItemKey) {
        CompositionLocalProvider(LocalPaneBackMotion provides null) { content(targetItem) }
      }
    }
  }
}
