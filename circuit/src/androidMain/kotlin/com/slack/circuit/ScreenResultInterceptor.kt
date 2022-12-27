package com.slack.circuit

public fun interface ScreenResultInterceptor {
  public operator fun invoke(screen: Screen, screenResult: ScreenResult?): Boolean
}