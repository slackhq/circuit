// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

/**
 * Marks internal Circuit saver APIs that may change without warning or migration support.
 *
 * These declarations support integration between Circuit's own artifacts and should not be used by
 * applications.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "This is an internal Circuit saver API. It may change without warning and should not be used " +
      "outside Circuit's first-party artifacts.",
)
public annotation class InternalCircuitSaverApi
