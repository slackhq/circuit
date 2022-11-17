// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.slack.circuit.retained.LocalRetainedStateRegistryOwner
import com.slack.circuit.retained.continuityRetainedStateRegistry

@Composable
internal actual fun PlatformCompositionLocals(content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalRetainedStateRegistryOwner provides continuityRetainedStateRegistry(),
  ) {
    content()
  }
}
