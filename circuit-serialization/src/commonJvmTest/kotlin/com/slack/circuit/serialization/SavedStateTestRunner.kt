// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import kotlin.reflect.KClass
import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier

actual typealias RunWith = org.junit.runner.RunWith

actual typealias Runner = org.junit.runner.Runner

/**
 * A [Runner] for multiplatform tests that touch `SavedState` at runtime, which is a `Bundle` on
 * Android and needs Robolectric in host tests.
 */
actual class SavedStateTestRunner(klass: Class<*>) : Runner() {

  actual constructor(klass: KClass<*>) : this(klass.java)

  private val delegateRunner = createRunner(klass)

  override fun getDescription(): Description = delegateRunner.description

  override fun run(notifier: RunNotifier?) = delegateRunner.run(notifier)
}

internal expect fun createRunner(klass: Class<*>): Runner
