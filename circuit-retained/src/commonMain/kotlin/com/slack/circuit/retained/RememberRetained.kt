// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive configuration changes, such
 * as a screen rotation.
 *
 * You can use it with a value stored inside [mutableStateOf].
 *
 * This differs from `rememberSaveable` by not being tied to Android bundles or parcelable. You
 * should take care to ensure that the state computed by [init] does not capture anything that is
 * not safe to persist across reconfiguration, such as Navigators. The same caveats of
 * `rememberSaveable` also still apply (i.e. do not retain Android Contexts, Views, etc).
 *
 * However, it does not participate in saved instance state either, so care should be taken to
 * choose the right retention mechanism for your use case. Consider the below two examples.
 *
 * The first case will retain `state` across configuration changes and the back stack but will _not_
 * survive process death.
 *
 * ```kotlin
 * @Composable
 * override fun present(): CounterState {
 *   var state by rememberRetained { mutableStateOf(CounterState(0)) }
 *
 *   return CounterState(count) { event ->
 *     when (event) {
 *       is CounterEvent.Increment -> state = state.copy(count = state.count + 1)
 *       is CounterEvent.Decrement -> state = state.copy(count = state.count - 1)
 *     }
 *   }
 * }
 * ```
 *
 * This second case will retain `count` across configuration changes, the back stack, _and_ survive
 * process death. However, it only works with primitives or implicitly Saveable (i.e. `Parcelable`
 * on Android) state types.
 *
 * ```kotlin
 * @Composable
 * override fun present(): CounterState {
 *   var count by rememberSaveable { mutableStateOf(0) }
 *
 *   return CounterState(count) { event ->
 *     when (event) {
 *       is CounterEvent.Increment -> state = count++
 *       is CounterEvent.Decrement -> state = count--
 *     }
 *   }
 * }
 * ```
 *
 * ## Layering
 *
 * There is also an overload of [rememberRetained] that takes a [Saver], which participates in both
 * the saved state registry system and retaining. Alternatively, use [rememberRetainedSaveable] for
 * one that uses [autoSaver]. These can be used to persist state across multiple layers, allowing
 * for both the caching of [rememberRetained] while also the process-death-survival of saveable.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 */
@Composable
@Deprecated(
  "Use first-party retain.",
  ReplaceWith("retain(*inputs, calculation = init)", "androidx.compose.runtime.retain.retain"),
)
@NonRestartableComposable
public fun <T : Any> rememberRetained(vararg inputs: Any?, init: () -> T): T =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    saver = neverSave(),
    calculation = init,
  )

/**
 * Remember the value produced by [init] using an explicit [key].
 *
 * A null or empty [key] uses the same positional identity as the unkeyed overload.
 */
@Deprecated(
  "Use first-party retain, or retain with an explicit key.",
  ReplaceWith(
    "key.let { retainedKey -> " +
      "if (retainedKey.isNullOrEmpty()) retain(*inputs, calculation = init) " +
      "else retain(*inputs, key = retainedKey, calculation = init) }",
    "androidx.compose.runtime.retain.retain",
    "com.slack.circuit.retained.retain",
  ),
)
@Composable
@NonRestartableComposable
public fun <T : Any> rememberRetained(vararg inputs: Any?, key: String? = null, init: () -> T): T =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    key = key,
    saver = neverSave(),
    calculation = init,
  )

/**
 * Retains the value produced by [calculation] under the explicit [key].
 *
 * @param inputs A set of inputs that reset the retained value when any of them change.
 * @param key The key used to identify the retained value.
 * @param calculation A factory that creates the retained value.
 */
@Composable
@NonRestartableComposable
public fun <T : Any> retain(
  vararg inputs: Any?,
  key: String,
  calculation: () -> T,
): T {
  require(key.isNotEmpty()) { "Retained key is empty" }
  return me.stagg.retainx.retain(
    inputs = inputs,
    key = key,
    calculation = calculation,
  )
}

/**
 * A simple proxy to [rememberRetained] that uses the default [autoSaver] for [saver] and a more
 * explicit name.
 *
 * @see rememberRetained
 */
