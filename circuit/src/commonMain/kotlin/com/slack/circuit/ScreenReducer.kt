package com.slack.circuit

public interface ScreenReducer {
  public fun reduce(screen: Screen, result: ScreenResult): Screen?
}