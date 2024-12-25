// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

@Retention(AnnotationRetention.BINARY)
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.CONSTRUCTOR,
)
@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message =
    "This is an experimental Circuit API that may be changed or removed at any time. " +
      "This usually means it's under active development or incubation, please report " +
      "any feedback on the issue tracker.",
)
public annotation class ExperimentalCircuitApi
