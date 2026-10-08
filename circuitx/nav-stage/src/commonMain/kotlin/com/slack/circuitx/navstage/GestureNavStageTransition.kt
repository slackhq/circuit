// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.math.abs
import kotlinx.coroutines.CancellationException

/**
 * A [NavStageTransition] that drives predictive back gestures with Material motion.
 *
 * During the gesture the current stage scales down and translates in the swipe direction while the
 * stage for the popped stack is shown behind it. Records the current stage already shows render as
 * shared-bounds placeholders in the previous stage. The gesture seeks a [SeekableTransitionState]
 * so shared elements track it. Calls [onBack] when the gesture completes, or [Navigator.pop] if
 * [onBack] is null. The preview assumes the stack is popped, so [onBack] should pop too.
 *
 * When the popped stack keeps the same stage layout, only the panes whose item changes move, so a
 * list stays put while its detail is swiped away. Other navigation within a stage is left to each
 * pane's [PaneTransition]. A change of stage layout swaps instantly.
 *
 * This drives the gesture directly rather than through Circuit's `AnimatedNavDecoration`, so its
 * Material treatment applies on every platform, not just Android.
 */
@ExperimentalNavStageApi
public class GestureNavStageTransition(private val onBack: (() -> Unit)? = null) :
  NavStageTransition {

  @OptIn(InternalCircuitApi::class, ExperimentalSharedTransitionApi::class)
  @Composable
  override fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
    navigator: Navigator,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  ) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember { mutableStateOf(Offset.Zero) }
    var isSwipeInProgress by remember { mutableStateOf(false) }
    var showPrevious by remember { mutableStateOf(false) }
    var completedGestures by remember { mutableIntStateOf(0) }

    val slots = remember { SlotHistory<T>() }
    val currentSlot = remember(targetState) { slots.slotFor(targetState) }
    val previousArgs = remember(targetState.args) { poppedArgsOf(targetState.args) }
    // Resolved through stateFor so the previous stack gets the stage it needs, not this one's.
    val previous = if (previousArgs != null) stateFor(previousArgs) else null
    val previousSlot =
      remember(previous, currentSlot) { previous?.let { slots.previewSlot(it, currentSlot) } }
    SideEffect { slots.current = currentSlot }

    val seekableTransitionState = remember { SeekableTransitionState(currentSlot) }

    suspend fun resetTo(slot: StageSlot<T>) {
      swipeProgress = 0f
      isSwipeInProgress = false
      seekableTransitionState.animateTo(slot)
      // Transient gesture state outlives the animation so the motion stays applied until it ends.
      showPrevious = false
      swipeOffset = Offset.Zero
      slots.seekedPreview = null
    }

    // Keyed on completed gestures too, so a commit that leaves the stack unchanged still settles.
    LaunchedEffect(currentSlot, completedGestures) { resetTo(currentSlot) }

    LaunchedEffect(previousSlot, currentSlot) {
      if (previousSlot != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
              isSwipeInProgress = true
              slots.seekedPreview = previousSlot
              try {
                seekableTransitionState.seekTo(fraction = abs(progress), targetState = previousSlot)
              } catch (_: CancellationException) {
                // If seekTo is interrupted we want to keep observing the swipeProgress
              }
            }
          }
      }
    }

    PredictiveBackEventHandler(
      isEnabled = previousSlot != null,
      onBackProgress = { progress, offset ->
        showPrevious = progress != 0f
        swipeProgress = progress
        swipeOffset = offset
      },
      onBackCancelled = { resetTo(currentSlot) },
      onBackCompleted = {
        swipeProgress = 0f
        completedGestures++
        if (onBack != null) {
          onBack()
        } else {
          navigator.pop()
        }
      },
    )

    // Within one stage layout only the panes whose item changes move, so the rest stay put.
    val from = seekableTransitionState.currentState.state
    val to = seekableTransitionState.targetState.state
    val unchangedItemKeys =
      remember(from, to) {
        if (from.stageKey != to.stageKey) {
          null
        } else {
          from.visibleItems
            .mapTo(HashSet()) { it.key }
            .apply {
              retainAll(to.visibleItems.mapTo(HashSet()) { it.key })
            }
        }
      }

    val transition =
      rememberTransition(seekableTransitionState, label = "GestureNavStageTransition")
    // Slots are keyed by id rather than state: a stage keeps its slot across in-stage navigation so
    // pane transitions animate it, and the preview keeps its slot when the gesture commits.
    transition.AnimatedContent(
      transitionSpec = {
        (EnterTransition.None togetherWith ExitTransition.None).apply {
          targetContentZIndex = this@AnimatedContent.targetState.zIndex
        }
      },
      contentKey = { it.id },
    ) { slot ->
      // An exiting slot can hold a state equal to the current one after a stage is left and
      // re-entered mid-transition. Both would compose its records, so only the current slot does.
      if (slot.id != currentSlot.id && slot.state == currentSlot.state) return@AnimatedContent
      // Gives the seek a duration to cover and the motion its progress, linear so it tracks the
      // finger while seeking.
      val exitProgress by
        this.transition.animateFloat(
          transitionSpec = { if (showPrevious) tween(easing = LinearEasing) else snap() },
          label = "GestureNavStageTransition exit",
        ) {
          if (it == EnterExitState.PostExit) 1f else 0f
        }
      val motion = remember {
        BackMotion(
          progress = { if (showPrevious) exitProgress else 0f },
          offset = { swipeOffset },
          fades = { !isSwipeInProgress },
        )
      }
      Box(
        Modifier.fillMaxSize()
          .then(if (unchangedItemKeys == null) Modifier.backMotion(motion) else Modifier)
      ) {
        val paneMotion =
          remember(unchangedItemKeys) { unchangedItemKeys?.let { PaneBackMotion(motion, it) } }
        CompositionLocalProvider(LocalPaneBackMotion provides paneMotion) {
          ProvideAnimatedTransitionScope(Navigation, this@AnimatedContent) {
            content(if (slot.id == currentSlot.id) currentSlot.state else slot.state)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalNavStageApi::class)
private data class StageSlot<T : NavArgument>(
  val state: NavStageTransitionState<T>,
  val id: Int,
  val zIndex: Float,
)

/**
 * Assigns slots to states. A state keeps the slot of the applied [current] when the stage is
 * unchanged, or takes over the [seekedPreview] when a gesture commits to it.
 */
@OptIn(ExperimentalNavStageApi::class)
private class SlotHistory<T : NavArgument> {
  private var nextId = 0
  var current: StageSlot<T>? = null
  var seekedPreview: StageSlot<T>? = null

  fun slotFor(state: NavStageTransitionState<T>): StageSlot<T> {
    val preview = seekedPreview
    val current = current
    return when {
      preview != null && preview.state == state -> preview.copy(state = state)
      current != null && current.state.stageKey == state.stageKey -> current.copy(state = state)
      else -> StageSlot(state, nextId++, current?.zIndex ?: 0f)
    }
  }

  fun previewSlot(state: NavStageTransitionState<T>, current: StageSlot<T>): StageSlot<T> =
    StageSlot(state, nextId++, current.zIndex - 1f)
}

/** The stack after popping the active item, or null if there is nothing behind it. */
private fun <T : NavArgument> poppedArgsOf(args: NavStackList<T>): NavStackList<T>? {
  val backward = args.backwardItems.toList()
  if (backward.isEmpty()) return null
  return navStackListOf(activeItem = backward.first(), backwardItems = backward.drop(1))
}

/** Predictive back motion for content leaving during a gesture or its commit. */
internal class BackMotion(
  val progress: () -> Float,
  val offset: () -> Offset,
  val fades: () -> Boolean,
)

/** [motion] for panes within a stage layout, except those showing [unchangedItemKeys]. */
internal class PaneBackMotion(val motion: BackMotion, val unchangedItemKeys: Set<Any>)

internal val LocalPaneBackMotion = compositionLocalOf<PaneBackMotion?> { null }

internal fun Modifier.backMotion(motion: BackMotion): Modifier = graphicsLayer {
  val progress = motion.progress()
  if (progress == 0f) return@graphicsLayer

  val scale = 1f - (progress * 0.1f)
  scaleX = scale
  scaleY = scale

  val maxTranslationX = progress * (size.width / 20)
  val maxTranslationY = progress * (size.height / 20)
  val offset = motion.offset()
  translationX = offset.x.coerceIn(-maxTranslationX, maxTranslationX)
  translationY = offset.y.coerceIn(-maxTranslationY, maxTranslationY)

  if (motion.fades()) {
    alpha = 1f - progress
  }
}
