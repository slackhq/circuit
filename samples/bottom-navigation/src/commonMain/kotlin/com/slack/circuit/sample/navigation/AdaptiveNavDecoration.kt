// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.slack.circuit.foundation.DelicateCircuitFoundationApi
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException

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

@OptIn(ExperimentalCircuitApi::class, ExperimentalSharedTransitionApi::class)
class AdaptiveNavDecoration(
  screenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
  normalDecoratorFactory: AnimatedNavDecorator.Factory,
  detailPaneDecoratorFactory: AnimatedNavDecorator.Factory,
  private val isDetailPane: (NavArgument) -> Boolean,
  private val shouldUsePaneLayout: (WindowAdaptiveInfo) -> Boolean = ::layoutSideBySide,
) : NavDecoration {

  private val delegate =
    AnimatedNavDecoration(
      animatedScreenTransforms = screenTransforms,
      decoratorFactory = normalDecoratorFactory,
    )

  private val detailPaneDelegate =
    AnimatedNavDecoration(
      animatedScreenTransforms = emptyMap(),
      decoratorFactory = detailPaneDecoratorFactory,
    )

  @OptIn(DelicateCircuitFoundationApi::class)
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: List<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    // Decorate the content layout based on the window size.
    // - Wide enough show as two pane
    // - Otherwise stack normally
    val windowInfo = currentWindowAdaptiveInfo()
    if (shouldUsePaneLayout(windowInfo)) {
      PaneContent(
        args = args,
        windowInfo = windowInfo,
        navigator = navigator,
        modifier = modifier,
        content = content,
      )
    } else {
      delegate.DecoratedContent(
        args = args,
        navigator = navigator,
        modifier = modifier,
        content = content,
      )
    }
  }

  @OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalComposeUiApi::class,
  )
  @Composable
  private fun <T : NavArgument> PaneContent(
    args: List<T>,
    windowInfo: WindowAdaptiveInfo,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val directive = remember(windowInfo) { calculatePaneScaffoldDirective(windowInfo) }
    val (primaryArgs, secondaryLookup) = rememberListDetailNavArguments(args, isDetailPane)
    val secondaryLookupTransition = updateTransition(secondaryLookup)
    delegate.DecoratedContent(primaryArgs, navigator, modifier) { primary ->
      val secondaryArgs =
        with(secondaryLookupTransition) { currentState[primary] ?: targetState[primary] }

      val hasSecondary = !secondaryArgs.isNullOrEmpty()
      val singleSecondary = hasSecondary && secondaryArgs.size == 1
      val scaffoldValue = if (hasSecondary) PrimarySecondary else Primary
      val scaffoldState = remember { MutableThreePaneScaffoldState(scaffoldValue) }

      // todo Anchors vs Resizeable with minimums
      val minPaneSize = 240.dp
      val paneExpansionState =
        rememberPaneExpansionState(
          key = scaffoldValue.paneExpansionStateKey,
          anchors =
            listOf(
              PaneExpansionAnchor.Offset.fromStart(minPaneSize),
              PaneExpansionAnchor.Offset.fromStart(directive.defaultPanePreferredWidth),
              PaneExpansionAnchor.Offset.fromEnd(directive.defaultPanePreferredWidth),
              PaneExpansionAnchor.Offset.fromEnd(minPaneSize),
            ),
        )
      LaunchedEffect(scaffoldValue) { scaffoldState.animateTo(scaffoldValue) }
      ListDetailPaneScaffold(
        modifier = Modifier.fillMaxSize(),
        directive = directive,
        scaffoldState = scaffoldState,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = { state ->
          val interactionSource = remember { MutableInteractionSource() }
          VerticalDragHandle(
            modifier =
              Modifier.paneExpansionDraggable(
                state = state,
                minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                interactionSource = interactionSource,
                semanticsProperties = state.defaultDragHandleSemantics(),
              ),
            interactionSource = interactionSource,
          )
        },
        listPane = { AnimatedPane(modifier = Modifier) { content(primary) } },
        detailPane = {
          AnimatedPane {
            if (hasSecondary) {
              // Stack multiple with a normal decoration
              detailPaneDelegate.DecoratedContent(secondaryArgs, navigator, Modifier, content)
            }
          }
        },
      )

      // Prevent the detailPaneDelegate from handling the back press
      PredictiveBackHandler(enabled = singleSecondary) { progress ->
        try {
          progress.collect { backEvent ->
            scaffoldState.seekTo(backEvent.progress, Primary, isPredictiveBackInProgress = true)
          }
          navigator.pop()
        } catch (_: CancellationException) {
          scaffoldState.snapTo(PrimarySecondary)
        }
      }
    }
  }
}

private fun layoutSideBySide(adaptiveInfo: WindowAdaptiveInfo): Boolean =
  when (adaptiveInfo.windowSizeClass.windowWidthSizeClass) {
    WindowWidthSizeClass.COMPACT -> false
    WindowWidthSizeClass.MEDIUM -> true
    WindowWidthSizeClass.EXPANDED -> true
    else -> false
  }

@Composable
private fun <T : NavArgument> rememberListDetailNavArguments(
  args: List<T>,
  isDetailPane: (T) -> Boolean,
): Pair<List<T>, Map<T, List<T>>> =
  remember(args) {
    val primary = mutableListOf<T>()
    val secondaryLookup = mutableMapOf<T, List<T>>()
    val secondary = mutableListOf<T>()
    for (arg in args) {
      when {
        isDetailPane(arg) -> {
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
