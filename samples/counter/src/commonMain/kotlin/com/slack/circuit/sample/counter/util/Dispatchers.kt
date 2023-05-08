// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Amazing that we have to suppress the lint on the suppression
@Suppress("EXTENSION_SHADOWED_BY_MEMBER", "KotlinRedundantDiagnosticSuppress")
expect val Dispatchers.IO: CoroutineDispatcher
