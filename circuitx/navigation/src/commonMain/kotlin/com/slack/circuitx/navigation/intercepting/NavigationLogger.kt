// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

/** Simple logger used by [LoggingNavigatorFailureNotifier] and [LoggingNavigationEventListener]. */
public interface NavigationLogger {

  public fun log(message: String)
}
