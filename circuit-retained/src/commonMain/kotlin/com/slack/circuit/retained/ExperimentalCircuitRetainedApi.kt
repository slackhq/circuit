// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

/** Indicates that the annotated retained API is experimental and may change or be removed. */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY)
public annotation class ExperimentalCircuitRetainedApi
