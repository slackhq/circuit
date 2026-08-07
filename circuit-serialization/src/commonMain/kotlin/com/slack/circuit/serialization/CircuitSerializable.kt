// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

/**
 * Marks a concrete [Screen] or [PopResult] for serialization registration code generation in
 * [scope].
 *
 * This annotation supplies the type's default kotlinx serializer. Add `@Serializable(with = ...)`
 * only when the type uses a custom serializer.
 *
 * Apply this annotation with the same [scope] to an expect declaration and every actual
 * declaration. Code generation emits one registration for the expect declaration.
 */
// Declared with expect/actual so the JVM annotation can include Hilt's `@GeneratesRootInput`.
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
public expect annotation class CircuitSerializable(val scope: KClass<*>)
