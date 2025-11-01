// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.toString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.isEmpty
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.NavigatorDefaults.DefaultDecorator.DefaultAnimatedState
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateHolder
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedStateHolder
import com.slack.circuit.retained.rememberRetainedStateRegistry
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A composable that provides the core navigation and state management for Circuit-based navigation
 * systems. It manages the rendering of screens from a backstack, handles navigation transitions,
 * and coordinates result passing between screens when navigating backward. This is the primary
 * entry point for creating navigable content surfaces in Circuit.
 *
 * ## Features
 * - **State Management**: Manages saveable and retained state for each screen in the backstack with
 *   automatic preservation across configuration changes
 * - **Navigation Transitions**: Handles animated transitions between screens using [NavDecoration]
 *   or custom [AnimatedNavDecorator]
 * - **Result Handling**: Automatically manages screen results when using
 *   [rememberAnsweringNavigator] to pass data back from child screens
 *
 * ## Usage
 *
 * ```kotlin
 * setContent {
 *   val backStack = rememberSaveableBackStack(root = HomeScreen)
 *   val navigator = rememberCircuitNavigator(backStack)
 *   NavigableCircuitContent(navigator, backStack)
 * }
 * ```
 *
 * ## State Management
 * This creates an isolated retained state registry for the navigation graph to ensure proper state
 * preservation across configuration changes when using [rememberRetained]. This prevents state loss
 * for off-screen backstack records, even when they're not actively composed. See the implementation
 * comments for technical details.
 *
 * @param navigator The [Navigator] used to handle navigation events. Typically created via
 *   [rememberCircuitNavigator].
 * @param backStack The [BackStack] containing the stack of [Record]s to display. Must have at least
 *   one record. Typically created via [rememberSaveableBackStack].
 * @param modifier The [Modifier] to apply to the content.
 * @param circuit The [Circuit] instance providing UI factories and configuration. Defaults to
 *   [LocalCircuit].
 * @param providedValues Optional map of [ProvidedValues] to make available to specific records in
 *   the backstack. These values will be provided via composition locals when the corresponding
 *   record is displayed.
 * @param decoration The [NavDecoration] used to decorate navigation transitions. Defaults to the
 *   circuit's default decoration.
 * @param decoratorFactory Optional [AnimatedNavDecorator.Factory] to create custom animated
 *   transitions. If provided, takes precedence over [decoration].
 * @param unavailableRoute A composable function invoked when a screen cannot be rendered (e.g., no
 *   UI factory available). Defaults to the circuit's [Circuit.onUnavailableContent].
 * @see rememberAnsweringNavigator for requesting results from child screens
 * @see rememberSaveableBackStack for creating a backstack
 * @see rememberCircuitNavigator for creating a navigator
 */
@OptIn(ExperimentalCircuitApi::class)
@Composable
public fun <R : Record> NavigableCircuitContent(
  navigator: Navigator,
  backStack: BackStack<R>,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  providedValues: Map<out Record, ProvidedValues> = emptyMap(),
  decoration: NavDecoration = circuit.defaultNavDecoration,
  decoratorFactory: AnimatedNavDecorator.Factory? = null,
  unavailableRoute: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  NavigableCircuitContent(
    navigator = navigator,
    backStack = backStack,
    modifier = modifier,
    circuit = circuit,
    answeringResultHandler = rememberAnsweringResultHandler(),
    providedValues = providedValues,
    decoration = decoration,
    decoratorFactory = decoratorFactory,
    unavailableRoute = unavailableRoute,
  )
}

/**
 * An experimental variant of [NavigableCircuitContent] that exposes [answeringResultHandler] for a
 * a [AnsweringResultHandler] to be provided.
 *
 * ## Usage
 *
 * ```kotlin
 * setContent {
 *   val backStack = rememberSaveableBackStack(root = HomeScreen)
 *   val navigator = rememberCircuitNavigator(backStack)
 *   val answeringResultHandler = rememberAnsweringResultHandler()
 *   NavigableCircuitContent(navigator, backStack, answeringResultHandler)
 * }
 * ```
 */
