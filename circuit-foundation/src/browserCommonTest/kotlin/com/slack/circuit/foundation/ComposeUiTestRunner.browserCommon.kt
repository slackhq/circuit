// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import kotlin.reflect.KClass

actual annotation class RunWith actual constructor(actual val value: KClass<out Runner>)

actual abstract class Runner

actual class ComposeUiTestRunner actual constructor(klass: KClass<*>) : Runner()