@Composable
@NonRestartableComposable
public fun <T : Any> rememberRetainedSaveable(
  vararg inputs: Any?,
  saver: Saver<T, out Any> = autoSaver(),
  key: String? = null,
  init: () -> T,
): T =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    saver = saver,
    key = key,
    calculation = init,
  )

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [rememberRetained] by retaining the value in memory, but with an
 * additional [saver] parameter that will opportunistically save the value to saved instance state.
 *
 * The retained value will be the source of truth for the value, except in the case where the
 * process was killed, and the value is being restored. In that situation only, the most recent
 * value recovered from the saveable state registry will be used instead of the value provided by
 * [init].
 *
 * Importantly, the most recent value persisted in saveable state may lag behind the value that is
 * saved in memory. For example, on Android, saved state will be collected when an Activity is
 * backgrounded and onStop is called. The retained value might continue to be updated while in the
 * background, but those updates will not be reflected in the saved state. If the process is killed
 * before the app has returned to the foreground, these background updates will be lost, as the
 * restored saved state will have been taken before the background updates occurred.
 *
 * Therefore, [saver] should only store and restore information that is logically safe to have an
 * out-of-date view of.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [init] to be rerun
 * @param saver The [Saver] object which defines how the state is saved and restored.
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 *   automatically generated by the Compose runtime which is unique for the every exact code
 *   location in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@DelicateCircuitRetainedApi
@Composable
@NonRestartableComposable
public fun <T : Any> rememberRetained(
  vararg inputs: Any?,
  saver: Saver<T, out Any>,
  key: String? = null,
  init: () -> T,
): T =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    saver = saver,
    key = key,
    calculation = init,
  )

/**
 * Retains the value produced by [calculation] and opportunistically saves it with [saver].
 *
 * @param inputs A set of inputs that reset the retained value when any of them change.
 * @param saver Defines how the value is saved and restored.
 * @param key An optional explicit identity for the retained and saved value.
 * @param calculation A factory that creates the value when neither retained nor saved state exists.
 */
@Composable
@NonRestartableComposable
public fun <T : Any> retainSaveable(
  vararg inputs: Any?,
  saver: Saver<T, out Any> = autoSaver(),
  key: String? = null,
  calculation: () -> T,
): T =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    saver = saver,
    key = key,
    calculation = calculation,
  )

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive configuration changes, such
 * as a screen rotation.
 *
 * Use this overload if you remember a mutable state to specify a [stateSaver] for the value
 * contained inside the [MutableState].
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [init] to be rerun
 * @param stateSaver The [Saver] object which defines how the value inside the MutableState is saved
 *   and restored.
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 *   automatically generated by the Compose runtime which is unique for the every exact code
 *   location in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@DelicateCircuitRetainedApi
@Composable
@NonRestartableComposable
public fun <T> rememberRetained(
  vararg inputs: Any?,
  stateSaver: Saver<T, out Any>,
  key: String? = null,
  init: () -> MutableState<T>,
): MutableState<T> =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    stateSaver = stateSaver,
    key = key,
    calculation = init,
  )

/**
 * A simple proxy to [rememberRetained] that uses the default [autoSaver] for [stateSaver] and a
 * more explicit name.
 *
 * @see rememberRetained
 */
@DelicateCircuitRetainedApi
@Composable
@NonRestartableComposable
public fun <T> rememberRetainedSaveable(
  vararg inputs: Any?,
  stateSaver: Saver<T, out Any>,
  key: String? = null,
  init: () -> MutableState<T>,
): MutableState<T> =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    stateSaver = stateSaver,
    key = key,
    calculation = init,
  )

/**
 * Retains a [MutableState] produced by [calculation] and opportunistically saves its value with
 * [stateSaver].
 */
@DelicateCircuitRetainedApi
@Composable
@NonRestartableComposable
public fun <T> retainSaveable(
  vararg inputs: Any?,
  stateSaver: Saver<T, out Any>,
  key: String? = null,
  calculation: () -> MutableState<T>,
): MutableState<T> =
  me.stagg.retainx.retainSaveable(
    inputs = inputs,
    stateSaver = stateSaver,
    key = key,
    calculation = calculation,
  )

private val NoOpSaver = Saver<Any?, Any>({ null }, { null })

@Suppress("UNCHECKED_CAST")
internal fun <Original, Saveable : Any> neverSave() = NoOpSaver as Saver<Original, Saveable>