@ExperimentalCircuitApi // For AnsweringResultHandler
@Composable
public fun <R : Record> NavigableCircuitContent(
  navigator: Navigator,
  backStack: BackStack<R>,
  answeringResultHandler: AnsweringResultHandler,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  providedValues: Map<out Record, ProvidedValues> = emptyMap(),
  decoration: NavDecoration = circuit.defaultNavDecoration,
  decoratorFactory: AnimatedNavDecorator.Factory? = null,
  unavailableRoute: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  if (backStack.isEmpty) return

  /*
   * We store the RetainedStateRegistries for each back stack entry into an 'navigation content'
   * RetainedStateRegistry. If we don't do this, those registries would be stored directly in the
   * current LocalRetainedStateRegistry value, which will mostly likely be the
   * lifecycleRetainedStateRegistry. On Android, that lifecycleRetainedStateRegistry will drop
   * any 'unclaimed' values when the host Activity is recreated. Since records on the back stack
   * aren't attached to composition, they can't claim their retained registries and thus we drop
   * all of the state for the record. See #1046.
   *
   * Using this 'navigation content' registry means that _it_ will be stored in the
   * lifecycleRetainedStateRegistry instead, and any back stack record registries stored within
   * the 'navigation content' registry. The difference is that NavigableCircuitContent
   * will be attached to composition for the entire lifetime that we care about, and thus will
   * be able to save/claim the 'navigation content' registry on recreations. Since any back stack
   * registries are nested in this 'navigation content' registry, everything is
   * saved/claimed correctly. As a diagram, it looks like this:
   *                          ┌────────────────┐
   *                          │ ContinuityRSR  │
   *                          └───────▲────────┘
   *                      ┌───────────┴─────────┐
   *                 ┌────► NavigableContentRSR ◄───────┐
   *                 │    └───────────▲─────────┘       │
   *          ┌──────┴──────┐  ┌──────┴──────┐   ┌──────┴──────┐
   *          │ ScreenA RSR │  │ ScreenB RSR │   │ ScreenC RSR │
   *          └──────▲──────┘  └──────▲──────┘   └──────▲──────┘
   * ┌─────────────┐ │         ┌──────┴──────┐   ┌──────┴──────┐
   * │retainedState├─┤         │retainedState│   │retainedState│
   * └─────────────┘ │         └─────────────┘   └─────────────┘
   * ┌─────────────┐ │
   * │retainedState├─┘         (RSR = RetainedStateRegistry)
   * └─────────────┘
   */
  val outerKey = "_navigable_registry_${currentCompositeKeyHashCode.toString(MaxSupportedRadix)}"
  val outerRegistry = rememberRetainedStateRegistry(key = outerKey)

  val navDecoration =
    remember(decoration, decoratorFactory) {
      // User specified decorator takes precedence over a default decoration.
      if (decoratorFactory != null) {
        AnimatedNavDecoration(circuit.animatedScreenTransforms, decoratorFactory)
      } else {
        decoration
      }
    }

  // Delegates pop results to the answering result handler.
  val resultNavigator =
    remember(navigator, backStack, answeringResultHandler) {
      AnsweringResultNavigator(navigator, backStack, answeringResultHandler)
    }

  CompositionLocalProvider(LocalRetainedStateRegistry provides outerRegistry) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val retainedStateHolder = rememberRetainedStateHolder()
    val contentProviderState =
      remember(saveableStateHolder, retainedStateHolder) {
          ContentProviderState(
            saveableStateHolder = saveableStateHolder,
            retainedStateHolder = retainedStateHolder,
            backStack = backStack,
            navigator = resultNavigator,
            circuit = circuit,
            unavailableRoute = unavailableRoute,
          )
        }
        .apply {
          lastBackStack = backStack
          lastNavigator = resultNavigator
          lastCircuit = circuit
          lastUnavailableRoute = unavailableRoute
        }
    val activeContentProviders = buildCircuitContentProviders(backStack = backStack)
    val circuitProvidedValues =
      providedValuesForBackStack(backStack, circuit.backStackLocalProviders)
    navDecoration.DecoratedContent(activeContentProviders, modifier) { provider ->
      val record = provider.record

      // Remember the `providedValues` lookup because this composition can live longer than
      // the record is present in the backstack, if the decoration is animated for example.
      val values = remember(record) { providedValues[record] }?.provideValues()
      val circuitProvidedValues =
        remember(record) { circuitProvidedValues[record] }?.provideValues()
      val providedLocals =
        remember(values, circuitProvidedValues) {
          (values.orEmpty() + circuitProvidedValues.orEmpty()).toTypedArray()
        }
      CompositionLocalProvider(
        LocalBackStack provides backStack,
        LocalAnsweringResultHandler provides answeringResultHandler,
        *providedLocals,
      ) {
        provider.content(record, contentProviderState)
      }
    }
  }
}

