package com.slack.circuit.foundation.scene

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.animation.PartialContentTransform
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

@Immutable
public interface AnimatedScene : AnimatedNavState {

  /** The [NavArgument] that are visible in this [AnimatedScene]. */
  public val visible: List<NavArgument>

  /**
   * @param animatedNavEvent The event that triggered the transition.
   * @param target The [AnimatedScene] this [AnimatedScene] is transition to
   * @param overlaps Whether the scenes are overlapping and have common [NavArgument] in
   *   [AnimatedScene.visible]
   */
  public fun AnimatedContentTransitionScope<AnimatedScene>.transition(
    animatedNavEvent: AnimatedNavEvent,
    target: AnimatedScene,
    overlaps: Boolean,
  ): PartialContentTransform = PartialContentTransform.EMPTY

  /**  */
  @Composable public fun AnimatedSceneDecoratorScope.Content(modifier: Modifier)
}

public data class DefaultAnimatedScene(override val navStack: NavStackList<out NavArgument>) :
  AnimatedScene {

  override val visible: List<NavArgument> = listOf(navStack.active)

  @Composable
  override fun AnimatedSceneDecoratorScope.Content(modifier: Modifier) {
    NavItem(arg = navStack.active, modifier = modifier)
  }
}
