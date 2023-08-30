// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** On Android, we retain only if the activity is changing configurations. */
@Composable
public actual fun rememberCanRetainChecker(): CanRetainChecker {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  return remember { CanRetainChecker { activity?.isChangingConfigurations == true } }
}

private fun Context.findActivity(): Activity? {
  var context = this
  while (context is ContextWrapper) {
    if (context is Activity) return context
    context = context.baseContext
  }
  return null
}
