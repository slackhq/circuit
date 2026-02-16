package com.slack.circuit.foundation.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

@Stable
public interface AnimatedSceneTransitionDriver {

  @Composable
  public fun <T : NavArgument, S : AnimatedScene> AnimatedSceneTransitionScope<S>.AnimateTransition(
    args: NavStackList<out T>,
    targetScene: (NavStackList<out T>) -> S,
  )
}

public class DefaultAnimatedSceneTransitionDriver : AnimatedSceneTransitionDriver {

  @Composable
  override fun <T : NavArgument, S : AnimatedScene> AnimatedSceneTransitionScope<S>
    .AnimateTransition(args: NavStackList<out T>, targetScene: (NavStackList<out T>) -> S) {
    val scene = remember(args) { targetScene(args) }
    LaunchedEffect(scene) { animateTo(scene) }
  }
}
