// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen.annotations.internal

import com.slack.circuit.runtime.InternalCircuitApi
import kotlin.reflect.KClass

/** TODO */
@Target(AnnotationTarget.CLASS)
@Suppress("unused")
@InternalCircuitApi
@Repeatable
public annotation class KotlinInjectHint(val scope: KClass<*>)