/** [Navigator] that sends pending results to the [answeringResultHandler] when popped. */
@ExperimentalCircuitApi
private class AnsweringResultNavigator<R : Record>(
  private val navigator: Navigator,
  private val backStack: BackStack<R>,
  private val answeringResultHandler: AnsweringResultHandler,
) : Navigator by navigator {
  override fun pop(result: PopResult?): Screen? {
    // Run in a snapshot to ensure the sendResult doesn't get missed.
    return Snapshot.withMutableSnapshot {
      val popped = navigator.pop(result)
      if (result != null) {
        // Send the pending result to our new top record, but only if it's expecting one
        backStack.topRecord?.apply {
          if (answeringResultHandler.expectingResult(key)) {
            answeringResultHandler.sendResult(key, result)
          }
        }
      }
      popped
    }
  }
}

/** A simple holder class for a [record] and its associated [content]. */
@Immutable
public class RecordContentProvider<R : Record>(
  public val record: R,
  internal val content: @Composable (R, ContentProviderState<R>) -> Unit,
) : NavArgument {

  override val screen: Screen
    get() = record.screen

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as RecordContentProvider<*>
    return record == other.record
  }

  override fun hashCode(): Int {
    val result = record.hashCode()
    return result
  }

  override fun toString(): String = "RecordContentProvider(record=$record)"
}

@Composable
private fun <R : Record> buildCircuitContentProviders(
  backStack: BackStack<R>
): List<RecordContentProvider<R>> {
  val previousContentProviders = remember { mutableMapOf<String, RecordContentProvider<R>>() }
  val activeRecordKeys = remember { mutableSetOf<String>() }
  val recordKeys by
    remember { mutableStateOf(emptySet<String>()) }
      .apply { value = backStack.mapTo(mutableSetOf()) { it.key } }
  val latestBackStack by rememberUpdatedState(backStack)
  DisposableEffect(recordKeys) {
    // Delay cleanup until the next backstack change.
    // - Any record in composition is considered active
    // - Any record in the backstack can be shown by a decorator
    // - Any reachable record can be shown on a root reset
    val contentNotInBackStack =
      previousContentProviders.keys.filterNot {
        it in activeRecordKeys ||
          it in recordKeys ||
          // Depth of 2 to exclude records that are late at leaving the composition.
          latestBackStack.isRecordReachable(key = it, depth = 2, includeSaved = true)
      }
    onDispose {
      // Only remove the keys that are no longer in the backstack or composition.
      contentNotInBackStack
        .filterNot {
          latestBackStack.isRecordReachable(key = it, depth = 1, includeSaved = true) ||
            it in activeRecordKeys
        }
        .forEach { previousContentProviders.remove(it) }
    }
  }
  return backStack.map { record ->
    // Query the previous content providers map, so that we use the same
    // RecordContentProvider instances across calls.
    previousContentProviders.getOrPut(record.key) {
      RecordContentProvider(
        record = record,
        content =
          createRecordContent(
            onActive = { activeRecordKeys.add(record.key) },
            onDispose = { activeRecordKeys.remove(record.key) },
          ),
      )
    }
  }
}

