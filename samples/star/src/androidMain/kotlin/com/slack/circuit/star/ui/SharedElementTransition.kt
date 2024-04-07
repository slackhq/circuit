package com.slack.circuit.star.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.NavigatorDefaults.DefaultDecoration.backward
import com.slack.circuit.foundation.NavigatorDefaults.DefaultDecoration.forward
import com.slack.circuit.overlay.AnimatedOverlay
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.LocalOverlayState
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayState
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.InternalCircuitApi
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementTransitionScope(content: @Composable SharedTransitionScope.() -> Unit) {
  LocalSharedTransitionScope.current.content()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementTransitionLayout(
  modifier: Modifier = Modifier,
  content: @Composable SharedTransitionScope.() -> Unit,
) {
  SharedTransitionLayout(modifier = modifier) {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) { content() }
  }
}

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalNavAnimatedContentScope: ProvidableCompositionLocal<AnimatedContentScope> =
  compositionLocalOf<AnimatedContentScope> { error("Not in a LocalAnimatedContentScope") }

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalOverlayAnimatedContentScope: ProvidableCompositionLocal<AnimatedContentScope?> =
  compositionLocalOf<AnimatedContentScope?> { null }

@SuppressLint("ComposeCompositionLocalUsage")
@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSharedTransitionScope =
  compositionLocalOf<SharedTransitionScope> { error("Not in a SharedElementTransitionLayout") }

object SharedElementNavDecoration : NavDecoration {
  @Composable
  override fun <T> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    @OptIn(InternalCircuitApi::class)
    AnimatedContent(
      targetState = args,
      modifier = modifier,
      transitionSpec = {
        // A transitionSpec should only use values passed into the `AnimatedContent`, to
        // minimize
        // the transitionSpec recomposing. The states are available as `targetState` and
        // `initialState`
        val diff = targetState.size - initialState.size
        val sameRoot = targetState.lastOrNull() == initialState.lastOrNull()

        when {
          sameRoot && diff > 0 -> forward
          sameRoot && diff < 0 -> backward
          else -> fadeIn() togetherWith fadeOut()
        }.using(
          // Disable clipping since the faded slide-in/out should
          // be displayed out of bounds.
          SizeTransform(clip = false)
        )
      },
      label = "SharedElementNavDecoration",
    ) {
      CompositionLocalProvider(LocalNavAnimatedContentScope provides this) { content(it.first()) }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.sharedElementAnimatedContentScope(): AnimatedContentScope {
  return LocalOverlayAnimatedContentScope.current ?: LocalNavAnimatedContentScope.current
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun SharedElementContentWithOverlays(
  modifier: Modifier = Modifier,
  overlayHost: OverlayHost = rememberOverlayHost(),
  content: @Composable () -> Unit,
) {
  val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
  val overlayState by remember {
    derivedStateOf { overlayHostData?.let { OverlayState.SHOWING } ?: OverlayState.HIDDEN }
  }
  CompositionLocalProvider(
    LocalOverlayHost provides overlayHost,
    LocalOverlayState provides overlayState,
  ) {
    SharedElementTransitionLayout(modifier) {
      content()
      AnimatedContent(
        targetState = overlayHostData,
        transitionSpec = {
          val enter =
            (targetState?.overlay as? AnimatedOverlay)?.enterTransition ?: EnterTransition.None
          val exit =
            (initialState?.overlay as? AnimatedOverlay)?.exitTransition ?: ExitTransition.None
          val sizeTransform = if (targetState != null) SizeTransform { _, _ -> snap(0) } else null
          (enter togetherWith exit).using(sizeTransform).also {
            it.targetContentZIndex = targetState?.let { 1f } ?: -1f
          }
        },
        contentAlignment = Alignment.Center,
      ) { data ->
        CompositionLocalProvider(LocalOverlayAnimatedContentScope provides this) {
          when (val overlay = data?.overlay) {
            null -> Unit
            is AnimatedOverlay -> with(overlay) { AnimatedContent(data::finish) }
            else -> overlay.Content(data::finish)
          }
        }
      }
    }
  }
}
