package com.slack.circuitx.navigation.intercepting

/** Simple logger used by [LoggingNavigatorFailureNotifier] and [LoggingNavigationEventListener]. */
public interface NavigationLogger {

  public fun log(message: String)
}
