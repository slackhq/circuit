// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.collection.mutableScatterSetOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
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
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.internal.mapToImmutableSet
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCircuitApi::class)
public class AnimatedSceneDecoration
constructor(
  private val decorator: AnimatedNavArgumentDecorator = DefaultAnimatedNavArgumentDecorator(),
  private val sceneProvider: (NavStackList<NavArgument>) -> AnimatedScene = {
    NavListAnimatedScene(it)
  },
  private val driver: AnimatedSceneTransitionDriver = DefaultAnimatedSceneTransitionDriver(),
) : NavDecoration {

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    modifier: Modifier,
    content: @Composable ((T) -> Unit),
  ) {
    @Suppress("UNCHECKED_CAST") val castArgs = args as NavStackList<NavArgument>
    val transitionState = remember { SeekableTransitionState(sceneProvider(castArgs)) }
    // Scope
    val transition =
      rememberTransition(
        transitionState,
        label = "SeekableTransitionState for ${decorator::class.simpleName}",
      )
    with(driver) { transitionState.updateTransition(castArgs, sceneProvider) }
    val innerContent by rememberUpdatedState(content)
    val state = remember { AnimatedNavArgumentDecoratorState<T>(transition) }
    SharedTransitionScope { sharedModifier ->
      // todo Get rid of this
      state.Check()
      with(decorator) {
        transition.AnimatedContent(
          transitionSpec = {
            val animatedNavEvent =
              determineAnimatedNavEvent()
                ?: return@AnimatedContent EnterTransition.None togetherWith ExitTransition.None
            this.targetState.transition(animatedNavEvent, this.initialState)
          },
          modifier = modifier.then(sharedModifier),
        ) { scene ->
          DefaultAnimatedSceneDecoratorScope(
              transitionDriver = driver,
              state = state,
              scopeScene = scene,
              sharedTransitionScope = this@SharedTransitionScope,
              animatedScope = this,
              innerContent = innerContent,
            )
            .DecorateSceneContent<T>(scene, Modifier)
        }
      }
    }
  }
}

public sealed interface AnimatedNavArgumentScope : AnimatedVisibilityScope {
  public companion object {
    public operator fun invoke(
      animatedVisibilityScope: AnimatedVisibilityScope
    ): AnimatedNavArgumentScope = Impl(animatedVisibilityScope)
  }

  private class Impl(animatedVisibilityScope: AnimatedVisibilityScope) :
    AnimatedNavArgumentScope, AnimatedVisibilityScope by animatedVisibilityScope
}

public interface AnimatedScene : AnimatedNavState {
  public val visible: List<NavArgument>

  public fun transition(animatedNavEvent: AnimatedNavEvent, other: AnimatedScene): ContentTransform

  @Composable public fun AnimatedSceneDecoratorScope.Content(modifier: Modifier)
}

// todo DSL
private data class NavListAnimatedScene(override val navStack: NavStackList<out NavArgument>) :
  AnimatedScene {

  val forward = navStack.forwardItems.firstOrNull()
  val current = navStack.active
  val backward = navStack.backwardItems.firstOrNull()

  override val visible: List<NavArgument>
    get() = buildList {
      add(current)
      forward?.let { add(it) }
      backward?.let { add(it) }
    }

  @OptIn(InternalCircuitApi::class)
  override fun transition(
    animatedNavEvent: AnimatedNavEvent,
    other: AnimatedScene,
  ): ContentTransform {
    val sharedTransition = visible.intersect(other.visible.toSet()).isNotEmpty()
    if (sharedTransition) {
      // This is the base for anything running that doesn't match
      return when (animatedNavEvent) {
        AnimatedNavEvent.Forward,
        AnimatedNavEvent.GoTo -> NavigatorDefaults.backward

        AnimatedNavEvent.Backward,
        AnimatedNavEvent.Pop -> NavigatorDefaults.forward

        AnimatedNavEvent.RootReset -> fadeIn() togetherWith fadeOut()
      }
    }
    return when (animatedNavEvent) {
      AnimatedNavEvent.Forward,
      AnimatedNavEvent.GoTo -> NavigatorDefaults.forward

      AnimatedNavEvent.Backward,
      AnimatedNavEvent.Pop -> NavigatorDefaults.backward

      AnimatedNavEvent.RootReset -> fadeIn() togetherWith fadeOut()
    }
  }

  @Composable
  override fun AnimatedSceneDecoratorScope.Content(modifier: Modifier) {
    Row(modifier = modifier) {
      if (forward != null) {
        Place(arg = forward, modifier = Modifier.weight(1f))
      }
      Place(
        arg = current,
        modifier = Modifier.weight(if (backward != null && forward != null) 1f else 2f),
      )
      if (backward != null) {
        Place(arg = backward, modifier = Modifier.weight(1f))
      }
    }
  }
}

public interface AnimatedSceneTransitionDriver {

  @Composable
  public fun <T : NavArgument, S> SeekableTransitionState<S>.updateTransition(
    args: NavStackList<T>,
    targetScene: (NavStackList<T>) -> S,
  )
}

