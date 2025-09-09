// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.Color
import androidx.window.core.layout.WindowWidthSizeClass
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException

interface PrimaryScreen : Screen

interface SecondaryScreen : Screen

// todo Generalize this
//  - Allow for styling etc
@OptIn(ExperimentalCircuitApi::class)
class AdaptiveNavDecoration(
  screenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
  private val decoratorFactory: AnimatedNavDecorator.Factory,
  private val backgroundColor: @Composable () -> Color = { Color.Unspecified },
  private val onPop: () -> Unit = {},
) : NavDecoration {

  private val delegate =
    AnimatedNavDecoration(
      animatedScreenTransforms = screenTransforms,
      decoratorFactory = decoratorFactory,
    )

  private val sideBySideDelegate =
    AnimatedNavDecoration(
      animatedScreenTransforms = emptyMap(),
      decoratorFactory = decoratorFactory,
    )

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: List<T>,
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
      SideBySideContent(args, windowInfo, modifier, content)
    } else {
      delegate.DecoratedContent(args, modifier, content)
    }
  }

  @OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalComposeUiApi::class,
  )
  @Composable
  private fun <T : NavArgument> SideBySideContent(
    args: List<T>,
    windowInfo: WindowAdaptiveInfo,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val (primaryArgs, secondaryLookup) = rememberSideBySideNavArguments(args)
    val topPrimary by rememberUpdatedState(primaryArgs.first())
    val secondaryTransition =
      updateTransition(secondaryLookup[topPrimary]?.takeUnless { it.isEmpty() })
    sideBySideDelegate.DecoratedContent(primaryArgs, modifier) { primary ->
      val secondary =
        if (primary == topPrimary) {
          secondaryTransition.currentState ?: secondaryTransition.targetState
        } else {
          null
        }
      val hasSecondary = !secondary.isNullOrEmpty()
      val scaffoldValue = if (hasSecondary) PrimarySecondary else Primary
      val scaffoldState = remember { MutableThreePaneScaffoldState(scaffoldValue) }
      val directive = remember(windowInfo) { calculatePaneScaffoldDirective(windowInfo) }

      val paneExpansionState = rememberPaneExpansionState(key = scaffoldValue.paneExpansionStateKey)
      LaunchedEffect(scaffoldValue) { scaffoldState.animateTo(scaffoldValue) }
      ListDetailPaneScaffold(
        modifier = Modifier.fillMaxSize().background(backgroundColor()),
        directive = directive,
        scaffoldState = scaffoldState,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = {
          //          VerticalDragHandle()
        },
        listPane = { AnimatedPane(modifier = Modifier) { content(primary) } },
        detailPane = {
          AnimatedPane {
            // Stack everything that is not the primary screen
            if (hasSecondary) {
              delegate.DecoratedContent(secondary, Modifier, content)
            }
          }
        },
      )

      // Prevent the decorator from handling the back press
      PredictiveBackHandler(enabled = secondary?.size == 1) { progress ->
        try {
          progress.collect { backEvent ->
            scaffoldState.seekTo(backEvent.progress, Primary, isPredictiveBackInProgress = true)
          }
          onPop()
        } catch (_: CancellationException) {
          scaffoldState.snapTo(PrimarySecondary)
        }
      }
    }
  }
}

@Composable
private fun <T : NavArgument> rememberSideBySideNavArguments(
  args: List<T>
): Pair<List<T>, Map<T, List<T>>> =
  remember(args) {
    val primary = mutableListOf<T>()
    val secondaryLookup = mutableMapOf<T, List<T>>()
    val secondary = mutableListOf<T>()
    for (arg in args) {
      val screen = arg.screen
      when {
        screen is SecondaryScreen -> {
          secondary += arg
        }
        else -> {
          primary += arg
          secondaryLookup[arg] = secondary.toList()
          secondary.clear()
        }
      }
    }
    if (primary.isEmpty()) {
      secondary.toList() to emptyMap()
    } else {
      primary.toList() to secondaryLookup.toMap()
    }
  }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val PrimarySecondary =
  ThreePaneScaffoldValue(
    // List
    secondary = PaneAdaptedValue.Expanded,
    // Detail
    primary = PaneAdaptedValue.Expanded,
    tertiary = PaneAdaptedValue.Hidden,
  )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val Primary =
  ThreePaneScaffoldValue(
    // List
    secondary = PaneAdaptedValue.Expanded,
    // Detail
    primary = PaneAdaptedValue.Hidden,
    tertiary = PaneAdaptedValue.Hidden,
  )
