// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import kotlin.reflect.KClass

expect annotation class RunWith(val value: KClass<out Runner>)

expect abstract class Runner

expect class SavedStateTestRunner(klass: KClass<*>) : Runner
