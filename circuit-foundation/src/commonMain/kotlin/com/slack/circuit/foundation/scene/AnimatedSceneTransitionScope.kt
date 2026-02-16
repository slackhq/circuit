package com.slack.circuit.foundation.scene

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.Stable

@Stable
public sealed interface AnimatedSceneTransitionScope<S : AnimatedScene> {

  public val transitionState: SeekableTransitionState<S>

  /**
   * Current state of the transition.
   *
   * @see [SeekableTransitionState.currentState]
   */
  public val currentState: S
  /**
   * Target state of the transition.
   *
   * @see [SeekableTransitionState.targetState]
   */
  public val targetState: S

  /**
   * Jumps the full transition to [targetState] without any animations.
   *
   * @see [SeekableTransitionState.snapTo]
   */
  public suspend fun snapTo(targetState: S)

  /**
   * Seeks the transition to [fraction] of the transition towards [targetState].
   *
   * @see [SeekableTransitionState.seekTo]
   */
  public suspend fun seekTo(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float,
    targetState: S = this.targetState,
  )

  /**
   * Animates the transition to [targetState] using the provided [animationSpec].
   *
   * @see [SeekableTransitionState.animateTo]
   */
  public suspend fun animateTo(
    targetState: S = this.targetState,
    animationSpec: FiniteAnimationSpec<Float>? = null,
  )
}

internal class StableAnimatedSceneTransitionScope<S : AnimatedScene>(
  override val transitionState: SeekableTransitionState<S>
) : AnimatedSceneTransitionScope<S> {
  override val currentState: S
    get() = transitionState.currentState

  override val targetState: S
    get() = transitionState.targetState

  override suspend fun snapTo(targetState: S) {
    transitionState.snapTo(targetState)
  }

  override suspend fun seekTo(fraction: Float, targetState: S) {
    transitionState.seekTo(fraction, targetState)
  }

  override suspend fun animateTo(targetState: S, animationSpec: FiniteAnimationSpec<Float>?) {
    transitionState.animateTo(targetState, animationSpec)
  }
}