@Stable
public class ContentProviderState<R : Record>(
  internal val saveableStateHolder: SaveableStateHolder,
  internal val retainedStateHolder: RetainedStateHolder,
  backStack: BackStack<R>,
  navigator: Navigator,
  circuit: Circuit,
  unavailableRoute: @Composable (screen: Screen, modifier: Modifier) -> Unit,
) {

  internal var lastBackStack by mutableStateOf(backStack)
  internal var lastNavigator by mutableStateOf(navigator)
  internal var lastCircuit by mutableStateOf(circuit)
  internal var lastUnavailableRoute by mutableStateOf(unavailableRoute)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ContentProviderState<*>) return false

    if (saveableStateHolder != other.saveableStateHolder) return false
    if (retainedStateHolder != other.retainedStateHolder) return false

    return true
  }

  override fun hashCode(): Int {
    var result = saveableStateHolder.hashCode()
    result = 31 * result + retainedStateHolder.hashCode()
    return result
  }
}

private fun <R : Record> createRecordContent(onActive: () -> Unit, onDispose: () -> Unit) =
  movableContentOf<R, ContentProviderState<R>> { record, contentProviderState ->
    with(contentProviderState) {
      val lifecycle = remember { MutableRecordLifecycle() }
      SideEffect { lifecycle.isActive = lastBackStack.topRecord == record }
      saveableStateHolder.SaveableStateProvider(record.registryKey) {
        // Provides a RetainedStateRegistry that is maintained independently for each record while
        // the record exists in the back stack.
        retainedStateHolder.RetainedStateProvider(record.registryKey) {
          CompositionLocalProvider(LocalRecordLifecycle provides lifecycle) {
            CircuitContent(
              screen = record.screen,
              navigator = lastNavigator,
              circuit = lastCircuit,
              unavailableContent = lastUnavailableRoute,
              key = record.key,
            )
          }
        }
      }
      // Remove saved states for records that are no longer in the back stack
      DisposableEffect(record.registryKey) {
        onDispose {
          if (!lastBackStack.containsRecord(record, includeSaved = true)) {
            retainedStateHolder.removeState(record.registryKey)
            saveableStateHolder.removeState(record.registryKey)
          }
        }
      }
    }
    // Track if the movableContent is still active to correctly reuse it and not create a new one.
    DisposableEffect(Unit) {
      onActive()
      onDispose { onDispose() }
    }
  }

/** The maximum radix available for conversion to and from strings. */
private const val MaxSupportedRadix = 36

private val Record.registryKey: String
  get() = "_registry_${key}"

/** Default values and common alternatives used by navigable composables. */
public object NavigatorDefaults {

  private val FastOutExtraSlowInEasing = CubicBezierEasing(0.208333f, 0.82f, 0.25f, 1f)
  private val AccelerateEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
  private const val DEBUG_MULTIPLIER = 1
  private const val SHORT_DURATION = 83 * DEBUG_MULTIPLIER
  private const val NORMAL_DURATION = 450 * DEBUG_MULTIPLIER

