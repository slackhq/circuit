// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sharedelements

import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Indicates that the annotated shared elements API is experimental and should be used carefully.
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION, PROPERTY)
public annotation class ExperimentalCircuitSharedElementsApi
