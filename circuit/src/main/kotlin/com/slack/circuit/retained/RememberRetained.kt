/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive the activity or process
 * recreation using the saved instance state mechanism (for example it happens when the screen is
 * rotated in the Android application).
 *
 * You can use it with a value stored inside [androidx.compose.runtime.mutableStateOf].
 *
 * This differs from [rememberSaveable] by not being tied to Android bundles or parcelable. You
 * should take care to ensure that the state computed by [init] does not capture anything that is
 * not save to persist across reconfiguration, such as Navigators.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 * reset and [init] to be rerun
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 * automatically generated by the Compose runtime which is unique for the every exact code location
 * in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@Composable
fun <T : Any> rememberRetained(vararg inputs: Any?, key: String? = null, init: () -> T): T {
  // key is the one provided by the user or the one generated by the compose runtime
  val finalKey =
    if (!key.isNullOrEmpty()) {
      key
    } else {
      currentCompositeKeyHash.toString(MaxSupportedRadix)
    }

  val registry = LocalRetainedStateRegistry.current
  // value is restored using the registry or created via [init] lambda
  val value =
    remember(*inputs) {
      // TODO not restore when the input values changed (use hashKeys?) b/152014032
      val restored = registry.consumeValue(finalKey)
      restored ?: run { init() }
    }

  // we want to use the latest instances of value in the valueProvider lambda
  // without restarting DisposableEffect as it would cause re-registering the provider in
  // the different order. so we use rememberUpdatedState.
  val valueState = rememberUpdatedState(value)

  // TODO(zsweers) would be nice to abstract this bit away since it's Android-specific
  val activity = LocalContext.current.findActivity()
  remember(registry, finalKey) {
    val entry = registry.registerValue(finalKey, valueState.value)
    object : RememberObserver {
      override fun onAbandoned() = registerIfNotChangingConfiguration()

      override fun onForgotten() = registerIfNotChangingConfiguration()

      override fun onRemembered() {
        // Do nothing
      }

      fun registerIfNotChangingConfiguration() {
        if (activity?.isChangingConfigurations != true) {
          entry.unregister()
        }
      }
    }
  }
  @Suppress("UNCHECKED_CAST") return value as T
}

/** The maximum radix available for conversion to and from strings. */
private val MaxSupportedRadix = 36
