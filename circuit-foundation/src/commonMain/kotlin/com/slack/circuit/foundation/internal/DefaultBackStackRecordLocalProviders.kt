// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStackRecordLocalProvider

internal expect val defaultBackStackRecordLocalProviders:
  List<BackStackRecordLocalProvider<BackStack.Record>>
