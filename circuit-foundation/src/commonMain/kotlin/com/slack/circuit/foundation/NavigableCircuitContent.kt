// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList

@Composable
public fun NavigableCircuitContent(
  navigator: Navigator,
  backstack: BackStack<out Record>,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  providedValues: ImmutableMap<out Record, ProvidedValues> = providedValuesForBackStack(backstack),
  decoration: NavDecoration = circuit.defaultNavDecoration,
  unavailableRoute: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  val activeContentProviders =
    backstack.buildCircuitContentProviders(
      navigator = navigator,
      circuit = circuit,
      unavailableRoute = unavailableRoute,
    )

  if (backstack.size > 0) {
    @Suppress("SpreadOperator")
    decoration.DecoratedContent(activeContentProviders, backstack.size, modifier) { provider ->
      val values = providedValues[provider.record]?.provideValues()
      val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }
      CompositionLocalProvider(*providedLocals) { provider.content(provider.record) }
    }
  }
}

/** A simple holder class for a [record] and its associated [content]. */
@Immutable
public class RecordContentProvider(
  public val record: Record,
  internal val content: @Composable (Record) -> Unit,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as RecordContentProvider

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
private fun BackStack<out Record>.buildCircuitContentProviders(
  navigator: Navigator,
  circuit: Circuit,
  unavailableRoute: @Composable (screen: Screen, modifier: Modifier) -> Unit,
): ImmutableList<RecordContentProvider> {
  val previousContentProviders = remember { mutableMapOf<String, RecordContentProvider>() }

  val lastNavigator by rememberUpdatedState(navigator)
  val lastCircuit by rememberUpdatedState(circuit)
  val lastUnavailableRoute by rememberUpdatedState(unavailableRoute)

  return iterator()
    .asSequence()
    .map { record ->
      // Query the previous content providers map, so that we use the same
      // RecordContentProvider instances across calls.
      previousContentProviders.getOrPut(record.key) {
        RecordContentProvider(
          record = record,
          content =
            movableContentOf { record ->
              CircuitContent(
                screen = record.screen,
                modifier = Modifier,
                navigator = lastNavigator,
                circuit = lastCircuit,
                unavailableContent = lastUnavailableRoute,
              )
            },
        )
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

/** Default values and common alternatives used by navigable composables. */
public object NavigatorDefaults {

  private const val FIVE_PERCENT = 0.05f
  private val SlightlyRight = { width: Int -> (width * FIVE_PERCENT).toInt() }
  private val SlightlyLeft = { width: Int -> 0 - (width * FIVE_PERCENT).toInt() }

  /** The default [NavDecoration] used in navigation. */
  public object DefaultDecoration : NavDecoration {
    @Composable
    override fun <T> DecoratedContent(
      args: ImmutableList<T>,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit
    ) {
      // Remember the previous stack depth so we know if the navigation is going "back".
      val prevStackDepth = rememberSaveable { mutableStateOf(backStackDepth) }
      val diff = backStackDepth - prevStackDepth.value
      prevStackDepth.value = backStackDepth
      AnimatedContent(
        targetState = args,
        modifier = modifier,
        transitionSpec = {
          // Mirror the forward and backward transitions of activities in Android 33
          when {
            diff > 0 -> {
              (slideInHorizontally(tween(), SlightlyRight) + fadeIn()) togetherWith
                slideOutHorizontally(tween(), SlightlyLeft) + fadeOut()
            }
            diff < 0 -> {
              (slideInHorizontally(tween(), SlightlyLeft) + fadeIn()) togetherWith
                slideOutHorizontally(tween(), SlightlyRight) + fadeOut()
            }
            else -> {
              // Crossfade if there was no diff
              fadeIn() togetherWith fadeOut()
            }
          }.using(
            // Disable clipping since the faded slide-in/out should
            // be displayed out of bounds.
            SizeTransform(clip = false)
          )
        },
      ) {
        content(it.first())
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
      content: @Composable (T) -> Unit
    ) {
      content(args.first())
    }
  }
}
