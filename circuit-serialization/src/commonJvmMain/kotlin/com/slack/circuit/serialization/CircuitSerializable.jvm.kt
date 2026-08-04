// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import dagger.hilt.GeneratesRootInput
import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@GeneratesRootInput
@Target(AnnotationTarget.CLASS)
public actual annotation class CircuitSerializable(actual val scope: KClass<*>)
