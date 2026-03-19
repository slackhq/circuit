// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import kotlin.reflect.KClass

expect annotation class RunWith(val value: KClass<out Runner>)

expect abstract class Runner

expect class ComposeUiTestRunner(klass: KClass<*>) : Runner
