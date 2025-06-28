// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

/**
 * Layer of indirection for presenter tests to allow for platform-specific `RunWith` annotations.
 */
expect abstract class BasePresenterTest {
  constructor()
}
