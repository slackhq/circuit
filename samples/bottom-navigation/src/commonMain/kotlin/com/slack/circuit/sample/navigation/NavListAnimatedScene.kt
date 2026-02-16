package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.AnimatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.PartialContentTransform
import com.slack.circuit.foundation.animation.asPartialContentTransform
import com.slack.circuit.foundation.scene.AnimatedScene
import com.slack.circuit.foundation.scene.AnimatedSceneDecoratorScope
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

data class NavListAnimatedScene(override val navStack: NavStackList<out NavArgument>) :
  AnimatedScene {

  val forward = navStack.forwardItems.firstOrNull()
  val current = navStack.active
  val backward = navStack.backwardItems.firstOrNull()

  override val visible: List<NavArgument>
    get() = buildList {
      forward?.let { add(it) }
      add(current)
      backward?.let { add(it) }
    }

  @OptIn(InternalCircuitApi::class)
  override fun AnimatedContentTransitionScope<AnimatedScene>.transition(
    animatedNavEvent: AnimatedNavEvent,
    target: AnimatedScene,
    overlaps: Boolean,
  ): PartialContentTransform {
    if (overlaps)
      return when (animatedNavEvent) {
        AnimatedNavEvent.Forward,
        AnimatedNavEvent.GoTo -> NavigatorDefaults.backward

        AnimatedNavEvent.Backward,
        AnimatedNavEvent.Pop -> NavigatorDefaults.forward

        AnimatedNavEvent.RootReset -> fadeIn() togetherWith fadeOut()
      }.asPartialContentTransform()
    // Use the default decorator transition
    return PartialContentTransform.EMPTY
  }

  @Composable
  override fun AnimatedSceneDecoratorScope.Content(modifier: Modifier) {
    Row(modifier = modifier) {
      BoxWithConstraints(modifier = Modifier.weight(2f)) {
        if (forward != null) {

          NavItem(
            arg = forward,
            enableSharedElementTransition = false,
            modifier =
              Modifier.sharedBounds(
                sharedContentState =
                  rememberSharedContentState("circuit_animated_scene_scope_${forward.key}"),
                animatedVisibilityScope = this@Content,
                resizeMode = RemeasureToBounds,
                placeholderSize = AnimatedSize,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                zIndexInOverlay = 1f,
              ),
          )
        }
        val sharedActive =
          Modifier.sharedBounds(
            sharedContentState =
              rememberSharedContentState("circuit_animated_scene_scope_${current.key}"),
            animatedVisibilityScope = this@Content,
            resizeMode = RemeasureToBounds,
            placeholderSize = AnimatedSize,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            zIndexInOverlay = 3f,
          )

        val activeModifier =
          if (forward != null) {
            sharedActive
              .offset(x = 32.dp)
              .padding(8.dp)
              .width(maxWidth - 48.dp)
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
          } else {
            sharedActive.width(maxWidth)
          }
        NavItem(arg = current, enableSharedElementTransition = false, modifier = activeModifier)
      }
      if (backward != null) {
        NavItem(
          arg = backward,
          modifier =
            Modifier.sharedBounds(
                sharedContentState =
                  rememberSharedContentState("circuit_animated_scene_scope_${backward.key}"),
                animatedVisibilityScope = this@Content,
                resizeMode = RemeasureToBounds,
                placeholderSize = AnimatedSize,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                zIndexInOverlay = 3f,
              )
              .weight(1f),
        )
      }
    }
  }
}
