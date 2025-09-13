// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.window.core.layout.WindowWidthSizeClass
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.DelicateCircuitFoundationApi
import com.slack.circuit.foundation.LocalBackStack
import com.slack.circuit.foundation.RecordContentProvider
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException

interface PrimaryScreen : Screen

interface SecondaryScreen : Screen

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

// todo Generalize this
//  - Allow for styling etc
@OptIn(ExperimentalCircuitApi::class, ExperimentalSharedTransitionApi::class)
class AdaptiveNavDecoration(
  screenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
  private val decoratorFactory: AnimatedNavDecorator.Factory,
  private val backgroundColor: @Composable () -> Color = { Color.Unspecified },
  private val onGoTo: (Screen) -> Unit = {},
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

  @OptIn(DelicateCircuitFoundationApi::class)
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
      // todo Backstack won't save this...
      val backStack = LocalBackStack.current as? BackStack<BackStack.Record>
      var standbyArgument by rememberRetainedSaveable { mutableStateOf<T?>(null) }
      val argsTransition = updateTransition(args, label = "AdaptiveNavDecoration")
      val currentArgs = argsTransition.currentState
      val targetArgs = argsTransition.targetState
      val topArg = targetArgs.first()
      when {
        // Root change
        currentArgs.last() != targetArgs.last() -> standbyArgument = null
        // Forward
        currentArgs.size < targetArgs.size -> standbyArgument = null
        // Back on last
        currentArgs.size > 1 && targetArgs.size == 1 -> {
          remember(targetArgs) {
            standbyArgument =
              currentArgs
                .first()
                .takeIf { it.screen is SecondaryScreen }
                ?.also {
                  val primaryRecord = (topArg as? RecordContentProvider<*>)?.record
                  val secondaryRecord = (it as? RecordContentProvider<*>)?.record
                  if (backStack != null && primaryRecord != null && secondaryRecord != null) {
                    backStack.stashTogether(primaryRecord, secondaryRecord)
                  }
                }
          }
        }
      }
      val standby = standbyArgument

      // todo This needs to merge with the decoration pop
      if (
        standby != null && currentArgs == targetArgs
      ) { // || args.size == 2 && args.get(1) is SecondaryScreen
        // Allow a swipe to restore the previous screen
        AnimatedContent(
          standby,
          transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        ) {
          ProvideAnimatedTransitionScope(
            SharedElementTransitionScope.AnimatedScope.Navigation,
            this,
          ) {

            // todo Layout
            val state =
              rememberSwipeState(
                standby,
                onRestored = { onGoTo(standby.screen) },
                onDismissed = { onPop() },
              )
            Box(
              modifier
                .onLayoutRectChanged {
                  state.maxWidth = it.width.toFloat()
                  state.offset = it.width.toFloat()
                }
                .draggable(
                  state = state.draggableState,
                  orientation = Orientation.Horizontal,
                  enabled = true,
                  reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                  onDragStopped = { velocity -> state.onDragStopped(velocity) },
                )
            ) {
              content(topArg)
              OptionalLayout(
                shouldLayout = { state.offset != state.maxWidth },
                modifier = Modifier,
              ) {
                content(standby)
              }
            }
          }
        }
      } else {
        delegate.DecoratedContent(args, modifier, content)
      }
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
    val secondaryLookupTransition = updateTransition(secondaryLookup)
    sideBySideDelegate.DecoratedContent(primaryArgs, modifier) { primary ->
      val secondaryArgs =
        with(secondaryLookupTransition) { currentState[primary] ?: targetState[primary] }

      val hasSecondary = !secondaryArgs.isNullOrEmpty()
      val singleSecondary = hasSecondary && secondaryArgs.size == 1
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
            when {
              singleSecondary -> {
                // ScaffoldState animates the single case
                content(secondaryArgs.single())
              }
              hasSecondary -> {
                // Stack multiple with the normal decoration
                delegate.DecoratedContent(secondaryArgs, Modifier, content)
              }
            }
          }
        },
      )

      // Prevent the decorator from handling the back press
      PredictiveBackHandler(enabled = singleSecondary) { progress ->
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

/**
 * A composable which optionally lays out the given [content], depending on what [shouldLayout]
 * returns, whilst still keeping [content] attached to composition.
 *
 * This is useful when used in conjunction with `movableContentOf`, as that will destroy the
 * content's state 'very soon' after the content is detached from composition. By using this
 * composable, the content can remain attached to composition, but can avoid being measured, laid
 * out or drawn, allowing us to reduce wasted work.
 */
@Composable
internal fun OptionalLayout(
  shouldLayout: () -> Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier
      // If we're not visible, don't measure, layout (or draw)
      .let { if (shouldLayout()) it else it.emptyLayout() }
      // Content in the back stack should not be interactive until they're on top
      .pointerInput(Unit) {}
  ) {
    content()
  }
}

/** This no-ops measure + layout (and thus draw) for child content. */
private fun Modifier.emptyLayout(): Modifier = layout { _, constraints ->
  layout(constraints.minWidth, constraints.minHeight) {}
}

@Composable
private fun rememberSwipeState(
  key: Any?,
  onRestored: () -> Unit,
  onDismissed: () -> Unit,
  swipeThreshold: Float = 0.5f,
): SwipeState {
  return remember(key, swipeThreshold) { SwipeState(swipeThreshold) }
    .apply {
      this.onRestored = onRestored
      this.onDismissed = onDismissed
    }
}

@Stable
private class SwipeState(private val swipeThreshold: Float) {
  var offset by mutableFloatStateOf(0f)
  var maxWidth by mutableFloatStateOf(0f)

  var onRestored: () -> Unit = {}
  var onDismissed: () -> Unit = {}

  val progress: Float by derivedStateOf { if (maxWidth == 0f) 0f else offset / maxWidth }

  val draggableState = DraggableState { delta ->
    val newOffset = (offset + delta).coerceIn(0f, maxWidth)
    val resistance = calculateResistance(newOffset)
    offset = newOffset * resistance
  }

  fun performDrag(delta: Float): Float {
    val previousOffset = offset
    val newOffset = (offset + delta).coerceIn(0f, maxWidth)
    val resistance = calculateResistance(newOffset)
    offset = newOffset * resistance
    return offset - previousOffset
  }

  suspend fun onDragStopped(velocity: Float) {
    val thresholdValue = swipeThreshold * maxWidth

    val shouldDismiss = offset >= thresholdValue || velocity > 1000f
    val shouldRestore = offset <= thresholdValue || velocity > 1000f
    val targetOffset = if (shouldDismiss) maxWidth else 0f

    draggableState.drag(MutatePriority.PreventUserInput) {
      Animatable(offset).animateTo(targetOffset) { dragBy(value - offset) }
    }
    if (shouldRestore && targetOffset == 0f) {
      onRestored()
    } else if (shouldDismiss && targetOffset == maxWidth) {
      onDismissed()
    }
  }

  private fun calculateResistance(offset: Float): Float {
    return if (offset > maxWidth) {
      val overshoot = offset - maxWidth
      val resistanceFactor = 0.1f
      val resistance = overshoot * resistanceFactor
      (maxWidth + resistance) / offset
    } else {
      1f
    }
  }
}
