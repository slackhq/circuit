// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

/**
 * Where a record sits this pass, and whether its presenter runs. One value replaces a scope +
 * offScreen pair so the illegal "off screen yet carrying a transition scope" state can't be
 * represented.
 *
 * The two off-screen states differ only in presenter lifecycle: [Warming] runs the presenter so a
 * precomposing record attaches its interop views, then it flips to [Parked], which pauses the
 * presenter so a warmed or leftover off-screen screen does no active work (marking read, analytics,
 * autoplay). Both hide from accessibility.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal sealed interface Placement {
  /** On screen, active record, no animation. */
  data object Settled : Placement

  /** On screen, sliding or crossfading under [scope]'s [Navigation] visibility transition. */
  data class Transitioning(val scope: AnimatedVisibilityScope) : Placement

  /** Off screen and active: precomposing ahead of navigation so interop views attach. */
  data object Warming : Placement

  /** Off screen and paused: warmed and waiting, or a leftover from an interrupted slide. */
  data object Parked : Placement
}

/**
 * One record's host in the shared record [androidx.compose.ui.layout.SubcomposeLayout]. Its
 * [slotContent] is a single stable lambda instance reused for every [Placement] the record plays —
 * warmed off screen, sliding, and settled. A record is subcomposed by [key] with this same instance
 * each time, so `subcompose` adopts the existing node (`contentChanged == false`) instead of
 * disposing and recomposing it, which would re-register the record's shared `SaveableStateHolder`
 * `_registry_` key and throw.
 *
 * [placement] drives everything that varies by role, but `content(record)` is called from exactly
 * one site regardless. It is a `movableContentOf`, so branching around it relocates it, and
 * relocation removes the dropped group's LayoutNodes even though composition state survives —
 * detaching any interop view the record hosts, which crashed on the focus recovery the platform
 * then runs. A record that isn't transitioning gets a static always-visible scope instead of no
 * scope. That is what its descendants already resolved at rest anyway, since `findAnimatedScope`
 * falls back to the parent scope chain, which is settled whenever this record is. Both off-screen
 * states hide the record from accessibility and report it as not visible through
 * [com.slack.circuit.foundation.RecordLifecycle.isVisible]. The presenter runs unless
 * [Placement.Parked], so a warming record attaches interop views but a warmed or leftover one does
 * no work.
 *
 * A host is cached per key, so [content] is the lambda from the composition that first created it,
 * reused for the host's lifetime. This assumes Circuit's record `content` captures only
 * record-keyed state, so the first instance re-resolves identically to a later one; if that ever
 * stops holding, a cached host would render stale.
 */
@Stable
@OptIn(ExperimentalSharedTransitionApi::class)
internal class RecordHost<T : NavArgument>(
  private val record: T,
  private val content: @Composable ((T) -> Unit),
) {
  val key: String = record.key

  // Warming until a slot assigns a placement: a warm precompose runs slotContent before any slot is
  // built, and must run active (to attach interop) while hidden from a11y.
  var placement by mutableStateOf<Placement>(Placement.Warming)

  val slotContent: @Composable () -> Unit = {
    val placement = placement
    val offScreen = placement == Placement.Warming || placement == Placement.Parked
    val modifier =
      if (offScreen) {
        Modifier.clearAndSetSemantics { hideFromAccessibility() }
          .focusProperties { canFocus = false }
      } else {
        Modifier
      }
    Box(modifier) {
      // Paused only when parked; warming stays active so its interop views attach ahead of nav,
      // but reads as not visible so on-screen-only work waits for the record to arrive.
      ProvideRecordLifecycle(isActive = placement != Placement.Parked, isVisible = !offScreen) {
        // One call site for content(record): it is a movableContentOf, so a structural branch here
        // relocates it and removes its interop views' LayoutNodes. Built unconditionally, or the
        // branch comes back.
        val settledScope =
          SimpleAnimatedVisibilityScope(updateTransition(Visible, label = "settled"))
        val scope = (placement as? Placement.Transitioning)?.scope ?: settledScope
        ProvideAnimatedTransitionScope(Navigation, scope) { content(record) }
      }
    }
  }
}

/**
 * A warmed adjacent record and its [RecordHost]. Precompose and on-screen placement both use
 * [host]'s single [RecordHost.slotContent] instance, so navigation adopts the warmed node.
 *
 * @property record the adjacent record.
 * @property host the shared host for [record].
 */
internal class WarmAdjacent<T : NavArgument>(val record: T, val host: RecordHost<T>)

/**
 * Drops every host whose key isn't in [keep]. [keep] must cover every key that can still be
 * mounted: a host owns the single [RecordHost.slotContent] instance its record is subcomposed with,
 * so evicting one early mints a new lambda, which `subcompose` reads as changed content and
 * recomposes the slot, re-registering the record's `SaveableStateHolder` `_registry_` key.
 */
internal fun <T : NavArgument> MutableMap<String, RecordHost<T>>.retainKeys(keep: Set<String>) {
  keys.retainAll { it in keep }
}
