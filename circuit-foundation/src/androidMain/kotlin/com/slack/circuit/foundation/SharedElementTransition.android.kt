package com.slack.circuit.foundation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.InternalCircuitApi

@OptIn(ExperimentalSharedTransitionApi::class, InternalCircuitApi::class)
@Composable
public actual fun SharedElementTransitionLayout(
  modifier: Modifier,
  content: @Composable () -> Unit,
) {
  SharedTransitionLayout(modifier = modifier) {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) { content() }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, InternalCircuitApi::class)
@SuppressLint("ComposeComposableModifier")
@Composable
public inline fun Modifier.thenIfSharedTransitionScope(
  @SuppressLint("ComposeModifierWithoutDefault")
  block: SharedTransitionScope.(AnimatedContentScope) -> Modifier
): Modifier {
  val transitionScope = LocalSharedTransitionScope.current
  return if (transitionScope != null) {
    val animatedContentScope = LocalTransitionAnimatedContentScope.current
    this then with(transitionScope) { block(animatedContentScope) }
  } else this
}

@SuppressLint("ComposeCompositionLocalUsage")
@InternalCircuitApi
public val LocalTransitionAnimatedContentScope: ProvidableCompositionLocal<AnimatedContentScope> =
  compositionLocalOf {
    error("Not in a SharedElementTransitionLayout")
  }

@SuppressLint("ComposeCompositionLocalUsage")
@OptIn(ExperimentalSharedTransitionApi::class)
@InternalCircuitApi
public val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?> =
  compositionLocalOf {
    null
  }

public object SharedElementTransitionNavDecorationInterceptor : AnimatedNavDecorationInterceptor {
  @OptIn(InternalCircuitApi::class)
  override fun <T> AnimatedNavDecorationState.Builder<T>.buildUpon(
    previousHolder: AnimatedNavHolder<T>?,
    currentHolder: AnimatedNavHolder<T>,
  ) {
    transitionContents.add(
      TransitionContent { _, content -> SharedElementTransitionLayout(Modifier) { content() } }
    )
    decoratedContents.add(
      NestedDecoratedContent { holder, animatedContentScope, content ->
        CompositionLocalProvider(
          LocalTransitionAnimatedContentScope provides animatedContentScope
        ) {
          content.Content(holder, animatedContentScope)
        }
      }
    )
  }
}
