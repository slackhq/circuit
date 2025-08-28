// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowWidthSizeClass
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

interface PrimaryScreen : Screen

interface SecondaryScreen : Screen

// todo Generalize this
@OptIn(ExperimentalCircuitApi::class)
class AdaptiveNavDecoration(
  screenTransforms: ImmutableMap<KClass<out Screen>, AnimatedScreenTransform>,
  onBackInvoked: () -> Unit,
) : NavDecoration {

  // Use GestureNavigationDecorator for the stacked case, normal cross-fade for multipane
  private val delegate =
    AnimatedNavDecoration(
      animatedScreenTransforms = screenTransforms,
      decoratorFactory = CrossFadeNavDecoratorFactory(),
    )

  private val delegateStacked =
    AnimatedNavDecoration(
      animatedScreenTransforms = screenTransforms,
      decoratorFactory =
        GestureNavigationDecorationFactory(
          fallback = NavigatorDefaults.DefaultDecoratorFactory,
          onBackInvoked = onBackInvoked,
        ),
    )

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: ImmutableList<T>,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    // Decorate the content layout based on the window size.
    // - Wide enough show as two pane
    // - Otherwise stack normally
    val windowInfo = currentWindowAdaptiveInfo()
    val layoutSideBySide =
      when (windowInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> false
        WindowWidthSizeClass.MEDIUM -> true
        WindowWidthSizeClass.EXPANDED -> true
        else -> false
      }
    if (layoutSideBySide) {
      Row(modifier = modifier) {
        // todo Make this smarter
        val (primary, stack) =
          remember(args) {
            val index = args.indexOfFirst { it.screen is PrimaryScreen }
            val hasSecondaryStack =
              index > 0 && args.subList(0, index).all { it.screen is SecondaryScreen }
            println("adaptive: $index $args")
            if (hasSecondaryStack) {
              args[index] to args.subList(0, index)
            } else {
              args.first() to null
            }
          }

        // Always show the primary screen
        Box(modifier = Modifier.weight(1f)) { content(primary) }
        // Show around the hinge
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxSize()
              .background(
                if (stack == null) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
              )
        ) {
          if (stack != null) {
            // Stack everything that is not the root screen
            delegate.DecoratedContent(stack, Modifier, content)
          }
          // todo Show a primary screen empty state here
          // else if (rootScreen is PrimaryScreen) {}
        }
      }
    } else {
      delegateStacked.DecoratedContent(args, modifier, content)
    }
  }
}
