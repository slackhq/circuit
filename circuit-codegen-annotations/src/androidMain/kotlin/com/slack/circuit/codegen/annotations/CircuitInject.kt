// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen.annotations

import com.slack.circuit.runtime.screen.Screen
import dagger.hilt.GeneratesRootInput
import kotlin.reflect.KClass

/**
 * Android-specific [CircuitInject], applying the Hilt [GeneratesRootInput] annotation.
 *
 * For more general information about this annotation, see
 * [com.slack.circuit.codegen.annotations.CircuitInject]
 */
@GeneratesRootInput
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public actual annotation class CircuitInject(
  actual val screen: KClass<out Screen>,
  actual val scope: KClass<*>,
)
