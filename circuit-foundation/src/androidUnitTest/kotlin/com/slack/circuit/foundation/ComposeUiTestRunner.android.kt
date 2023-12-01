// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import org.junit.runner.Runner
import org.robolectric.RobolectricTestRunner

internal actual fun createRunner(klass: Class<*>): Runner = RobolectricTestRunner(klass)
