// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStackRecordLocalProvider

/**
 * This is specifically a get() rather than a statically initialized property. The Kotlin/Native
 * optimizer seems to trip up otherwise: https://github.com/slackhq/circuit/issues/1075
 */
internal actual val defaultBackStackRecordLocalProviders:
  List<BackStackRecordLocalProvider<BackStack.Record>>
  get() = emptyList()