  public object DefaultDecoratorFactory : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> = DefaultDecorator()
  }

  /**
   * The [ContentTransform] used for 'forward' navigation changes (i.e. items added to stack). This
   * isn't meant for public consumption, so be aware that this may be removed/changed at any time.
   */
  @InternalCircuitApi public val forward: ContentTransform by lazy { computeTransition(1) }

  /**
   * The [ContentTransform] used for 'backward' navigation changes (i.e. items popped off stack).
   * This isn't meant for public consumption, so be aware that this may be removed/changed at any
   * time.
   */
  @InternalCircuitApi public val backward: ContentTransform by lazy { computeTransition(-1) }

  private fun computeTransition(sign: Int): ContentTransform {
    val enterTransition =
      fadeIn(
        animationSpec =
          tween(
            durationMillis = SHORT_DURATION,
            delayMillis = if (sign > 0) 50 else 0,
            easing = LinearEasing,
          )
      ) +
        slideInHorizontally(
          initialOffsetX = { fullWidth -> (fullWidth / 10) * sign },
          animationSpec = tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing),
        ) +
        if (sign > 0) {
          expandHorizontally(
            animationSpec =
              tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing),
            initialWidth = { (it * .9f).toInt() },
            expandFrom = if (sign > 0) Alignment.Start else Alignment.End,
          )
        } else {
          EnterTransition.None
        }

    val exitTransition =
      fadeOut(
        animationSpec =
          tween(
            durationMillis = if (sign > 0) NORMAL_DURATION else SHORT_DURATION,
            delayMillis = if (sign > 0) 0 else 50,
            easing = AccelerateEasing,
          )
      ) +
        slideOutHorizontally(
          targetOffsetX = { fullWidth -> (fullWidth / 10) * -sign },
          animationSpec = tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing),
        ) +
        if (sign > 0) {
          shrinkHorizontally(
            animationSpec =
              tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing),
            targetWidth = { (it * .9f).toInt() },
            shrinkTowards = Alignment.End,
          )
        } else {
          ExitTransition.None
        }

    return enterTransition togetherWith exitTransition
  }

  public class DefaultDecorator<T : NavArgument> :
    AnimatedNavDecorator<T, DefaultAnimatedState<T>> {

    public data class DefaultAnimatedState<T : NavArgument>(val args: List<T>) : AnimatedNavState {
      override val backStack: List<NavArgument> = args
    }

    override fun targetState(args: List<T>): DefaultAnimatedState<T> {
      return DefaultAnimatedState(args)
    }

    @Composable
    public override fun updateTransition(args: List<T>): Transition<DefaultAnimatedState<T>> {
      return updateTransition(targetState(args))
    }

    @OptIn(InternalCircuitApi::class)
    override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
      animatedNavEvent: AnimatedNavEvent
    ): ContentTransform {
      // A transitionSpec should only use values passed into the `AnimatedContent`, to minimize
      // the transitionSpec recomposing.
      // The states are available as `targetState` and `initialState`.
      return when (animatedNavEvent) {
        AnimatedNavEvent.GoTo -> forward
        AnimatedNavEvent.Pop -> backward
        AnimatedNavEvent.RootReset -> fadeIn() togetherWith fadeOut()
      }.using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(clip = false)
      )
    }

    @Composable
    public override fun AnimatedContentScope.Decoration(
      targetState: DefaultAnimatedState<T>,
      innerContent: @Composable (T) -> Unit,
    ) {
      innerContent(targetState.args.first())
    }
  }

  /** An empty [NavDecoration] that emits the content with no surrounding decoration or logic. */
  public object EmptyDecoration : NavDecoration {
    @Composable
    override fun <T : NavArgument> DecoratedContent(
      args: List<T>,
      modifier: Modifier,
      content: @Composable (T) -> Unit,
    ) {
      Box(modifier = modifier) { content(args.first()) }
    }
  }
}

/**
 * Delicate API to access the [BackStack] from within a [CircuitContent] or
 * [rememberAnsweringNavigator] composable, useful for cases where we create nested nav handling.
 *
 * This is generally considered an internal API to Circuit, but can be useful for interop cases and
 * testing of [rememberAnsweringNavigator] APIs. As such, it's public but annotated as
 * [DelicateCircuitFoundationApi].
 */
@DelicateCircuitFoundationApi
public val LocalBackStack: ProvidableCompositionLocal<BackStack<out Record>?> = compositionLocalOf {
  null
}

/**
 * Delicate API to access the [AnsweringResultHandler] from within a [NavigableCircuitContent] or
 * [rememberAnsweringNavigator] composable, useful for cases where we create nested nav handling.
 *
 * This is generally considered an internal API to Circuit, but can be useful for interop cases and
 * testing of [rememberAnsweringNavigator] APIs. As such, it's public but annotated as
 * [DelicateCircuitFoundationApi].
 */
@OptIn(ExperimentalCircuitApi::class)
@DelicateCircuitFoundationApi
public val LocalAnsweringResultHandler: ProvidableCompositionLocal<AnsweringResultHandler?> =
  compositionLocalOf {
    null
  }
