// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message =
    "Inheriting from this circuit-test API is unstable. " +
      "Either new methods may be added in the future, which would break the inheritance, " +
      "or correctly inheriting from it requires fulfilling contracts that may change in the future.",
)
public annotation class ExperimentalForInheritanceCircuitTestApi
