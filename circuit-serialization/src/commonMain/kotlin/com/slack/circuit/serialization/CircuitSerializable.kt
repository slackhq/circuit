// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable
import kotlinx.serialization.Serializable

/**
 * Marks a concrete [Screen] or [PopResult] for serialization registration code generation in
 * [scope].
 *
 * This annotation also generates the type's default kotlinx serializer. Add [Serializable] with a
 * custom serializer when the type needs one.
 */
// KMP only for `@GeneratesRootInput` use on android/jvm
@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
public expect annotation class CircuitSerializable(val scope: KClass<*>)
