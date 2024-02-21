// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.effects

import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel

/**
 * A composable function that returns a lambda to show a [Toast]. Any previously shown toast will be
 * cancelled when a new one is shown or this composable exits composition. The returned lambda can
 * be called with the text to show in the toast.
 *
 * ```kotlin
 * val showToast = toastEffect()
 * // ...
 * Button(onClick = { showToast("Hello, world!") }) {
 *   Text("Show Toast")
 * }
 * ```
 *
 * @param duration The duration of the toast, either [Toast.LENGTH_SHORT] or [Toast.LENGTH_LONG].
 */
@CheckResult
@Composable
public fun toastEffect(duration: Int = Toast.LENGTH_SHORT): (String) -> Unit {
  val postChannel = remember { Channel<String>(Channel.CONFLATED) }
  val context = LocalContext.current
  LaunchedEffect(context) {
    var toast: Toast? = null
    try {
      for (text in postChannel) {
        toast?.cancel()
        toast = Toast.makeText(context, text, duration).also { it.show() }
      }
      awaitCancellation()
    } finally {
      toast?.cancel()
    }
  }
  return remember(context) { { text -> postChannel.trySend(text) } }
}
