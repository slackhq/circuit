package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.NavigatorDefaults.DefaultDecoration
import com.slack.circuit.runtime.InternalCircuitApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Stable
public class AnimatedNavDecoration(
  private val interceptors: ImmutableList<AnimatedNavDecorationInterceptor> =
    persistentListOf(DefaultAnimatedNavDecorationInterceptor)
) : NavDecoration {

  public constructor(
    vararg interceptors: AnimatedNavDecorationInterceptor
  ) : this(interceptors.toList().toImmutableList())

  @Composable
  override fun <T> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val currentArgs = remember(args, backStackDepth) { AnimatedNavHolder(args, backStackDepth) }
    var previousArgs by remember { mutableStateOf<AnimatedNavHolder<T>?>(null) }
    SideEffect { previousArgs = currentArgs }

    val decorationState =
      remember(interceptors) {
        interceptors
          .fold(AnimatedNavDecorationState.Builder<T>()) { builder, interceptor ->
            with(interceptor) { builder.buildUpon(previousArgs, currentArgs) }
            builder
          }
          .build()
      }

    val transition = updateTransition(targetState = currentArgs, label = "AnimatedNavDecoration")
    Box(modifier = modifier) {
      TransitionContentDecorate(decorationState.transitionContents, transition) {
        transition.AnimatedContent(
          modifier = Modifier,
          transitionSpec = decorationState.contentTransform,
        ) { state ->
          AnimatedContentDecorate(decorationState.decoratedContents, state, this) { navHolder, _ ->
            content(navHolder.args.first())
          }
        }
      }
    }
  }
}

@Composable
public fun <T> TransitionContentDecorate(
  decoratedContents: ImmutableList<TransitionContent<T>>,
  transition: Transition<AnimatedNavHolder<T>>,
  base: @Composable () -> Unit,
) {
  TransitionContentDecorate(0, decoratedContents, transition, base)
}

@Composable
private fun <T> TransitionContentDecorate(
  i: Int,
  transitionContents: ImmutableList<TransitionContent<T>>,
  transition: Transition<AnimatedNavHolder<T>>,
  base: @Composable () -> Unit,
) {
  if (i == transitionContents.lastIndex) {
    transitionContents[i].Content(transition, base)
  } else {
    transitionContents[i].Content(transition) {
      TransitionContentDecorate(i + 1, transitionContents, transition, base)
    }
  }
}

public fun interface TransitionContent<T> {
  @Composable
  public fun Content(transition: Transition<AnimatedNavHolder<T>>, content: @Composable () -> Unit)
}

@Composable
public fun <T> AnimatedContentDecorate(
  decoratedContents: ImmutableList<NestedDecoratedContent<T>>,
  holder: AnimatedNavHolder<T>,
  animatedContentScope: AnimatedContentScope,
  base: DecoratedContent<T>,
) {
  AnimatedContentDecorate(0, decoratedContents, holder, animatedContentScope, base)
}

@Composable
private fun <T> AnimatedContentDecorate(
  i: Int,
  decoratedContents: ImmutableList<NestedDecoratedContent<T>>,
  holder: AnimatedNavHolder<T>,
  animatedContentScope: AnimatedContentScope,
  baseContent: DecoratedContent<T>,
) {
  if (i == decoratedContents.lastIndex) {
    decoratedContents[i].Content(holder, animatedContentScope) { navHolder, scope ->
      baseContent.Content(navHolder, scope)
    }
  } else {
    decoratedContents[i].Content(holder, animatedContentScope) { navHolder, scope ->
      AnimatedContentDecorate(i + 1, decoratedContents, navHolder, scope, baseContent)
    }
  }
}

public fun interface DecoratedContent<T> {

  @Composable
  public fun Content(holder: AnimatedNavHolder<T>, animatedContentScope: AnimatedContentScope)
}

public fun interface NestedDecoratedContent<T> {

  @Composable
  public fun Content(
    holder: AnimatedNavHolder<T>,
    animatedContentScope: AnimatedContentScope,
    content: DecoratedContent<T>,
  )
}

public class AnimatedNavDecorationState<T> private constructor(builder: Builder<T>) {

  public val contentTransform:
    AnimatedContentTransitionScope<AnimatedNavHolder<T>>.() -> ContentTransform =
    builder.contentTransform
  public val transitionContents: ImmutableList<TransitionContent<T>> =
    builder.transitionContents.toImmutableList()
  public val decoratedContents: ImmutableList<NestedDecoratedContent<T>> =
    builder.decoratedContents.toImmutableList()

  public fun builder(): Builder<T> = Builder(this)

  public class Builder<T>() {
    public var contentTransform:
      AnimatedContentTransitionScope<AnimatedNavHolder<T>>.() -> ContentTransform =
      {
        EnterTransition.None togetherWith ExitTransition.None
      }
    public val transitionContents: MutableList<TransitionContent<T>> = mutableListOf()
    public val decoratedContents: MutableList<NestedDecoratedContent<T>> = mutableListOf()

    internal constructor(state: AnimatedNavDecorationState<T>) : this() {
      contentTransform = state.contentTransform

      transitionContents.addAll(state.transitionContents)
      decoratedContents.addAll(state.decoratedContents)
    }

    public fun build(): AnimatedNavDecorationState<T> = AnimatedNavDecorationState(this)
  }
}

public interface AnimatedNavDecorationInterceptor {

  public fun <T> AnimatedNavDecorationState.Builder<T>.buildUpon(
    previousHolder: AnimatedNavHolder<T>?,
    currentHolder: AnimatedNavHolder<T>,
  )
}

@Immutable
public data class AnimatedNavHolder<T>(
  public val args: ImmutableList<T>,
  public val backStackDepth: Int,
)

public object DefaultAnimatedNavDecorationInterceptor : AnimatedNavDecorationInterceptor {
  @OptIn(InternalCircuitApi::class)
  override fun <T> AnimatedNavDecorationState.Builder<T>.buildUpon(
    previousHolder: AnimatedNavHolder<T>?,
    currentHolder: AnimatedNavHolder<T>,
  ) {
    contentTransform = {
      val diff = targetState.args.size - initialState.args.size
      val sameRoot = targetState.args.lastOrNull() == initialState.args.lastOrNull()
      when {
        sameRoot && diff > 0 -> DefaultDecoration.forward
        sameRoot && diff < 0 -> DefaultDecoration.backward
        else -> fadeIn() togetherWith fadeOut()
      }.using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(clip = false)
      )
    }
  }
}
