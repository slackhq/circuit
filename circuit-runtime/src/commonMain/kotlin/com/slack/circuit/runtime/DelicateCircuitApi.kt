// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

/**
 * Marks declarations that are **delicate** Circuit APIs, which means they are only safe to use in
 * very specific circumstances and only if you know what you're doing.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.CONSTRUCTOR,
)
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message = "This is a delicate Circuit API that should be used with care and avoided if possible.",
)
public annotation class DelicateCircuitApi
