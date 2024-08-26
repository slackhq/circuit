// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import kotlin.annotation.AnnotationTarget.FUNCTION

/** Indicates that the annotated retained API is delicate and should be used carefully. */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION)
public annotation class DelicateCircuitRetainedApi
