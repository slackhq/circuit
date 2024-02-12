// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen.annotations

import kotlin.reflect.KClass

/** TODO */
@Target(AnnotationTarget.CLASS)
@Suppress("unused")
public annotation class MergeCircuitComponent<ParentComponent>(val scope: KClass<*>)
