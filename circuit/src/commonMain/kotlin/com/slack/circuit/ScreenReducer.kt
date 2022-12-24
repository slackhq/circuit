package com.slack.circuit

public fun interface ScreenReducer {
  public operator fun invoke(screen: Screen, result: ScreenResult): Screen?
}