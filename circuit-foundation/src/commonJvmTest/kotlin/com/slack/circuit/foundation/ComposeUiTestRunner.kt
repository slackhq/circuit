// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

/**
 * A [Runner] to be used with multiplatform Compose tests.
 *
 * Internally, this delegates to a platform specific runner.
 */
class ComposeUiTestRunner(klass: Class<*>) : Runner() {
  private val delegateRunner = createRunner(klass)

  override fun getDescription(): Description = delegateRunner.description

  override fun run(notifier: RunNotifier?) = delegateRunner.run(notifier)
}

internal expect fun createRunner(klass: Class<*>): Runner
