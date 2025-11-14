// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.window.core.layout.WindowWidthSizeClass
import com.slack.circuit.foundation.DelicateCircuitFoundationApi
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.NavStackList
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedStateHolder
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.navigation.ListDetailScaffoldStyle.Companion.defaultListDetailScaffoldStyle
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope
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

object AdaptiveListDetailAnimatedScope : AnimatedScope

/**
 * A [NavDecoration] that displays navigation content in a list-detail layout using Material 3's
 * adaptive pane scaffold.
 *
 * On wide screens, shows a two-pane layout with list and detail panes. On narrow screens, falls
 * back to the [normalDecoratorFactory].
 */
@ExperimentalMaterial3AdaptiveApi
@OptIn(ExperimentalCircuitApi::class, ExperimentalSharedTransitionApi::class)
class AdaptiveListDetailNavDecoration(
  screenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
  normalDecoratorFactory: AnimatedNavDecorator.Factory,
  detailPaneDecoratorFactory: AnimatedNavDecorator.Factory,
  private val showInDetailPane: (NavArgument) -> Boolean,
  private val listDetailScaffoldStyle: (Density, WindowAdaptiveInfo) -> ListDetailScaffoldStyle =
    ::defaultListDetailScaffoldStyle,
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
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val paneScaffoldStyle =
      listDetailScaffoldStyle(LocalDensity.current, currentWindowAdaptiveInfo())
    // Decorate the content layout based on the window size.
    // - Wide enough show as list detail panes
    // - Otherwise stack normally
    AnimatedContent(paneScaffoldStyle.shouldUsePaneLayout) { shouldUsePaneLayout ->
      ProvideAnimatedTransitionScope(AdaptiveListDetailAnimatedScope, this) {
        if (shouldUsePaneLayout) {
          // todo I had expected the `PaneContent` retains to work with just the navigable_registry
          rememberRetainedStateHolder().RetainedStateProvider("list-detail-${args.current.key}") {
            ListDetailContent(
              args = args,
              navigator = navigator,
              listDetailScaffoldStyle = paneScaffoldStyle,
              modifier = modifier,
              content = content,
            )
          }
        } else {
          delegate.DecoratedContent(
            args = args,
            navigator = navigator,
            modifier = modifier,
            content = content,
          )
        }
      }
    }
  }

  @OptIn(
    ExperimentalMaterial3AdaptiveApi::class, // For ListDetailPaneScaffold
    ExperimentalComposeUiApi::class, // For PredictiveBackHandler
  )
  @Composable
  private fun <T : NavArgument> ListDetailContent(
    args: NavStackList<T>,
    navigator: Navigator,
    listDetailScaffoldStyle: ListDetailScaffoldStyle,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val (primaryArgs, secondaryLookup) = rememberListDetailNavArguments(args, showInDetailPane)
    val secondaryLookupTransition = updateTransition(secondaryLookup)
    delegate.DecoratedContent(primaryArgs, navigator, modifier) { primary ->
      val secondaryArgs =
        with(secondaryLookupTransition) { currentState[primary] ?: targetState[primary] }

      val hasSecondary = !secondaryArgs?.entries.isNullOrEmpty()
      val scaffoldValue = if (hasSecondary) PrimarySecondary else Primary
      val scaffoldState = remember { MutableThreePaneScaffoldState(scaffoldValue) }

      var initialAnchorIndex by rememberRetained { mutableIntStateOf(-1) }
      val paneExpansionState =
        rememberPaneExpansionState(
          key = scaffoldValue.paneExpansionStateKey,
          anchors = listDetailScaffoldStyle.anchors,
          initialAnchoredIndex = initialAnchorIndex,
        )
      DisposableEffect(paneExpansionState.currentAnchor) {
        initialAnchorIndex =
          listDetailScaffoldStyle.anchors.indexOf(paneExpansionState.currentAnchor)
        onDispose {}
      }
      LaunchedEffect(Unit) {
        if (listDetailScaffoldStyle.anchors.isNotEmpty() && initialAnchorIndex != -1) {
          paneExpansionState.animateTo(listDetailScaffoldStyle.anchors[initialAnchorIndex])
        }
      }
      LaunchedEffect(scaffoldValue) { scaffoldState.animateTo(scaffoldValue) }
      ListDetailPaneScaffold(
        modifier = Modifier.fillMaxSize(),
        directive = listDetailScaffoldStyle.directive,
        scaffoldState = scaffoldState,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = listDetailScaffoldStyle.dragHandle,
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
      val singleSecondary = hasSecondary && secondaryArgs.size == 1
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

/**
 * Configuration for the pane scaffold layout, including layout behavior, directives, resize
 * anchors, and drag handle customization.
 */
@Stable
class ListDetailScaffoldStyle
@ExperimentalMaterial3AdaptiveApi
constructor(
  val shouldUsePaneLayout: Boolean = false,
  val directive: PaneScaffoldDirective = PaneScaffoldDirective.Default,
  val anchors: List<PaneExpansionAnchor> = emptyList(),
  val dragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? = null,
) {

  companion object {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    val DefaultDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit) =
      { state ->
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
      }

    /**
     * Creates the default [ListDetailScaffoldStyle] based on window size class. Enables pane layout
     * for medium and expanded windows, with drag handles and appropriate resize anchors.
     */
    @ExperimentalMaterial3AdaptiveApi
    @Stable
    fun defaultListDetailScaffoldStyle(
      density: Density,
      windowInfo: WindowAdaptiveInfo,
    ): ListDetailScaffoldStyle {
      val directive = calculatePaneScaffoldDirective(windowInfo)

      val shouldUsePaneLayout =
        when (windowInfo.windowSizeClass.windowWidthSizeClass) {
          WindowWidthSizeClass.COMPACT -> false
          WindowWidthSizeClass.MEDIUM -> true
          WindowWidthSizeClass.EXPANDED -> true
          else -> false
        }

      val minimumPaneSize =
        when (windowInfo.windowSizeClass.windowWidthSizeClass) {
          WindowWidthSizeClass.MEDIUM -> 240.dp
          WindowWidthSizeClass.EXPANDED -> 300.dp
          else -> Dp.Unspecified
        }

      // todo Anchors vs Resizeable with minimums
      val anchors = density.paneExpansionAnchors(windowInfo, directive, minimumPaneSize)

      return ListDetailScaffoldStyle(
        shouldUsePaneLayout = shouldUsePaneLayout,
        dragHandle = DefaultDragHandle,
        directive = directive,
        anchors = anchors,
      )
    }
  }
}

/**
 * Calculates pane expansion anchors for resizing, including minimum size, default width, and hinge
 * positions on foldable devices.
 */
@ExperimentalMaterial3AdaptiveApi
private fun Density.paneExpansionAnchors(
  windowInfo: WindowAdaptiveInfo,
  directive: PaneScaffoldDirective,
  minimumPaneSize: Dp,
): List<PaneExpansionAnchor> = buildList {
  if (minimumPaneSize.isSpecified) {
    add(PaneExpansionAnchor.Offset.fromStart(minimumPaneSize))
  }
  if (minimumPaneSize.isUnspecified || directive.defaultPanePreferredWidth > minimumPaneSize) {
    add(PaneExpansionAnchor.Offset.fromStart(directive.defaultPanePreferredWidth))
  }

  windowInfo.windowPosture.hingeList.forEach { info ->
    if (info.isVertical && info.isSeparating) {
      val hingeOffset = info.bounds.center.x.toDp()
      if (hingeOffset != minimumPaneSize || hingeOffset != directive.defaultPanePreferredWidth) {
        add(PaneExpansionAnchor.Offset.fromStart(hingeOffset))
      }
    }
  }
  if (minimumPaneSize.isSpecified) {
    add(PaneExpansionAnchor.Offset.fromEnd(minimumPaneSize))
  }
  // Don't lock this to a single anchor
  if (size == 1) {
    clear()
  }
}

/**
 * Strategy that partitions navigation arguments into primary (list) and secondary (detail) panes
 * based on the [showInDetailPane] predicate.
 */
@Composable
internal fun <T : NavArgument> rememberListDetailNavArguments(
  navStackList: NavStackList<T>,
  showInDetailPane: (T) -> Boolean,
): Pair<NavStackList<T>, Map<T, NavStackList<T>>> =
  remember(navStackList) {
    val primary = mutableListOf<T>()
    val secondaryLookup = mutableMapOf<T, NavStackList<T>>()
    val secondary = mutableListOf<T>()
    for (arg in navStackList.backwardStack()) {
      when {
        showInDetailPane(arg) -> {
          secondary += arg
        }
        else -> {
          primary += arg
          secondaryLookup[arg] = NavStackList(secondary.toList())
          secondary.clear()
        }
      }
    }
    if (primary.isEmpty()) {
      NavStackList(secondary.toList()) to emptyMap()
    } else {
      // Show the next secondary if it exists
      navStackList
        .forwardStack()
        .firstOrNull()
        ?.takeIf { showInDetailPane(it) }
        ?.let { secondaryLookup.getOrPut(primary.first()) { NavStackList(listOf(it)) } }
      NavStackList(primary.toList()) to secondaryLookup.toMap()
    }
  }
