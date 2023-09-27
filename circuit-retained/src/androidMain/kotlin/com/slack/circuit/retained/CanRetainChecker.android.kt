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
  return remember {
    CanRetainChecker { registry ->
      if (registry is ContinuityViewModel) {
        // If this is the root Continuity registry, only retain if the Activity is changing
        // configuration
        activity?.isChangingConfigurations == true
      } else {
        // Otherwise we always allow retaining for 'child' registries
        true
      }
    }
  }
}

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}
