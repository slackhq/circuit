// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.isEmpty
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LocalCanRetainChecker
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList

@Composable
public fun <R : Record> NavigableCircuitContent(
  navigator: Navigator,
  backStack: BackStack<R>,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  providedValues: ImmutableMap<out Record, ProvidedValues> = providedValuesForBackStack(backStack),
  decoration: NavDecoration = circuit.defaultNavDecoration,
  unavailableRoute: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  val activeContentProviders =
    buildCircuitContentProviders(
      backStack = backStack,
      navigator = navigator,
      circuit = circuit,
      unavailableRoute = unavailableRoute,
    )

  if (backStack.isEmpty) return

  /*
   * We store the RetainedStateRegistries for each back stack entry into an 'navigation content'
   * RetainedStateRegistry. If we don't do this, those registries would be stored directly in the
   * current LocalRetainedStateRegistry value, which will mostly likely be the
   * continuityRetainedStateRegistry. On Android, that continuityRetainedStateRegistry will drop
   * any 'unclaimed' values when the host Activity is recreated. Since records on the back stack
   * aren't attached to composition, they can't claim their retained registries and thus we drop
   * all of the state for the record. See #1046.
   *
   * Using this 'navigation content' registry means that _it_ will be stored in the
   * continuityRetainedStateRegistry instead, and any back stack record registries stored within
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
  val outerKey = "_navigable_registry_${currentCompositeKeyHash.toString(MaxSupportedRadix)}"
  val outerRegistry = rememberRetained(key = outerKey) { RetainedStateRegistry() }

  val saveableStateHolder = rememberSaveableStateHolder()

  CompositionLocalProvider(LocalRetainedStateRegistry provides outerRegistry) {
    decoration.DecoratedContent(activeContentProviders, backStack.size, modifier) { provider ->
      val record = provider.record

      saveableStateHolder.SaveableStateProvider(record.key) {
        // Remember the `providedValues` lookup because this composition can live longer than
        // the record is present in the backstack, if the decoration is animated for example.
        val values = remember(record) { providedValues[record] }?.provideValues()
        val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }

        CompositionLocalProvider(LocalBackStack provides backStack, *providedLocals) {
          provider.content(record)
        }
      }
    }
  }
}

/** A simple holder class for a [record] and its associated [content]. */
@Immutable
public class RecordContentProvider<R : Record>(
  public val record: R,
  internal val content: @Composable (R) -> Unit,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as RecordContentProvider<*>

    if (record != other.record) return false
    if (content != other.content) return false

    return true
  }

  override fun hashCode(): Int {
    var result = record.hashCode()
    result = 31 * result + content.hashCode()
    return result
  }

  override fun toString(): String = "RecordContentProvider(record=$record)"
}

@Composable
private fun <R : Record> buildCircuitContentProviders(
  backStack: BackStack<R>,
  navigator: Navigator,
  circuit: Circuit,
  unavailableRoute: @Composable (screen: Screen, modifier: Modifier) -> Unit,
): ImmutableList<RecordContentProvider<R>> {
  val previousContentProviders = remember { mutableMapOf<String, RecordContentProvider<R>>() }

  val lastBackStack by rememberUpdatedState(backStack)
  val lastNavigator by rememberUpdatedState(navigator)
  val lastCircuit by rememberUpdatedState(circuit)
  val lastUnavailableRoute by rememberUpdatedState(unavailableRoute)

  fun createRecordContent() =
    movableContentOf<R> { record ->
      val recordInBackStackRetainChecker =
        remember(lastBackStack, record) {
          CanRetainChecker { lastBackStack.containsRecord(record, includeSaved = true) }
        }

      val lifecycle =
        remember { MutableRecordLifecycle() }.apply { isActive = lastBackStack.topRecord == record }

      CompositionLocalProvider(LocalCanRetainChecker provides recordInBackStackRetainChecker) {
        // Now provide a new registry to the content for it to store any retained state in,
        // along with a retain checker which is always true (as upstream registries will
        // maintain the lifetime), and the other provided values
        val recordRetainedStateRegistry =
          rememberRetained(key = record.registryKey) { RetainedStateRegistry() }

        CompositionLocalProvider(
          LocalRetainedStateRegistry provides recordRetainedStateRegistry,
          LocalCanRetainChecker provides CanRetainChecker.Always,
          LocalRecordLifecycle provides lifecycle,
        ) {
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

  return lastBackStack
    .iterator()
    .asSequence()
    .map { record ->
      // Query the previous content providers map, so that we use the same
      // RecordContentProvider instances across calls.
      previousContentProviders.getOrPut(record.key) {
        RecordContentProvider(record = record, content = createRecordContent())
      }
    }
    .toImmutableList()
    .also { list ->
      // Update the previousContentProviders map so we can reference it on the next call
      previousContentProviders.clear()
      for (provider in list) {
        previousContentProviders[provider.record.key] = provider
      }
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

  /** The default [NavDecoration] used in navigation. */
  // Mirrors the forward and backward transitions of activities in Android 34
  public object DefaultDecoration : NavDecoration {

    /**
     * The [ContentTransform] used for 'forward' navigation changes (i.e. items added to stack).
     * This isn't meant for public consumption, so be aware that this may be removed/changed at any
     * time.
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
            animationSpec =
              tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing),
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
            animationSpec =
              tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing),
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

    @OptIn(ExperimentalSharedTransitionApi::class)
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
      ) {
        ProvideAnimatedTransitionScope(Navigation, this) { content(it.first()) }
      }
    }
  }

  /** An empty [NavDecoration] that emits the content with no surrounding decoration or logic. */
  public object EmptyDecoration : NavDecoration {
    @Composable
    override fun <T> DecoratedContent(
      args: ImmutableList<T>,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit,
    ) {
      content(args.first())
    }
  }
}

/**
 * Internal API to access the [BackStack] from within a [CircuitContent] or
 * [rememberAnsweringNavigator] composable, useful for cases where we create nested nav handling.
 */
internal val LocalBackStack = compositionLocalOf<BackStack<out Record>?> { null }
