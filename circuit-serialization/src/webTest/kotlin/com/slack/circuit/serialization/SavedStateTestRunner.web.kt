// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import kotlin.reflect.KClass

actual annotation class RunWith actual constructor(actual val value: KClass<out Runner>)

actual abstract class Runner

actual class SavedStateTestRunner actual constructor(klass: KClass<*>) : Runner()
