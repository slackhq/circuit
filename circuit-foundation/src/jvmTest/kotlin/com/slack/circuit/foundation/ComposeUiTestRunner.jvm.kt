// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import org.junit.runner.Runner
import org.junit.runners.BlockJUnit4ClassRunner

internal actual fun createRunner(klass: Class<*>): Runner = BlockJUnit4ClassRunner(klass)
