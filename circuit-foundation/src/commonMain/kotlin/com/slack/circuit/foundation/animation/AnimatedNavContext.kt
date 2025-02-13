package com.slack.circuit.foundation.animation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.navigation.InternalCircuitNavigationApi
import com.slack.circuit.runtime.navigation.NavigationContext

public fun NavigationContext.transition(
  block: AnimatedNavContext.Builder.() -> Unit
): NavigationContext {
  val builder = tag<AnimatedNavContext>()?.buildUpon() ?: AnimatedNavContext.Builder()
  putTag(builder.apply(block).build())
  return this
}

@OptIn(InternalCircuitNavigationApi::class)
public fun transition(block: AnimatedNavContext.Builder.() -> Unit): NavigationContext {
  return NavigationContext().apply { putTag(AnimatedNavContext.Builder().apply(block).build()) }
}

@Immutable
public class AnimatedNavContext
internal constructor(
  public val enterTransition: EnterTransition? = null,
  public val exitTransition: ExitTransition? = null,
  public val transform: ContentTransform? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as AnimatedNavContext

    if (enterTransition != other.enterTransition) return false
    if (exitTransition != other.exitTransition) return false
    if (transform != other.transform) return false

    return true
  }

  override fun hashCode(): Int {
    var result = enterTransition?.hashCode() ?: 0
    result = 31 * result + (exitTransition?.hashCode() ?: 0)
    result = 31 * result + (transform?.hashCode() ?: 0)
    return result
  }

  public fun buildUpon(): Builder {
    return Builder().apply {
      enterTransition(enterTransition)
      exitTransition(exitTransition)
      transform(transform)
    }
  }

  @Stable
  public class Builder {

    private var enterTransition: EnterTransition? = null
    private var exitTransition: ExitTransition? = null
    private var contentTransform: ContentTransform? = null

    public fun enterTransition(enter: EnterTransition?): Builder {
      enterTransition = enter
      return this
    }

    public fun exitTransition(exit: ExitTransition?): Builder {
      exitTransition = exit
      return this
    }

    public fun transform(transform: ContentTransform?): Builder {
      contentTransform = transform
      return this
    }

    public fun build(): AnimatedNavContext {
      return AnimatedNavContext(enterTransition, exitTransition, contentTransform)
    }
  }
}
