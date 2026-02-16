// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.scene

import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.AnimatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntSize
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.contextualNavigationOverride
import com.slack.circuit.foundation.animation.determineAnimatedNavEvent
import com.slack.circuit.foundation.internal.mapToImmutableSet
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalCircuitApi::class,
  InternalCircuitApi::class,
)
public class AnimatedSceneDecoration(
  private val decorator: AnimatedSceneDecorator = DefaultAnimatedSceneDecorator(),
  private val sceneProvider: (NavStackList<out NavArgument>) -> AnimatedScene = {
    DefaultAnimatedScene(it)
  },
  private val transitionDriver: AnimatedSceneTransitionDriver =
    DefaultAnimatedSceneTransitionDriver(),
) : NavDecoration {

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    modifier: Modifier,
    content: @Composable ((T) -> Unit),
  ) {
    // Set up the transition
    val transitionState = remember { SeekableTransitionState(sceneProvider(args)) }
    val transitionScope = remember { StableAnimatedSceneTransitionScope(transitionState) }
    val transition = rememberTransition(transitionState, label = "DecoratedContent")
    val decoratorState = remember { AnimatedSceneDecoratorState<T>(transition) }

    // Drive the transition to the next state.
    with(transitionDriver) { transitionScope.AnimateTransition(args, sceneProvider) }
    decoratorState.UpdateVisibleState()

    SharedTransitionScope { sharedModifier ->
      with(decorator) {
        transition.AnimatedContent(
          transitionSpec = transitionSpec(),
          modifier = modifier.then(sharedModifier),
        ) { scene ->
          AnimatedSceneDecoratorScopeImpl(
              transitionDriver = transitionDriver,
              decoratorState = decoratorState,
              scopeScene = scene,
              sharedTransitionScope = this@SharedTransitionScope,
              animatedScope = this,
              content = content,
            )
            .DecorateSceneContent(scene, Modifier)
        }
      }
    }
  }
}

/** Constructs the transition specification used in [AnimatedNavDecoration]. */
@OptIn(ExperimentalCircuitApi::class, InternalCircuitApi::class)
@Composable
private fun AnimatedSceneDecorator.transitionSpec():
  AnimatedContentTransitionScope<AnimatedScene>.() -> ContentTransform = spec@{
  val animatedNavEvent =
    determineAnimatedNavEvent() ?: return@spec EnterTransition.None togetherWith ExitTransition.None

  // Scene specific overrides
  val sceneOverride =
    with(initialState) {
      val areScenesOverlapping =
        targetState.visible.intersect(initialState.visible.toSet()).isNotEmpty()
      transition(
        animatedNavEvent = animatedNavEvent,
        target = targetState,
        overlaps = areScenesOverlapping,
      )
    }
  contextualNavigationOverride(
    baseTransform = transitionSpec(animatedNavEvent),
    screenOverride = sceneOverride,
  )
}

@Stable
private class AnimatedSceneDecoratorState<N : NavArgument>(
  val sceneTransition: Transition<AnimatedScene>
) {

  val sizes = mutableScatterMapOf<String, IntSize>()
  val staying = mutableScatterSetOf<String>()
  val animating = mutableScatterSetOf<String>()

  val targetVisible
    get() = sceneTransition.targetState.visible.mapToImmutableSet { it.key }

  val currentVisible
    get() = sceneTransition.currentState.visible.mapToImmutableSet { it.key }

  val currentlyVisible =
    mutableStateMapOf<String, Unit>().apply { currentVisible.forEach { put(it, Unit) } }

  val fullSceneChange
    get() = staying.isEmpty()

  val sameState
    get() = targetVisible == currentVisible

  @Composable
  fun UpdateVisibleState() {
    Snapshot.withMutableSnapshot {
      staying.clear()
      for (key in currentVisible) {
        if (targetVisible.contains(key)) {
          staying.add(key)
        } else {
          currentlyVisible.remove(key)
        }
      }
      targetVisible.forEach { currentlyVisible[it] = Unit }
      animating.forEach { currentlyVisible[it] = Unit }

      sizes.removeIf { key, _ ->
        key !in currentlyVisible && key !in targetVisible && key !in animating
      }
    }
  }
}

@Stable
private data class AnimatedSceneDecoratorScopeImpl<NavArgType : NavArgument>(
  override val transitionDriver: AnimatedSceneTransitionDriver,
  val decoratorState: AnimatedSceneDecoratorState<NavArgType>,
  val sharedTransitionScope: SharedTransitionScope,
  val animatedScope: AnimatedVisibilityScope,
  val scopeScene: AnimatedScene,
  val content: @Composable (NavArgType) -> Unit,
) :
  AnimatedSceneDecoratorScope,
  AnimatedVisibilityScope by animatedScope,
  SharedTransitionScope by sharedTransitionScope {

  @OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTransitionApi::class)
  @Composable
  override fun <T : NavArgument> NavItem(
    arg: T,
    modifier: Modifier,
    enableSharedElementTransition: Boolean,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
  ) {
    ReusableContent(arg.key) {
      DisposableEffect(Unit) {
        decoratorState.animating.add(arg.key)
        onDispose { decoratorState.animating.remove(arg.key) }
      }
      val isCurrentScene = scopeScene == decoratorState.sceneTransition.currentState
      val isTargetScene = scopeScene == decoratorState.sceneTransition.targetState
      val isActiveScene = isCurrentScene || isTargetScene
      // The content is moving to the target, don't include in both to avoid the registry crash
      val isMovingFromCurrent =
        isTargetScene &&
          !decoratorState.sameState &&
          isCurrentVisible(arg.key) &&
          isTargetVisible(arg.key)
      // This content is animating outside the active scenes
      val isAnimatingElsewhere = !isActiveScene && isAnimating(arg.key)
      val showPlaceholder = isMovingFromCurrent || isAnimatingElsewhere
      // Shared elements
      val sharedModifier =
        if (enableSharedElementTransition) {
          val sharedContentState =
            rememberSharedContentState("circuit_animated_scene_scope_${arg.key}")
          Modifier.sharedBounds(
              sharedContentState = sharedContentState,
              animatedVisibilityScope = animatedScope,
              resizeMode = RemeasureToBounds,
              placeholderSize = AnimatedSize,
              enter = enterTransition,
              exit = exitTransition,
              renderInOverlayDuringTransition = !showPlaceholder,
            )
            .then(modifier)
        } else {
          modifier
        }
      if (showPlaceholder) {
        // Use an empty placeholder as the shared element target.
        Layout(
          modifier = sharedModifier.skipToLookaheadSize(),
          measurePolicy = { _, _ ->
            val itemSize = decoratorState.sizes[arg.key] ?: IntSize.Zero
            layout(itemSize.width, itemSize.height) {}
          },
        )
      } else {
        Box(modifier = sharedModifier.onPlaced { decoratorState.sizes[arg.key] = it.size }) {
          // T and NavArgType are the same
          @Suppress("UNCHECKED_CAST") content(arg as NavArgType)
        }
      }
    }
  }

  @Composable
  private fun isCurrentVisible(key: String) = decoratorState.currentVisible.contains(key)

  @Composable private fun isTargetVisible(key: String) = decoratorState.targetVisible.contains(key)

  @Composable private fun isAnimating(key: String) = decoratorState.animating.contains(key)
}
