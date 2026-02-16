package com.slack.circuit.foundation.scene

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.NavigatorDefaults.backward
import com.slack.circuit.foundation.NavigatorDefaults.forward
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument

@Stable
public interface AnimatedSceneDecorator { // Layout

  /** Builds the default [AnimatedContent] transition spec. */
  public fun AnimatedContentTransitionScope<AnimatedScene>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform

  @Composable
  public fun AnimatedSceneDecoratorScope.DecorateSceneContent(
    scene: AnimatedScene,
    modifier: Modifier = Modifier,
  )
}

@Stable
public sealed interface AnimatedSceneDecoratorScope :
  AnimatedVisibilityScope, SharedTransitionScope {

  /** The [AnimatedSceneTransitionDriver] used to drive the transition. */
  public val transitionDriver: AnimatedSceneTransitionDriver

  /**
   * Compose the specific [NavArgument] in the [AnimatedScene.Content].
   *
   * @param arg The [NavArgument] to render.
   * @param modifier The [Modifier] to apply to the content.
   * @param enableSharedElementTransition Whether to enable shared element transitions when this
   *   item is matched in another [AnimatedScene].
   * @param enterTransition The [EnterTransition] to use with [enableSharedElementTransition]
   * @param exitTransition The [ExitTransition] to use with [enableSharedElementTransition]
   */
  @Composable
  public fun <T : NavArgument> NavItem(
    arg: T,
    modifier: Modifier,
    enableSharedElementTransition: Boolean = true,
    enterTransition: EnterTransition = EnterTransition.None,
    exitTransition: ExitTransition = ExitTransition.None,
  )
}

@OptIn(InternalCircuitApi::class)
public class DefaultAnimatedSceneDecorator : AnimatedSceneDecorator {

  override fun AnimatedContentTransitionScope<AnimatedScene>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return when (animatedNavEvent) {
      AnimatedNavEvent.Forward,
      AnimatedNavEvent.GoTo -> forward

      AnimatedNavEvent.Backward,
      AnimatedNavEvent.Pop -> backward

      AnimatedNavEvent.RootReset -> fadeIn() togetherWith fadeOut()
    }.using(
      // Disable clipping since the faded slide-in/out should
      // be displayed out of bounds.
      SizeTransform(clip = false)
    )
  }

  @Composable
  override fun AnimatedSceneDecoratorScope.DecorateSceneContent(
    scene: AnimatedScene,
    modifier: Modifier,
  ) {
    with(scene) { Content(modifier) }
  }
}