public class DefaultAnimatedSceneTransitionDriver : AnimatedSceneTransitionDriver {

  @Composable
  override fun <T : NavArgument, S> SeekableTransitionState<S>.updateTransition(
    args: NavStackList<T>,
    targetScene: (NavStackList<T>) -> S,
  ) {
    val target = remember(args) { targetScene(args) }
    LaunchedEffect(target) { animateTo(target) }
  }
}

@Stable
public interface AnimatedNavArgumentDecorator { // Layout

  @Composable
  public fun <T : NavArgument> AnimatedSceneDecoratorScope.DecorateSceneContent(
    scene: AnimatedScene,
    modifier: Modifier = Modifier,
  )
}

private class DefaultAnimatedNavArgumentDecorator : AnimatedNavArgumentDecorator {

  @Composable
  override fun <T : NavArgument> AnimatedSceneDecoratorScope.DecorateSceneContent(
    scene: AnimatedScene,
    modifier: Modifier,
  ) {
    with(scene) { Content(modifier) }
  }
}

public sealed interface AnimatedSceneDecoratorScope {

  public val transitionDriver: AnimatedSceneTransitionDriver

  @Composable
  public fun <T : NavArgument> Place(
    arg: T,
    modifier: Modifier,
    enterTransition: EnterTransition = EnterTransition.None,
    exitTransition: ExitTransition = ExitTransition.None,
    boundsTransform: BoundsTransform = BoundsTransform { _, _ -> spring() },
  )
}

private class AnimatedNavArgumentDecoratorState<N : NavArgument>(
  val sceneTransition: Transition<AnimatedScene>
) {

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
  fun Check() {
    Snapshot.withMutableSnapshot {
      staying.clear()
      for (e in currentVisible) {
        if (targetVisible.contains(e)) {
          staying.add(e)
        } else {
          currentlyVisible.remove(e)
        }
      }
      targetVisible.forEach { currentlyVisible[it] = Unit }
      animating.forEach { currentlyVisible[it] = Unit }
    }
  }
}

@Stable
private data class DefaultAnimatedSceneDecoratorScope<N : NavArgument>(
  override val transitionDriver: AnimatedSceneTransitionDriver,
  val state: AnimatedNavArgumentDecoratorState<N>,
  val sharedTransitionScope: SharedTransitionScope,
  val animatedScope: AnimatedVisibilityScope,
  val scopeScene: AnimatedScene,
  val innerContent: @Composable (N) -> Unit,
) :
  AnimatedSceneDecoratorScope,
  AnimatedVisibilityScope by animatedScope,
  SharedTransitionScope by sharedTransitionScope {

  fun isTargetScene(scene: AnimatedScene) =
    scene == state.sceneTransition.targetState || state.sameState

  fun isTargetVisible(key: String) = state.targetVisible.contains(key)

  fun isCurrentVisible(key: String) = state.currentVisible.contains(key)

  @OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTransitionApi::class)
  @Composable
  override fun <T : NavArgument> Place(
    arg: T,
    modifier: Modifier,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    boundsTransform: BoundsTransform,
  ) {
    key(arg.key) {
      DisposableEffect(Unit) {
        state.animating.add(arg.key)
        onDispose { state.animating.remove(arg.key) }
      }
      val sharedContentState =
        rememberSharedContentState("___circuit_animated_scene_scope_${arg.key}___")
      val isCurrentScene = scopeScene == state.sceneTransition.currentState
      val isTargetScene = scopeScene == state.sceneTransition.targetState
      val isActiveScene = isTargetScene || isCurrentScene
      // The content is moving to the target, don't include right away in both to avoid the registry
      // crash
      val isSharedElsewhere =
        !state.sameState && isTargetScene && isCurrentVisible(arg.key) && isTargetVisible(arg.key)
      // This content is animating outside the active scene
      val isAnimatingElsewhere = !isActiveScene && state.animating.contains(arg.key)
      // Use a spacer as the shared element target
      if (isSharedElsewhere || isAnimatingElsewhere) {
        Spacer(
          modifier
            .then(
              Modifier.sharedBounds(
                  sharedContentState = sharedContentState,
                  animatedVisibilityScope = animatedScope,
                  resizeMode = RemeasureToBounds,
                  placeholderSize = AnimatedSize,
                  enter = EnterTransition.None,
                  exit = ExitTransition.None,
                  renderInOverlayDuringTransition = false,
                )
                .skipToLookaheadSize()
            )
            .fillMaxSize()
        )
      } else {
        Box(
          modifier.then(
            Modifier.sharedBounds(
              sharedContentState = sharedContentState,
              animatedVisibilityScope = animatedScope,
              resizeMode = RemeasureToBounds,
              placeholderSize = AnimatedSize,
              enter = EnterTransition.None,
              exit = ExitTransition.None,
              renderInOverlayDuringTransition = true,
              zIndexInOverlay = 10f,
            )
          )
        ) {
          @Suppress("UNCHECKED_CAST") innerContent(arg as N)
        }
      }
    }
  }
}
